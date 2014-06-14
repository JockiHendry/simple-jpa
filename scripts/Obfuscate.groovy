import simplejpa.obfuscate.Obfuscator

target(name: 'obfuscate', description: "Encrypt or decrypt a value used by simple-jpa", prehook: null, posthook: null) {

    def helpDescription = """
DESCRIPTION
    obfuscate

    Generate encrypted value that can be placed in simplejpa.properties or
    decrypt such value.
    The purpose of this command is to obfuscate a value. It should not be used
    to protect very sensitive information.

SYNTAX
    obfuscate -generate=[value]
    obfuscate -reverse=[value]

ARGUMENTS
    value
        String value (for generate operation) or Base64 encoded string (for
        reverse operation).

DETAILS
    The generate operation will generate Base64 encoded string.  You can use
    this string in simplejpa.properties by appending it with 'obfuscated:'.
    simple-jpa will reverse such string into its plain value.

EXAMPLES
    griffon obfuscate -generate='Solid Snake is cool'
    griffon obfuscate -reverse=obfuscated:SxqGXgetBRJi+AU8FvGGlg==

"""

    if (argsMap['params']=='info') {
        println helpDescription
        return
    }

    if (!argsMap['generate'] && !argsMap['reverse']) {
        println '''

You didn't specify all required arguments.  Please see the following
description for more information.

'''
        println helpDescription
        return

    }

    if (argsMap['generate']) {
        println "\nResult:\nobfuscated:${Obfuscator.generate(argsMap['generate'].toString())}"
    }

    if (argsMap['reverse']) {
        println "\nResult:\n${Obfuscator.reverse(argsMap['reverse'].toString())}"
    }

    println ""
}

setDefaultTarget('obfuscate')
