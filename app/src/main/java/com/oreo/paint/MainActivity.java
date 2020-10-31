package com.oreo.paint;

import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.oreo.paint.help.ContentPusher;
import com.oreo.paint.help.KeyValManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    Paper paper;
    ContentPusher contentPusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // full screen setup
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // set up static resources
        KeyValManager.setContext(getApplicationContext());

        setContentView(R.layout.activity_full_screen);

        paper = findViewById(R.id.paper);
        paper.setBackgroundColor(Color.WHITE);



        ((PaperController) findViewById(R.id.paperController)).setPaper(paper);

//        getPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
//        contentPusher = new ContentPusher(paper);
//        new Handler().postDelayed(() -> contentPusher.connect(
//                () -> runOnUiThread(() -> Toast.makeText(this, "connected", Toast.LENGTH_LONG).show()),
//                () -> runOnUiThread(() -> Toast.makeText(this, "failed to connect", Toast.LENGTH_LONG).show())), 1000);
    }
}