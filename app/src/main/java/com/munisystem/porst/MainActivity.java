package com.munisystem.porst;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Intent intent = new Intent(this, QRReaderActivity.class);
//        startActivity(intent);
        Intent intent = new Intent(this, NFCReaderActivity.class);
        startActivity(intent);
    }
}
