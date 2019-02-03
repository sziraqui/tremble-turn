package com.trembleturn.trembleturn;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class MapsSearchActivity extends AppCompatActivity {

    private static final String TAG = MapsSearchActivity.class.getSimpleName();

    private EditText etDest;
    private ImageView ivSearch;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_search);

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.et_dest:
                //set map marker
                break;
            case R.id.iv_search:
                // set marker
                // get directions
                // show start FAB
                break;

        }
    }
}
