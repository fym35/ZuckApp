package eu.konggdev.zuckapp;

import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    static byte[] toSignalKey(byte[] key) {
        if (key == null) {
            return null;
        }
        return switch (key.length) {
            case 33 -> key;
            case 32 -> {
                var result = new byte[33];
                System.arraycopy(key, 0, result, 1, key.length);
                result[0] = 5;
                yield result;
            }
            default -> throw new IllegalArgumentException("Invalid key size");
        };
    }

    static byte[] concatBytes(byte[] first, byte[] second)
    {
        if (first == null) return second;
        if (second == null) return first;
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


    static byte[] cipher(byte[] iv, byte[] input, int offset, int length, byte[] key, byte[] additionalData, boolean encrypt) {
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            var keySpec = new SecretKeySpec(key, "AES");
            var gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if(additionalData != null) {
                cipher.updateAAD(additionalData);
            }
            var outputLength = cipher.getOutputSize(length);
            var output = new byte[outputLength];
            var outputOffset = cipher.update(input, offset, length, output, 0);
            cipher.doFinal(output, outputOffset);
            return output;
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot %s data", throwable);
        }
    }

    static byte[] generateBytes(int length)
    {
        var bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
