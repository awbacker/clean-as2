package org.cleanas2.util

import java.io.*
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/*
    Modified by Andrew Backer to be compatible with Java v1.7.
 */

/**
 * Uber-fast, ultra-lightweight Java classpath scanner. Scans the classpath by parsing the classfile binary
 * format directly rather than by using reflection. (Reflection causes the classloader to load each class,
 * which can take an order of magnitude more time than parsing the classfile directly.)
 *
 *
 * This classpath scanner is able to scan directories and jar/zip files on the classpath to locate: (1)
 * classes that subclass a given class or one of its subclasses; (2) classes that implement an interface or
 * one of its subinterfaces; (3) classes that have a given annotation; and (4) file paths (even for
 * non-classfiles) anywhere on the classpath that match a given regexp.
 *
 *
 *
 *
 * Usage example (with Java 8 lambda expressions):
 *
 *
 * `
 * new FastClasspathScanner(
 * new String[] { "com.xyz.widget", "com.xyz.gizmo" })  // Whitelisted package prefixes to scan
 *
 *
 * .matchSubclassesOf(DBModel.class,
 * // c is a subclass of DBModel
 * c -> System.out.println("Subclasses DBModel: " + c.getName()))
 *
 *
 * .matchClassesImplementing(Runnable.class,
 * // c is a class that implements Runnable
 * c -> System.out.println("Implements Runnable: " + c.getName()))
 *
 *
 * .matchClassesWithAnnotation(RestHandler.class,
 * // c is a class annotated with @RestHandler
 * c -> System.out.println("Has @RestHandler class annotation: " + c.getName()))
 *
 *
 *
 *
 * .matchFilenamePattern("^template/.*\\.html",
 * // templatePath is a path on the classpath that matches the above pattern;
 * // inputStream is a stream opened on the file or zipfile entry
 * // No need to close inputStream before exiting, it is closed by caller.
 * (absolutePath, relativePath, inputStream) -> {
 * try {
 * String template = IOUtils.toString(inputStream, "UTF-8");
 * System.out.println("Found template: " + absolutePath
 * + " (size " + template.length() + ")");
 * } catch (IOException e) {
 * throw new RuntimeException(e);
 * }
 * })
 *
 *
 * .scan();  // Actually perform the scan
` *
 *
 *
 * Note that you need to pass a whitelist of package prefixes to scan into the constructor, and the ability
 * to detect that a class or interface extends another depends upon the entire ancestral path between the two
 * classes or interfaces having one of the whitelisted package prefixes.
 *
 *
 * The scanner also records the latest last-modified timestamp of any file or directory encountered, and you
 * can see if that latest last-modified timestamp has increased (indicating that something on the classpath
 * has been updated) by calling:
 *
 *
 * `
 * boolean classpathContentsModified = fastClassPathScanner.classpathContentsModifiedSinceScan();
` *
 *
 *
 * This can be used to enable dynamic class-reloading if something on the classpath is updated, for example
 * to support hot-replace of route handler classes in a webserver. The above call is several times faster
 * than the original call to scan(), since only modification timestamps need to be checked.
 *
 *
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 *
 *
 * Inspired by: https://github.com/rmuller/infomas-asl/tree/master/annotation-detector
 *
 *
 * See also: http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4
 *
 *
 * Let me know if you find this useful!
 *
 * @author Luke Hutchison <luke .dot. hutch .at. gmail .dot. com>
 * @license MIT
 *
 *
 * The MIT License (MIT)
 *
 *
 * Copyright (c) 2014 Luke Hutchison
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
</luke> */
class FastClasspathScanner
// ------------------------------------------------------------------------------------------------------

/**
 * Initialize a classpath scanner, with a list of package prefixes to scan.
 *
 * @param pacakagesToScan A list of package prefixes to scan.
 */
(
        /**
         * List of directory path prefixes to scan (produced from list of package prefixes passed into the
         * constructor)
         */
        private val pathsToScan: Array<String>) {

    /**
     * The latest last-modified timestamp of any file, directory or sub-directory in the classpath, in millis
     * since the Unix epoch. Does not consider timestamps inside zipfiles/jarfiles, but the timestamp of the
     * zip/jarfile itself is considered.
     */
    private var lastModified: Long = 0

    /**
     * A list of class matchers to call once all classes have been read in from classpath.
     */
    private val classMatchers = ArrayList<ClassMatcher>()

    /**
     * A list of file path matchers to call when a directory or subdirectory on the classpath matches a given
     * regexp.
     */
    private val filePathMatchers = ArrayList<FilePathMatcher>()

    /**
     * A map from fully-qualified class name to the corresponding ClassInfo object.
     */
    private val classNameToClassInfo = HashMap<String, ClassInfo>()

    /**
     * A map from fully-qualified class name to the corresponding InterfaceInfo object.
     */
    private val interfaceNameToInterfaceInfo = HashMap<String, InterfaceInfo>()

    /**
     * Reverse mapping from annotation to classes that have the annotation
     */
    private val annotationToClasses = HashMap<String, ArrayList<String>>()

    /**
     * Reverse mapping from interface to classes that implement the interface
     */
    private val interfaceToClasses = HashMap<String, ArrayList<String>>()

    init {
        for (i in pathsToScan.indices) {
            pathsToScan[i] = pathsToScan[i].replace('.', '/') + "/"
        }
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * The method to run when a subclass of a specific class is found on the classpath.
     */
    interface SubclassMatchProcessor<in T> {
        fun processMatch(matchingClass: Class<out T>)
    }

    /**
     * Call the given ClassMatchProcessor if classes are found on the classpath that extend the specified
     * superclass.
     *
     * @param superclass          The superclass to match (i.e. the class that subclasses need to extend to match).
     * @param classMatchProcessor the ClassMatchProcessor to call when a match is found.
     */
    fun <T> matchSubclassesOf(superclass: Class<T>,
                              classMatchProcessor: SubclassMatchProcessor<T>): FastClasspathScanner {
        if (superclass.isInterface) {
            // No support yet for scanning for interfaces that extend other interfaces
            throw IllegalArgumentException(superclass.name + " is an interface, not a regular class")
        }
        if (superclass.isAnnotation) {
            // No support yet for scanning for interfaces that extend other interfaces
            throw IllegalArgumentException(superclass.name + " is an annotation, not a regular class")
        }

        classMatchers.add(object : ClassMatcher {
            override fun lookForMatches() {
                val superclassInfo = classNameToClassInfo[superclass.name]
                var foundMatches = false
                if (superclassInfo != null) {
                    // For all subclasses of the given superclass
                    for (subclassInfo in superclassInfo.allSubclasses) {
                        try {
                            // Load class
                            val klass = Class.forName(subclassInfo.name) as Class<out T>
                            // Process match
                            classMatchProcessor.processMatch(klass)
                            foundMatches = true
                        } catch (e: ClassNotFoundException) {
                            throw RuntimeException(e)
                        } catch (e: NoClassDefFoundError) {
                            throw RuntimeException(e)
                        }

                    }
                }
                if (!foundMatches) {
                    // Log.info("No classes found with superclass " + superclass.getName());
                }
            }
        })
        return this
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * The method to run when a class implementing a specific interface is found on the classpath.
     */
    interface InterfaceMatchProcessor<T> {
        fun processMatch(matchingClass: Class<out T>)
    }

    /**
     * Call the given ClassMatchProcessor if classes are found on the classpath that implement the specified
     * interface.
     *
     * @param iface                   The interface to match (i.e. the interface that classes need to implement to match).
     * @param interfaceMatchProcessor the ClassMatchProcessor to call when a match is found.
     */
    fun <T> matchClassesImplementing(iface: Class<T>,
                                     interfaceMatchProcessor: InterfaceMatchProcessor<T>): FastClasspathScanner {
        if (!iface.isInterface) {
            throw IllegalArgumentException(iface.name + " is not an interface")
        }
        classMatchers.add(object : ClassMatcher {
            override fun lookForMatches() {
                val classesImplementingIface = interfaceToClasses[iface.name]
                if (classesImplementingIface != null) {
                    // For all classes implementing the given interface
                    for (implClass in classesImplementingIface) {
                        try {
                            // Load class
                            val klass = Class.forName(implClass) as Class<out T>
                            // Process match
                            interfaceMatchProcessor.processMatch(klass)
                        } catch (e: ClassNotFoundException) {
                            throw RuntimeException(e)
                        } catch (e: NoClassDefFoundError) {
                            throw RuntimeException(e)
                        }

                    }
                } else {
                    // Log.info("No classes found implementing interface " + iface.getName());
                }
            }
        })
        return this
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * The method to run when a class with the right matching annotation is found on the classpath.
     */
    interface ClassAnnotationMatchProcessor {
        fun processMatch(matchingClass: Class<*>)
    }

    /**
     * Call the given ClassMatchProcessor if classes are found on the classpath that have the given
     * annotation.
     *
     * @param annotation          The class annotation to match.
     * @param classMatchProcessor the ClassMatchProcessor to call when a match is found.
     */
    fun matchClassesWithAnnotation(annotation: Class<*>,
                                   classMatchProcessor: ClassAnnotationMatchProcessor): FastClasspathScanner {
        if (!annotation.isAnnotation) {
            throw IllegalArgumentException("Class " + annotation.name + " is not an annotation")
        }
        classMatchers.add(object : ClassMatcher {
            override fun lookForMatches() {
                val classesWithAnnotation = annotationToClasses[annotation.name]
                if (classesWithAnnotation != null) {
                    // For all classes with the given annotation
                    for (classWithAnnotation in classesWithAnnotation) {
                        try {
                            // Load class
                            val klass = Class.forName(classWithAnnotation)
                            // Process match
                            classMatchProcessor.processMatch(klass)
                        } catch (e: ClassNotFoundException) {
                            throw RuntimeException(e)
                        } catch (e: NoClassDefFoundError) {
                            throw RuntimeException(e)
                        }

                    }
                } else {
                    // Log.info("No classes found with annotation " + annotation.getName());
                }
            }
        })
        return this
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * The method to run when a matching file is found on the classpath.
     */
    interface FileMatchProcessor {
        /**
         * Process a matching file.
         *
         * @param absolutePath The path of the matching file on the filesystem.
         * @param relativePath The path of the matching file relative to the classpath entry that contained the match.
         * @param inputStream  An InputStream (either a FileInputStream or a ZipEntry InputStream) opened on the file.
         * You do not need to close this InputStream before returning, it is closed by the caller.
         */
        fun processMatch(absolutePath: String, relativePath: String, inputStream: InputStream)
    }

    /**
     * Call the given FileMatchProcessor if files are found on the classpath with the given regex pattern in
     * their path.
     *
     * @param filenameMatchPattern The regex to match, e.g. "app/templates/.*\\.html"
     * @param fileMatchProcessor   The FileMatchProcessor to call when each match is found.
     */
    fun matchFilenamePattern(filenameMatchPattern: String,
                             fileMatchProcessor: FileMatchProcessor): FastClasspathScanner {
        filePathMatchers.add(FilePathMatcher(Pattern.compile(filenameMatchPattern), fileMatchProcessor))
        return this
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * An interface used for testing if a file path matches a specified pattern.
     */
    private class FilePathMatcher(internal var pattern: Pattern, internal var fileMatchProcessor: FileMatchProcessor)

    /**
     * A functional interface used for testing if a class matches specified criteria.
     */
    private interface ClassMatcher {
        fun lookForMatches()
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * An object to hold class information. For speed purposes, this is reconstructed directly from the
     * classfile header without calling the classloader.
     */
    private class ClassInfo {
        /**
         * Class name
         */
        internal var name: String

        /**
         * Set to true when this class is encountered in the classpath (false if the class is so far only
         * cited as a superclass)
         */
        internal var encountered: Boolean = false

        /**
         * Direct superclass
         */
        internal var directSuperclass: ClassInfo? = null

        /**
         * Direct subclasses
         */
        internal var directSubclasses = ArrayList<ClassInfo>()

        /**
         * All superclasses, including java.lang.Object.
         */
        internal var allSuperclasses = HashSet<ClassInfo>()

        /**
         * All subclasses
         */
        internal var allSubclasses = HashSet<ClassInfo>()

        /**
         * All interfaces
         */
        internal var interfaces = HashSet<String>()

        /**
         * All annotations
         */
        internal var annotations = HashSet<String>()

        /**
         * This class was encountered on the classpath.
         */
        constructor(name: String, interfaces: ArrayList<String>, annotations: HashSet<String>) {
            this.name = name
            this.encounter(interfaces, annotations)
        }

        /**
         * If called by another class, this class was previously cited as a superclass, and now has been
         * itself encountered on the classpath.
         */
        fun encounter(interfaces: ArrayList<String>, annotations: HashSet<String>) {
            this.encountered = true
            this.interfaces.addAll(interfaces)
            this.annotations.addAll(annotations)
        }

        /**
         * This class was referenced as a superclass of the given subclass.
         */
        constructor(name: String, subclass: ClassInfo) {
            this.name = name
            this.encountered = false
            addSubclass(subclass)
        }

        /**
         * Connect this class to a subclass.
         */
        fun addSubclass(subclass: ClassInfo) {
            if (subclass.directSuperclass != null && subclass.directSuperclass !== this) {
                throw RuntimeException(subclass.name + " has two superclasses: "
                        + subclass.directSuperclass!!.name + ", " + this.name)
            }
            subclass.directSuperclass = this
            subclass.allSuperclasses.add(this)
            this.directSubclasses.add(subclass)
            this.allSubclasses.add(subclass)
        }

        override fun toString(): String {
            return name
        }
    }

    /**
     * Direct and ancestral interfaces of a given interface.
     */
    private class InterfaceInfo(superInterfaces: ArrayList<String>) {
        internal var superInterfaces = ArrayList<String>()

        internal var allSuperInterfaces = HashSet<String>()

        init {
            this.superInterfaces.addAll(superInterfaces)
        }

    }

    /**
     * Recursively find all superinterfaces of each interface; called by finalizeClassHierarchy.
     */
    private fun finalizeInterfaceHierarchyRec(interfaceInfo: InterfaceInfo) {
        // Interface inheritance is a DAG; don't double-visit nodes
        if (interfaceInfo.allSuperInterfaces.isEmpty() && !interfaceInfo.superInterfaces.isEmpty()) {
            interfaceInfo.allSuperInterfaces.addAll(interfaceInfo.superInterfaces)
            for (iface in interfaceInfo.superInterfaces) {
                val superinterfaceInfo = interfaceNameToInterfaceInfo[iface]
                if (superinterfaceInfo != null) {
                    finalizeInterfaceHierarchyRec(superinterfaceInfo)
                    // Merge all ancestral interfaces into list of all superinterfaces for this interface
                    interfaceInfo.allSuperInterfaces.addAll(superinterfaceInfo.allSuperInterfaces)
                }
            }
        }
    }

    /**
     * Find all superclasses and subclasses for each class once all classes have been read.
     */
    private fun finalizeClassHierarchy() {
        if (classNameToClassInfo.isEmpty() && interfaceNameToInterfaceInfo.isEmpty()) {
            // If no classes or interfaces were matched, there is no hierarchy to build
            return
        }

        // Find all root nodes (most classes and interfaces have java.lang.Object as a superclass)
        val roots = ArrayList<ClassInfo>()
        for (classInfo in classNameToClassInfo.values) {
            if (classInfo.directSuperclass == null) {
                roots.add(classInfo)
            }
        }

        // Accumulate all superclasses and interfaces along each branch of class hierarchy.
        // Traverse top down / breadth first from roots.
        val nodes = LinkedList<ClassInfo>()
        nodes.addAll(roots)
        while (!nodes.isEmpty()) {
            val head = nodes.removeFirst()

            if (head.directSuperclass != null) {
                // Accumulate superclasses from ancestral classes
                head.allSuperclasses.addAll(head.directSuperclass!!.allSuperclasses)
            }

            // Add subclasses to queue for BFS
            for (subclass in head.directSubclasses) {
                nodes.add(subclass)
            }
        }

        // Accumulate all subclasses along each branch of class hierarchy.
        // Traverse depth first, postorder from roots.
        for (root in roots) {
            finalizeClassHierarchyRec(root)
        }

        // Create reverse mapping from annotation to classes that have the annotation
        for (classInfo in classNameToClassInfo.values) {
            for (annotation in classInfo.annotations) {
                val classList: ArrayList<String>? = annotationToClasses[annotation]
                if (classList == null) {
                    annotationToClasses.put(annotation, ArrayList())
                }
                classList!!.add(classInfo.name)
            }
        }

        for (ii in interfaceNameToInterfaceInfo.values) {
            finalizeInterfaceHierarchyRec(ii)
        }

        // Create reverse mapping from interface to classes that implement the interface
        for (classInfo in classNameToClassInfo.values) {
            // Find all interfaces and superinterfaces of a class
            val interfaceAndSuperinterfaces = HashSet<String>()
            for (iface in classInfo.interfaces) {
                interfaceAndSuperinterfaces.add(iface)
                val ii = interfaceNameToInterfaceInfo[iface]
                if (ii != null) {
                    interfaceAndSuperinterfaces.addAll(ii.allSuperInterfaces)
                }
            }
            // Add a mapping from the interface or super-interface back to the class
            for (iface in interfaceAndSuperinterfaces) {
                val classList: ArrayList<String>? = interfaceToClasses[iface]
                if (classList == null) {
                    interfaceToClasses.put(iface, ArrayList())
                }
                classList!!.add(classInfo.name)
            }
        }

        // Classes that subclass another class that implements an interface also implement that interface
        for (iface in interfaceToClasses.keys) {
            val classes = interfaceToClasses[iface]
            val subClasses = HashSet(classes)
            for (klass in classes!!) {
                val ci = classNameToClassInfo[klass]
                if (ci != null) {
                    for (subci in ci.allSubclasses) {
                        subClasses.add(subci.name)
                    }
                }
            }
            interfaceToClasses.put(iface, ArrayList(subClasses))
        }
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * Read annotation entry from classfile.
     */
    @Throws(IOException::class)
    private fun readAnnotation(inp: DataInputStream, constantPool: Array<Any?>): String {
        val annotationFieldDescriptor = readRefdString(inp, constantPool)
        val annotationClassName: String
        if (annotationFieldDescriptor[0] == 'L' && annotationFieldDescriptor[annotationFieldDescriptor.length - 1] == ';') {
            // Lcom/xyz/Annotation; -> com.xyz.Annotation
            annotationClassName = annotationFieldDescriptor.substring(1,
                    annotationFieldDescriptor.length - 1).replace('/', '.')
        } else {
            // Should not happen
            annotationClassName = annotationFieldDescriptor
        }
        val numElementValuePairs = inp.readUnsignedShort()
        for (i in 0 until numElementValuePairs) {
            inp.skipBytes(2) // element_name_index
            readAnnotationElementValue(inp, constantPool)
        }
        return annotationClassName
    }

    /**
     * Read annotation element value from classfile.
     */
    @Throws(IOException::class)
    private fun readAnnotationElementValue(inp: DataInputStream, constantPool: Array<Any?>) {
        val tag = inp.readUnsignedByte().toChar()
        when (tag) {
            'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's' ->
                // const_value_index
                inp.skipBytes(2)
            'e' ->
                // enum_const_value
                inp.skipBytes(4)
            'c' ->
                // class_info_index
                inp.skipBytes(2)
            '@' ->
                // Complex (nested) annotation
                readAnnotation(inp, constantPool)
            '[' -> {
                // array_value
                val count = inp.readUnsignedShort()
                for (l in 0 until count) {
                    // Nested annotation element value
                    readAnnotationElementValue(inp, constantPool)
                }
            }
            else -> throw ClassFormatError("Invalid annotation element type tag: 0x" + Integer.toHexString(tag.toInt()))
        }
    }

    /**
     * Directly examine contents of classfile binary header.
     */
    @Throws(IOException::class)
    private fun readClassInfoFromClassfileHeader(inputStream: InputStream) {
        val inp = DataInputStream(BufferedInputStream(inputStream, 1024))

        // Magic
        if (inp.readInt() != -0x35014542) {
            // Not classfile
            return
        }

        // Minor version
        inp.readUnsignedShort()
        // Major version
        inp.readUnsignedShort()

        // Constant pool count (1-indexed, zeroth entry not used)
        val cpCount = inp.readUnsignedShort()
        // Constant pool
        val constantPool = arrayOfNulls<Any>(cpCount)
        run {
            var i = 1
            while (i < cpCount) {
                val tag = inp.readUnsignedByte()
                when (tag) {
                    1 // Modified UTF8
                    -> constantPool[i] = inp.readUTF()
                    3 // int
                        , 4 // float
                    -> inp.skipBytes(4)
                    5 // long
                        , 6 // double
                    -> {
                        inp.skipBytes(8)
                        i++ // double slot
                    }
                    7 // Class
                        , 8 // String
                    ->
                        // Forward or backward reference a Modified UTF8 entry
                        constantPool[i] = inp.readUnsignedShort()
                    9 // field ref
                        , 10 // method ref
                        , 11 // interface ref
                        , 12 // name and type
                    -> inp.skipBytes(4) // two shorts
                    15 // method handle
                    -> inp.skipBytes(3)
                    16 // method type
                    -> inp.skipBytes(2)
                    18 // invoke dynamic
                    -> inp.skipBytes(4)
                    else -> throw ClassFormatError("Unkown tag value for constant pool entry: " + tag)
                }
                ++i
            }
        }

        // Access flags
        val flags = inp.readUnsignedShort()
        val isInterface = flags and 0x0200 != 0

        // This class name, with slashes replaced with dots
        val className = readRefdString(inp, constantPool).replace('/', '.')

        // Superclass name, with slashes replaced with dots
        val superclassName = readRefdString(inp, constantPool).replace('/', '.')

        // Interfaces
        val interfaceCount = inp.readUnsignedShort()
        val interfaces = ArrayList<String>()
        for (i in 0 until interfaceCount) {
            interfaces.add(readRefdString(inp, constantPool).replace('/', '.'))
        }

        // Fields
        val fieldCount = inp.readUnsignedShort()
        for (i in 0 until fieldCount) {
            inp.skipBytes(6) // access_flags, name_index, descriptor_index
            val attributesCount = inp.readUnsignedShort()
            for (j in 0 until attributesCount) {
                inp.skipBytes(2) // attribute_name_index
                val attributeLength = inp.readInt()
                inp.skipBytes(attributeLength)
            }
        }

        // Methods
        val methodCount = inp.readUnsignedShort()
        for (i in 0 until methodCount) {
            inp.skipBytes(6) // access_flags, name_index, descriptor_index
            val attributesCount = inp.readUnsignedShort()
            for (j in 0 until attributesCount) {
                inp.skipBytes(2) // attribute_name_index
                val attributeLength = inp.readInt()
                inp.skipBytes(attributeLength)
            }
        }

        // Attributes (including class annotations)
        val annotations = HashSet<String>()
        val attributesCount = inp.readUnsignedShort()
        for (i in 0 until attributesCount) {
            val attributeName = readRefdString(inp, constantPool)
            val attributeLength = inp.readInt()
            if ("RuntimeVisibleAnnotations" == attributeName) {
                val annotationCount = inp.readUnsignedShort()
                for (m in 0 until annotationCount) {
                    val annotationName = readAnnotation(inp, constantPool)
                    annotations.add(annotationName)
                }
            } else {
                inp.skipBytes(attributeLength)
            }
        }

        if (isInterface) {
            // Save the info recovered from the classfile for an interface

            // Look up InterfaceInfo object for this interface
            val thisInterfaceInfo: InterfaceInfo? = interfaceNameToInterfaceInfo[className]
            if (thisInterfaceInfo == null) {
                // This interface has not been encountered before on the classpath
                interfaceNameToInterfaceInfo.put(className, InterfaceInfo(interfaces))
            } else {
                // An interface of this fully-qualified name has been encountered already earlier on
                // the classpath, so this interface is shadowed, ignore it
                return
            }

        } else {
            // Save the info recovered from the classfile for a class

            // Look up ClassInfo object for this class
            val thisClassInfo: ClassInfo? = classNameToClassInfo[className]

            if (thisClassInfo == null) {
                // This class has not been encountered before on the classpath
                classNameToClassInfo.put(className, ClassInfo(className, interfaces, annotations))
            } else if (thisClassInfo.encountered) {
                // A class of this fully-qualified name has been encountered already earlier on
                // the classpath, so this class is shadowed, ignore it
                return
            } else {
                // This is the first time this class has been encountered on the classpath, but
                // it was previously cited as a superclass of another class
                thisClassInfo.encounter(interfaces, annotations)
            }

            // Look up ClassInfo object for superclass, and connect it to this class
            val superclassInfo: ClassInfo? = classNameToClassInfo[superclassName]
            when (superclassInfo) {
                null -> classNameToClassInfo.put(superclassName, ClassInfo(superclassName, thisClassInfo!!))
                else -> superclassInfo.addSubclass(thisClassInfo!!)
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------

    /**
     * Scan a file.
     */
    @Throws(IOException::class)
    private fun scanFile(file: File, absolutePath: String, relativePath: String, scanTimestampsOnly: Boolean) {
        lastModified = Math.max(lastModified, file.lastModified())
        if (!scanTimestampsOnly) {
            if (relativePath.endsWith(".class")) {
                // Found a classfile
                FileInputStream(file).use { inputStream ->
                    // Inspect header of classfile
                    readClassInfoFromClassfileHeader(inputStream)
                }
            } else {
                // For non-classfiles, match file paths against path patterns
                for (fileMatcher in filePathMatchers) {
                    if (fileMatcher.pattern.matcher(relativePath).matches()) {
                        // If there's a match, open the file as a stream and call the match processor
                        FileInputStream(file).use { inputStream -> fileMatcher.fileMatchProcessor.processMatch(absolutePath, relativePath, inputStream) }
                    }
                }
            }
        }
    }

    /**
     * Scan a directory for matching file path patterns.
     */
    @Throws(IOException::class)
    private fun scanDir(dir: File, ignorePrefixLen: Int, scanTimestampsOnly: Boolean) {
        val absolutePath = dir.path
        val relativePath = if (ignorePrefixLen > absolutePath.length) "" else absolutePath.substring(ignorePrefixLen)
        var scanDirs = false
        var scanFiles = false
        for (pathToScan in pathsToScan) {
            if (relativePath.startsWith(pathToScan) || //
                    relativePath.length == pathToScan.length - 1 && pathToScan.startsWith(relativePath)) {
                // In a path that has a whitelisted path as a prefix -- can start scanning files
                scanFiles = true
                scanDirs = scanFiles
                break
            }
            if (pathToScan.startsWith(relativePath)) {
                // In a path that is a prefix of a whitelisted path -- keep recursively scanning dirs
                scanDirs = true
            }
        }
        if (scanDirs || scanFiles) {
            lastModified = Math.max(lastModified, dir.lastModified())
            val subFiles = dir.listFiles()
            for (subFile in subFiles!!) {
                if (subFile.isDirectory) {
                    // Recurse into subdirectory
                    scanDir(subFile, ignorePrefixLen, scanTimestampsOnly)
                } else if (scanFiles && subFile.isFile) {
                    // Scan file
                    val leafSuffix = "/" + subFile.name
                    scanFile(subFile, absolutePath + leafSuffix, relativePath + leafSuffix, scanTimestampsOnly)
                }
            }
        }
    }

    /**
     * Scan a zipfile for matching file path patterns. (Does not recurse into zipfiles within zipfiles.)
     */
    @Throws(IOException::class)
    private fun scanZipfile(zipfilePath: String, zipFile: ZipFile, scanTimestampsOnly: Boolean) {
        var timestampWarning = false
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            // Scan for matching filenames
            val entry = entries.nextElement()
            if (!entry.isDirectory) {
                // Only process file entries (zipfile indices contain both directory entries and
                // separate file entries for files within each directory, in lexicographic order)
                val path = entry.name
                var scanFile = false
                for (pathToScan in pathsToScan) {
                    if (path.startsWith(pathToScan)) {
                        // File path has a whitelisted path as a prefix -- can scan file
                        scanFile = true
                        break
                    }
                }
                if (scanFile) {
                    // Assumes that the clock used to timestamp zipfile entries is in sync with the
                    // clock used to timestamp regular file and directory entries in the classpath.
                    // Just in case, we check entry timestamps against the current time.
                    val entryTime = entry.time
                    lastModified = Math.max(lastModified, entryTime)
                    if (entryTime > System.currentTimeMillis() && !timestampWarning) {
                        val msg = zipfilePath + " contains modification timestamps after the current time"
                        // Log.warning(msg);
                        System.err.println(msg)
                        // Only warn once
                        timestampWarning = true
                    }
                    if (!scanTimestampsOnly) {
                        if (path.endsWith(".class")) {
                            // Found a classfile, open it as a stream and inspect header
                            zipFile.getInputStream(entry).use { inputStream -> readClassInfoFromClassfileHeader(inputStream) }
                        } else {
                            // For non-classfiles, match file paths against path patterns
                            for (fileMatcher in filePathMatchers) {
                                if (fileMatcher.pattern.matcher(path).matches()) {
                                    // There's a match, open the file as a stream and call the match processor
                                    zipFile.getInputStream(entry).use { inputStream -> fileMatcher.fileMatchProcessor.processMatch(path, path, inputStream) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Scan classpath for matching files. Call this after all match processors have been added.
     */
    private fun scan(scanTimestampsOnly: Boolean) {
        // long scanStart = System.currentTimeMillis();

        if (!scanTimestampsOnly) {
            classNameToClassInfo.clear()
            interfaceNameToInterfaceInfo.clear()
            annotationToClasses.clear()
            interfaceToClasses.clear()
        }

        try {
            // Iterate through path elements and recursively scan within each directory and zipfile
            for (pathElt in uniqueClasspathElements) {
                val path = pathElt.path
                if (pathElt.isDirectory) {
                    // Scan within dir path element
                    scanDir(pathElt, path.length + 1, scanTimestampsOnly)
                } else if (pathElt.isFile) {
                    val pathLower = path.toLowerCase()
                    if (pathLower.endsWith(".jar") || pathLower.endsWith(".zip")) {
                        // Scan within jar/zipfile path element
                        scanZipfile(path, ZipFile(pathElt), scanTimestampsOnly)
                    } else {
                        // File listed directly on classpath
                        scanFile(pathElt, path, pathElt.name, scanTimestampsOnly)

                        for (fileMatcher in filePathMatchers) {
                            if (fileMatcher.pattern.matcher(path).matches()) {
                                // If there's a match, open the file as a stream and call the match processor
                                FileInputStream(pathElt).use { inputStream ->
                                    fileMatcher.fileMatchProcessor.processMatch(path, pathElt.name,
                                            inputStream)
                                }
                            }
                        }
                    }
                } else {
                    // Log.info("Skipping non-file/non-dir on classpath: " + file.getCanonicalPath());
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        if (!scanTimestampsOnly) {
            // Finalize class hierarchy, then look for class matches
            finalizeClassHierarchy()
            for (classMatcher in classMatchers) {
                classMatcher.lookForMatches()
            }
        }
        // Log.info("Classpath " + (scanTimestampsOnly ? "timestamp " : "") + "scanning took: "
        //      + (System.currentTimeMillis() - scanStart) + " ms");
    }

    /**
     * Scan classpath for matching files. Call this after all match processors have been added.
     */
    fun scan() {
        scan(/* scanTimestampsOnly = */false)
    }

    /**
     * Returns true if the classpath contents have been changed since scan() was last called. Only considers
     * classpath prefixes whitelisted in the call to the constructor.
     */
    fun classpathContentsModifiedSinceScan(): Boolean {
        val lastModified = this.lastModified
        scan(/* scanTimestampsOnly = */true)
        return this.lastModified > lastModified
    }

    companion object {

        // ------------------------------------------------------------------------------------------------------

        /**
         * Recursively find all subclasses for each class; called by finalizeClassHierarchy.
         */
        private fun finalizeClassHierarchyRec(curr: ClassInfo) {
            // DFS through subclasses
            for (subclass in curr.directSubclasses) {
                finalizeClassHierarchyRec(subclass)
            }
            // Postorder traversal of curr node to accumulate subclasses
            for (subclass in curr.directSubclasses) {
                curr.allSubclasses.addAll(subclass.allSubclasses)
            }
        }

        /**
         * Read a string reference from a classfile, then look up the string in the constant pool.
         */
        @Throws(IOException::class)
        private fun readRefdString(inp: DataInputStream, constantPool: Array<Any?>): String {
            val constantPoolIdx = inp.readUnsignedShort()
            val constantPoolObj = constantPool[constantPoolIdx]
            return if (constantPoolObj is Int)
                constantPool[constantPoolObj] as String
            else
                constantPoolObj as String
        }

        // ------------------------------------------------------------------------------------------------------

        /**
         * Get a list of unique elements on the classpath as File objects, preserving order.
         * Classpath elements that do not exist are not returned.
         */
        val uniqueClasspathElements: ArrayList<File>
            get() {
                val pathElements = System.getProperty("java.class.path").split(File.pathSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val pathElementsSet = HashSet<String>()
                val pathFiles = ArrayList<File>()
                for (pathElement in pathElements) {
                    if (pathElementsSet.add(pathElement)) {
                        val file = File(pathElement)
                        if (file.exists()) {
                            pathFiles.add(file)
                        }
                    }
                }
                return pathFiles
            }
    }
}
