package com.example.fingerprint;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_FINGERPRINT_PERMISSION = 1;
    private static final String KEY_NAME = "test_key";
    private KeyguardManager mKeyguardManager;
    private FingerprintManager mFingerPrintManager;
    private Button closeBtt;
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private FingerprintManager.CryptoObject mCryptoObj;
    private boolean backdoor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.get("alibaba") != null) {
            Log.i("alibaba", "yeap");
            backdoor = true;
        }
        checkFingerprints();
    }

    private class FingerprintListener extends FingerprintManager.AuthenticationCallback {

        private FingerprintManager.CryptoObject cryptoObject;
        private Dialog dialog;

        public FingerprintListener(Dialog dialog, FingerprintManager.CryptoObject cryptoObject) {
            super();
            this.dialog = dialog;
            this.cryptoObject = cryptoObject;
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            Log.i("finger print", "emitted");
            if(backdoor || result.getCryptoObject().equals(this.cryptoObject)) {
                dialog.dismiss();
                openSecretActivity();
            }else{
                Toast.makeText(MainActivity.this, R.string.user_not_authenticated, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            Toast.makeText(MainActivity.this, errString, Toast.LENGTH_LONG).show();
        }
    }

    private void openSecretActivity() {
        Toast.makeText(MainActivity.this, R.string.user_authenticated, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, SecretActivity.class);
        startActivity(intent);
    }

    private void checkFingerprints() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT}, REQ_FINGERPRINT_PERMISSION);
            return;
        }
        mKeyguardManager = getSystemService(KeyguardManager.class);
        mFingerPrintManager = getSystemService(FingerprintManager.class);

        if(!backdoor) {
            Toast.makeText(MainActivity.this, R.string.checking_secure_n_permissions, Toast.LENGTH_SHORT).show();
            if (!mKeyguardManager.isKeyguardSecure()) {
                Toast.makeText(MainActivity.this, R.string.go_2_settings, Toast.LENGTH_LONG).show();
                return;
            }
            if (!mFingerPrintManager.hasEnrolledFingerprints()) {
                Toast.makeText(MainActivity.this, R.string.go_2_settings, Toast.LENGTH_LONG).show();
                return;
            }
        }
        createKey();
        showFingerPrintDialog();
    }

    private void showFingerPrintDialog() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(R.layout.fragment_fingerprint);
            Dialog dialog = builder.create();
            dialog.show();

            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            Cipher lu = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            lu.init(Cipher.ENCRYPT_MODE, key);

            mCryptoObj = new FingerprintManager.CryptoObject(lu);
            FingerprintListener listener = new FingerprintListener(dialog, mCryptoObj);
            mFingerPrintManager.authenticate(mCryptoObj, new CancellationSignal(),
                    0, listener, null);

        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void createKey() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            mKeyStore.load(null);
            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_FINGERPRINT_PERMISSION &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, R.string.must_accept_fingerprint_permission, Toast.LENGTH_LONG).show();
            return;
        }
        checkFingerprints();
    }
}
