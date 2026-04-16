package eu.konggdev.zuckapp;

import static eu.konggdev.zuckapp.CryptoUtils.toSignalKey;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

public class CryptoKeychain {
    Curve25519 cipher;
    public Curve25519KeyPair noiseKeyPair;
    public Curve25519KeyPair identityKeyPair;
    public Curve25519KeyPair signedKeyPair;
    public Curve25519KeyPair sharedKeyPair;
    public byte[] signature;
    public CryptoKeychain() {
        cipher = Curve25519.getInstance(Curve25519.BEST);
        noiseKeyPair = cipher.generateKeyPair();
        identityKeyPair = cipher.generateKeyPair();
        signedKeyPair = cipher.generateKeyPair();
        sharedKeyPair = cipher.generateKeyPair();
        signature = cipher.calculateSignature(identityKeyPair.getPrivateKey(), toSignalKey(signedKeyPair.getPublicKey()));
    }
}
