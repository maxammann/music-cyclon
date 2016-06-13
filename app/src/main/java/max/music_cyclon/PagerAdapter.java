package max.music_cyclon;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the fragments for the configuration of all {@link SynchronizeConfig}s
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    private final List<String> configs = new ArrayList<>();

    private final Map<String, SynchronizeConfig> configData = new HashMap<>();

    public PagerAdapter(List<SynchronizeConfig> configs , FragmentManager fm) {
        super(fm);


        for (SynchronizeConfig config : configs) {
            this.configs.add(config.getName());
            this.configData.put(config.getName(), config);
        }
    }

    public void save(OutputStream os) throws JSONException, IOException {
        SynchronizeConfig.save(configData.values(), os);
    }

    public boolean add(String name) {
        configData.put(name, new SynchronizeConfig(name));
        return configs.add(name);
    }

    public void remove(String name) {
        configData.remove(name);
        configs.remove(name);
    }

    @Override
    public Fragment getItem(int i) {
        SynchronizeConfigFragment fragment = new SynchronizeConfigFragment();

        String name = getConfigs().get(i);

        fragment.setName(name);
        fragment.setPagerAdapter(this);
        fragment.setConfig(configData.get(name));
        return fragment;
    }

    @Override
    public int getCount() {
        return getConfigs().size();
    }

    @Override
    public int getItemPosition(Object object) {
//        http://stackoverflow.com/a/10399127
        return PagerAdapter.POSITION_NONE;
    }

    public List<String> getConfigs() {
        return configs;
    }

    public List<SynchronizeConfig> getConfigData() {
        return new ArrayList<>(configData.values());
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getConfigs().get(position);
    }
}
