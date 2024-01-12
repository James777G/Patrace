package com.arm.pa.paretrace.Activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.arm.pa.paretrace.R;

public class MainActivity extends Activity {

    private RadioButton defaultModeRadioButton;
    private RadioButton testModeRadioButton;
    private EditText urlEditText;
    private EditText deviceIdEditText;
    private Button proceedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        defaultModeRadioButton = (RadioButton) findViewById(R.id.default_mode_radio_button);
        testModeRadioButton = (RadioButton) findViewById(R.id.test_mode_radio_button);
        urlEditText = (EditText) findViewById(R.id.url_edit_text);
        deviceIdEditText = (EditText) findViewById(R.id.device_id_edit_text);
        proceedButton = (Button) findViewById(R.id.proceed_button);

        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onProceedClicked();
            }
        });
    }

    private void onProceedClicked() {
        if (testModeRadioButton.isChecked()) {
            String url = urlEditText.getText().toString();
            String deviceId = deviceIdEditText.getText().toString();

            if (url.isEmpty() || deviceId.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields for test mode.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, SelectActivity.class);
            intent.putExtra("mode", "test");
            intent.putExtra("fileUri", url);
            intent.putExtra("testDevice", deviceId);
            startActivity(intent);
        } else if (defaultModeRadioButton.isChecked()) {
            Intent intent = new Intent(MainActivity.this, SelectActivity.class);
            intent.putExtra("mode", "default");
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "Please select a mode.", Toast.LENGTH_SHORT).show();
        }
    }
}