package org.hotteam67.bluetoothscouter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button serverButton;
    Button scoutButton;
    Button schemaButton;
    Button matchesButton;

    public static final String ORIENTATION = "orientation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverButton = (Button) findViewById(R.id.serverButton);
        scoutButton = (Button) findViewById(R.id.scoutButton);
        schemaButton = (Button) findViewById(R.id.schemaButton);
        matchesButton = (Button) findViewById(R.id.matchesButton);

        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(getApplicationContext(), ServerActivity.class);
                startActivity(serverIntent);
            }
        });
        scoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scoutIntent = new Intent(getApplicationContext(), ScoutActivity.class);
                startActivity(scoutIntent);
            }
        });
        schemaButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent schemaIntent = new Intent(getApplicationContext(), SchemaActivity.class);
                startActivity(schemaIntent);
            }
        });
        matchesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent matchesIntent = new Intent(getApplicationContext(), MatchesActivity.class);
                startActivity(matchesIntent);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
            return;
        }
    }
}
