Clean AS2 
===================

Clean AS2 is a Java 7 based AS2 EDI server inspired by the venerable [Open AS2](http://sourceforge.net/projects/openas2/ "Open AS2") project.  Clean AS2 was developed by me while working for [Power E2E](http://powere2e.com/) in Shanghai.   The code is Copyright [Power E2E](http://powere2e.com/) and released under the BSD license. It is still a work in progress, and contributions are welcome.  This original code base is used internally, in a modified form, as part of one of our products.

The OpenAS2 code base was confusing to work with, and configuration was, for me, quite painful.  While trying to understand AS2 and struggling with the code, I slowly began updating parts to make the logic easier to follow.  After some time struggling with this and reverting many changes (my first java project in years), I decided to take steps to re-write it from scratch, following the path they had laid down.  Java has changed a lot since 2007!  

*Development was done on OSX and against a Cyclone AS2 Server.  As such, all scripts are in bash.  Windows versions are much appreciated.*

## Features ##
 * Easier configuration in JSON
 * Command line interface re-written
 * Java 1.7/1.8 compatible
 * Gradle 2.2+ for building (not using gradle wrapper yet)
 * Latest Bouncy Castle libraries (`jdk15on`) and changes to support them
 * Read `P12` and `cer` files for company and partner, respectively
 * HTTP server re-written to use [Apache Http Components](http://hc.apache.org/)
 * Message bus by [MBassador](https://github.com/bennidi/mbassador)
 * DI by [Google Guice](https://github.com/google/guice) *with unfortunate guava dependency*
 * Helpers by [Boon](https://github.com/boonproject/boon) (`list()`, `map()`, `datarepo`, `json`, ...)

Hopefully the new code is easier to follow and extended/debug.   If you have any suggestions for improving the code base, build process, or anything else, please send me an email `@gmail` or post an issue.  

Missing/broken Features
-----------------------

 * Tests!
 * Retry failure system is partially implemented
 * Real plugin system (system events, monitoring)
 * Servelet or HTTP based admin panel 
 
Running the project
-------------------

Download the repo and run gradle.  `-q` must be used.  This will read configuration information from the `home` directory.

    $> ~/> gradle -q run

If you want to run the server from a shell script, follow these steps.  This will read the configuration information from the `./home/` directory.  

    $> gradle compileToBin   // writes all files to ./bin
    $> ./run.sh
      
If you would like to run two servers at the same time, and test sending files back and forth, there is a script for that too!  This process will:

 * Generate private/public certificates for two different servers
 * Create configuration files for both servers
 * Create 2 shells scripts to run, to start the two servers

Here are the steps:

    $> cd ./scripts
    
    $> ./create-configs.sh [name-1] [name-2]
    $> ./home/run-name-1.sh  (in terminal 1)
    $> ./home/run-name-2.sh  (in terminal 2)
    
    # in terminal 1, create a file in the outbox of the first server
    $> echo "from name1 to name2" >> ./home/name-1/outbox/name-2/test.txt
    

Configuration File
------------------

The configuration file is a basic JSON file, `config.json`:

    {
        "server": {
            "directories": {
                "certificates": "{home}/certs/",
                "system": "{home}/system/"
            },
            "url": "http://10.204.3.21",
            "ports": {
                "receiveFile": 10090,
                "receiveMdn": 10091
            }
        },

        "company": {
            "as2id": "ab-client",
            "name": "Power E2E Test Company",
            "email": "ab-client@powere2e.com",
            "certificate": {
                "file": "ab-client.p12",
                "password": "12345678"
            }
        },

        "partners": [
            {
                "as2id": "ab-server",
                "name": "Someone AAA",
                "description": "On the old cyclone test server",
                "email": "someone@oldas2.server.com",
                "certificate": "ab-server.cer",
                "sendSettings": {
                    "mdnMode": "standard",
                    "url": "http://10.204.2.97:4080/exchange/ab-server",
                    "signAlgorithm": "sha1",
                    "mdnOptions": "signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, sha1"
                }
            }
        ]
    }

Cyclone AS2 Notes
-----------------

The version of cyclone we work with seems to intentionally ignore the provided Async MDN URL, instead sending it back to the URL registered with cyclone.  This makes the file receive code a bit uglier, since it has to get the request fully before it can open it and determine for sure if it is an MDN or a regular incoming file.