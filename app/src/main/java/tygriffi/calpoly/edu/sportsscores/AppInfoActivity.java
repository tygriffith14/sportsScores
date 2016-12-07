package tygriffi.calpoly.edu.sportsscores;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Tyler on 11/29/2016.
 */

public class AppInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("App Info");
        setContentView(R.layout.application_info);
    }
}
