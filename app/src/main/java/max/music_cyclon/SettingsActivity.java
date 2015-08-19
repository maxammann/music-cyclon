package max.music_cyclon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, LibraryService.class);
                startService(intent);
            }
        });
    }
}
