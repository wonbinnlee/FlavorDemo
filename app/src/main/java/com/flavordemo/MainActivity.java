package com.flavordemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.alibrary.ALibrary;
import com.blibrary.BLibrary;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.text);
        textView.setText(ALibrary.getStr() + ":" + BLibrary.getStr());
    }
}
