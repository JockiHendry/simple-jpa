package simplejpa.obfuscate

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Obfuscator {

    private static SecretKeySpec key = new SecretKeySpec((byte[])[-33, -35, -105, -123, -23, -21, 94, -18, -128, -4, -48, -42, -14, 125, 15, 2], 'AES')

    public static String generate(String text) {
        Cipher cipher = Cipher.getInstance('AES')
        cipher.init(Cipher.ENCRYPT_MODE, key)
        byte[] data = text.bytes
        return cipher.doFinal(data).encodeBase64().toString()
    }

    public static String reverse(String text) {
        if (text.startsWith('obfuscated:')) {
            text = text.substring(11)
        }
        byte[] data = text.decodeBase64()
        Cipher cipher = Cipher.getInstance('AES')
        cipher.init(Cipher.DECRYPT_MODE, key)
        return new String(cipher.doFinal(data))
    }

}
