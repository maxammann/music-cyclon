package max.music_cyclon;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Holds the fragments for the configuration of all {@link SynchronizeConfig}s
 */
public class PagerAdapter extends FragmentStatePagerAdapter {

    private final List<SynchronizeConfig> configs = new ArrayList<>();

    public PagerAdapter(Collection<SynchronizeConfig> configs , FragmentManager fm) {
        super(fm);

        this.configs.addAll(configs);
    }

    public void save(OutputStream os) throws JSONException, IOException {
        SynchronizeConfig.save(configs, os);
    }

    public void add(String name) {
        configs.add(new SynchronizeConfig(name));
        notifyDataSetChanged();
    }

    public void remove(String name) {
        int index = indexOf(name);

        if (index < 0) {
            return;
        }

        configs.remove(index);
        notifyDataSetChanged();
    }

    public void rename(String name, String newName) {
        int index = indexOf(name);

        if (index < 0) {
            return;
        }

        SynchronizeConfig config = configs.get(index);

        config.setName(newName);

        notifyDataSetChanged();
    }

    public int indexOf(String name) {
        for (int i = 0, size = configs.size(); i < size; i++) {
            SynchronizeConfig config = configs.get(i);
            if (config.getName().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public Fragment getItem(int i) {
        SynchronizeConfigFragment fragment = new SynchronizeConfigFragment();

        SynchronizeConfig config = getConfigs().get(i);

        fragment.setName(config.getName());
        fragment.setPagerAdapter(this);
        fragment.setConfig(config);
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

    public List<SynchronizeConfig> getConfigs() {
        return configs;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return configs.get(position).getName();
    }
}
