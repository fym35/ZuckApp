package eu.konggdev.zuckapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //WhatsappRegistration registration = fetchStoredRegistration();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        private ActivityResultLauncher<Intent> launcher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK) {
                                Intent data = result.getData();
                                if (data != null) {
                                    String value = data.getStringExtra("key");
                                    // handle result here
                                }
                            }
                        }
                );
        startActivity(intent);
    }
}