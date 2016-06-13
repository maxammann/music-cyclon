package max.music_cyclon;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A fragment which holds one {@link SynchronizeConfig} and displays it's content for
 * simple editing.
 */
public class SynchronizeConfigFragment extends PreferenceFragmentCompat {

    private String name;
    private PagerAdapter pagerAdapter;
    private SynchronizeConfig config;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sync_config);

        setRetainInstance(true);

        ConfigUpdater updater = new ConfigUpdater();

        JSONObject json = config.getJson();

        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);

            if (json.has(preference.getKey())) {
                if (preference instanceof TwoStatePreference) {
                    boolean data = json.optBoolean(preference.getKey());
                    ((TwoStatePreference) preference).setChecked(data);
                } else if (preference instanceof EditTextPreference) {
                    String data = json.optString(preference.getKey());
                    ((EditTextPreference) preference).setText(data);
                }
            }

            preference.setOnPreferenceChangeListener(updater);
        }

        // The remove listener
        Preference removePreference = findPreference("remove");
        removePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getPagerAdapter().getCount() == 1) {
                    return false;
                }
                getPagerAdapter().remove(getName());
                getPagerAdapter().notifyDataSetChanged();
                return true;
            }
        });
    }

    @Override
    public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public PagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    public void setPagerAdapter(PagerAdapter pagerAdapter) {
        this.pagerAdapter = pagerAdapter;
    }

    public void setConfig(SynchronizeConfig config) {
        this.config = config;
    }

    public SynchronizeConfig getConfig() {
        return config;
    }

    private class ConfigUpdater implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            String key = preference.getKey();
            JSONObject config = getConfig().getJson();

            try {
                config.put(key, o);
                return true;
            } catch (JSONException e) {
                return false;
            }
        }
    }
}
