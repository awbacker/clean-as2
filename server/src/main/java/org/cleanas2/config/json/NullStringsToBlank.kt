package org.cleanas2.config.json

import org.boon.core.TypeType
import org.boon.core.reflection.FastStringUtils
import org.boon.core.reflection.fields.FieldAccess
import org.boon.json.serializers.CustomFieldSerializer
import org.boon.json.serializers.JsonSerializerInternal
import org.boon.primitive.CharBuf

/**
 * When serializing, converts null values to blanks when they are found.  In all other cases it defers to the standard
 * boon handling.  The field name/value writing lines copied from FieldSerializerUseAnnotationsImpl.java
 *
 * This might be able to be written as a CustomObjectSerializer#string# but we should leave this here as an example
 * at least,
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class NullStringsToBlank : CustomFieldSerializer {
    override fun serializeField(serializer: JsonSerializerInternal, parent: Any, fieldAccess: FieldAccess, builder: CharBuf): Boolean {
        if (fieldAccess.typeEnum() == TypeType.STRING) {
            if (fieldAccess.getValue(parent) == null) {
                builder.addJsonFieldName(FastStringUtils.toCharArray(fieldAccess.alias()))
                builder.addQuoted("") // do this directly. No possible bad chars, so no need for addJsonEscapedString
                return true
            }
        }
        return false
    }
}
