package max.music_cyclon;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import org.json.JSONException;

public class SynchronizeConfigFragment extends PreferenceFragmentCompat {

    private String name;
    private PagerAdapter pagerAdapter;
    private Config config;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sync_config);

        setRetainInstance(true);

        ConfigUpdater updater = new ConfigUpdater();

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (config.getJson().has(preference.getKey())) {
                if (preference instanceof TwoStatePreference) {
                    ((TwoStatePreference) preference).setChecked(config.getJson().optBoolean(preference.getKey()));
                } else if (preference instanceof EditTextPreference) {
                    ((EditTextPreference) preference).setText(config.getJson().optString(preference.getKey()));
                }
            }

            preference.setOnPreferenceChangeListener(updater);
        }

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

    public void setConfig(Config config) {
        this.config = config;
    }

    private class ConfigUpdater implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            String key = preference.getKey();
            try {
                config.getJson().put(key, o);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return true;
        }
    }
}
