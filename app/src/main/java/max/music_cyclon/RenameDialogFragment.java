package max.music_cyclon;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

public class RenameDialogFragment extends DialogFragment {

    public static final DialogInterface.OnClickListener STUB_CLICK = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
        }
    };

    private PagerAdapter adapter;
    private String previousName;

    public void setAdapter(PagerAdapter adapter) {
        this.adapter = adapter;
    }

    public void setPreviousName(String previousName) {
        this.previousName = previousName;
    }

    private EditText newName;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        newName = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getActivity().getString(R.string.rename))
                .setView(newName)
                .setPositiveButton(android.R.string.ok, new ApplyRename())
                .setNegativeButton(android.R.string.cancel, STUB_CLICK);

        return builder.create();
    }

    private class ApplyRename implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            adapter.rename(previousName, newName.getText().toString());
            adapter.notifyDataSetChanged();
        }
    }
}