package eu.konggdev.zuckapp;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.graphics.Color;

import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.primitives.Bytes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPrivateKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.goterl.lazysodium.*;
import com.goterl.lazysodium.interfaces.Sign;

import org.bouncycastle.util.encoders.Hex;
import org.whispersystems.curve25519.Curve25519;

public class LoginActivity extends AppCompatActivity {
    //TODO: Split up methods that will be used outside logins into separate classes
    String WHATSAPP_MOBILE_LOGIN_CORE_URL = "https://v.whatsapp.net/v2/";
    String STATIC_TOKEN = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
    private static final byte[] REGISTRATION_PUBLIC_KEY = parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");
    private java.security.Security Security;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        findViewById(R.id.send_sms_btn).setOnClickListener(
                //TODO: Reimplement the splitting up of threads right here, either just make it smaller (in terms of less lines) or make the code use Async
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    sendCodeButtonHandler();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
    }

    public static byte[] parseHex(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    public void sendCodeButtonHandler() throws Exception {
        String countryCode = ((EditText) findViewById(R.id.country_code_input)).getText().toString();
        String phoneNumber = ((EditText) findViewById(R.id.phone_number)).getText().toString();

        String urlString = "";

        //TODO: Make all status strings android resources instead of literals
        //TODO: Finish missing requests

        if (countryCode.isEmpty() || phoneNumber.isEmpty()) {
            writeStatus("Please enter both the Phone number and the Country code");
            return;
        }

        //?: Phone number onboarding, looks like this prepares the whatsapp server for more data being sent about the number?
        String response = sendRequest(WHATSAPP_MOBILE_LOGIN_CORE_URL + "reg_onboard_abprop?"
                + "cc=" + countryCode
                + "&in=" + phoneNumber
                + "&ab_hash=");

        Log.i(TAG, response);

        if (response == null || response.isEmpty()) {
            writeStatus("Connection response error. Have you entered the correct phone number?");
            return;
        }

        JsonObject jsonResponse = getJson(response);

        if (jsonResponse == null || !jsonResponse.has("ab_hash")) {
            writeStatus("Incorrect JSON.");
            Log.e(TAG, response);
            return;
        }

        Log.i(TAG, response);

        String abHash = jsonResponse.get("ab_hash").getAsString();

        response = sendRequest(WHATSAPP_MOBILE_LOGIN_CORE_URL + "reg_onboard_abprop?"
                + "cc=" + countryCode
                + "&in=" + phoneNumber
                + "&ab_hash=" + abHash);

        jsonResponse = getJson(response);

        if (jsonResponse == null || !jsonResponse.has("ab_key")) {
            writeStatus("Incorrect JSON. Have you entered the correct phone number?");
            Log.e(TAG, response);
            return;
        }

        Log.i(TAG, response);

        //KeyPair noiseKeyPair = generateCurve25519KeyPair();
        //KeyPair identityKeyPair = generateCurve25519KeyPair();
        //KeyPair signedKeyPair = generateCurve25519KeyPair();



        CryptoKeychain keyChain = new CryptoKeychain();

        //var testKeyPair = generateCurve25519KeyPair();

        byte[] t = new byte[1];
        int registrationId = ThreadLocalRandom.current().nextInt(16380) + 1;
        //Which version to emulate, if the server returns old_version, changing this **COULD** fix it but it might also require changing the logic itself
        //For this very reason, the version for which the logic is written against is hardcoded into the code
        String version = "2.25.11.72";

        String baseEncData = "cc=" + countryCode
                + "&in=" + phoneNumber
                + "&lg=" + "cs"
                + "&lc=" + "CZ"
                + "&authkey=" + Base64.getUrlEncoder().encodeToString(keyChain.noiseKeyPair.getPublicKey())
                + "&e_regid=" + Base64.getUrlEncoder().encodeToString(ByteBuffer.allocate(4).putInt(registrationId).array())
                + "&e_keytype=" + Base64.getUrlEncoder().encodeToString(new byte[]{5})
                + "&e_ident=" + Base64.getUrlEncoder().encodeToString(keyChain.identityKeyPair.getPublicKey())
                + "&e_skey_id=" + Base64.getUrlEncoder().encodeToString(intToByteArr(registrationId, 3))
                + "&e_skey_val=" + Base64.getUrlEncoder().encodeToString(keyChain.signedKeyPair.getPublicKey())
                + "&e_skey_sig=" + Base64.getUrlEncoder().encodeToString(keyChain.signature)
                + "&fdid=" + UUID.randomUUID().toString().toUpperCase()
                + "&expid=" + Base64.getUrlEncoder().encodeToString(parseHex(UUID.randomUUID().toString().replaceAll("-", "")))
                + "&token=" + calculateToken(phoneNumber, version)
                + "&id=" + bufferToUrlEncodedHex(CryptoUtils.generateBytes(16));

        Log.i(TAG, calculateToken(phoneNumber, version));
        Log.i(TAG, calculateToken(phoneNumber, version));

        urlString = WHATSAPP_MOBILE_LOGIN_CORE_URL + "exist?ENC=" + encryptData(keyChain, baseEncData);;
        response = sendRequest(urlString);
        Log(urlString, response);

        urlString = WHATSAPP_MOBILE_LOGIN_CORE_URL + "code?ENC=" + encryptData(keyChain, baseEncData + "&method=sms&sim_mcc=000&sim_mnc=000&reason=");
        response = sendRequest(urlString);
        Log(urlString, response);

        writeStatus("Sent code successfully.");
    }

    public void Log(String urlString, String response) {
        Log.i(TAG, urlString);
        Log.i(TAG, response);
    }

    public String encryptData(CryptoKeychain keychain, String rawData) {
        var sharedKey = keychain.cipher.calculateAgreement(Hex.decode("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d"), keychain.sharedKeyPair.getPrivateKey());
        var buffer = CryptoUtils.cipher(new byte[12], rawData.getBytes(), 0, rawData.length(), sharedKey, null, true);
        return Base64.getUrlEncoder().encodeToString(CryptoUtils.concatBytes(keychain.sharedKeyPair.getPublicKey(), buffer));
    }

    public String calculateToken(String phoneNumber, String version) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(version.getBytes(StandardCharsets.UTF_8));
        String tokenOriginal = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            tokenOriginal = HexFormat.of().formatHex(md.digest());
        }
        else
        {
            tokenOriginal = formatHex(md.digest());
        }
        MessageDigest md2 = MessageDigest.getInstance("MD5");
        md2.update((STATIC_TOKEN + tokenOriginal + phoneNumber).getBytes(StandardCharsets.UTF_8));
        return formatHex(md2.digest());
    }

    public String formatHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }

        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xFF));
        }
        return hexString.toString();
    }

    private String bufferToUrlEncodedHex(byte[] data) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : data) {
            hexBuilder.append(String.format("%%%02X", b));
        }
        return hexBuilder.toString();
    }

    public static byte[] intToByteArr(int input, int length) {
        var result = new byte[length];
        for (var i = length - 1; i >= 0; i--) {
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }
        return result;
    }

    void writeStatus(String input) {
        runOnUiThread(() -> {
            ((TextView) findViewById(R.id.status_message)).setTextColor(Color.GREEN);
            ((TextView) findViewById(R.id.status_message)).setText(input);
        });
    }

    JsonObject getJson(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        JsonElement jsonElement = JsonParser.parseString(source);
        return jsonElement.getAsJsonObject();
    }

    public static String sendRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        connection.setRequestProperty("User-Agent", "WhatsApp/2.25.11.72 Android/13 Device/Nexus_4");
        connection.setRequestProperty("Accept", "text/json");
        connection.setRequestProperty("WaMsysRequest", "1");
        connection.setRequestProperty("request_token", UUID.randomUUID().toString());
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
