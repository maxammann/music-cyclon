package max.music_cyclon;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class InfoPreference extends DialogPreference {

    public InfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogMessage("Info");
    }

}
