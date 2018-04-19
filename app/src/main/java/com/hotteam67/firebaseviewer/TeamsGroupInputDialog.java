package com.hotteam67.firebaseviewer;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;

import org.hotteam67.common.DarkNumberPicker;

public class TeamsGroupInputDialog {

    public static final String TEAM_GROUP_QUALS = "Qualification Match";
    public static final String TEAM_GROUP_ELIMS = "Alliance";

    private Integer currentId;
    private String currentType;
    private Context context;

    public TeamsGroupInputDialog(Context c)
    {
        currentId = 0;
        context = c;
    }

    public View GetView()
    {
        View v = ((MainActivity)context).getLayoutInflater().inflate(R.layout.teamgroup_dialog_layout, null);

        DarkNumberPicker picker = v.findViewById(R.id.teamsGroupId);
        picker.setMinimum(0);
        picker.setMaximum(500);
        picker.setValue(currentId);

        Spinner spinner = v.findViewById(R.id.teamsGroupType);
        if (TEAM_GROUP_QUALS.equals(currentType))
            spinner.setSelection(0);
        else
            spinner.setSelection(1);

        return v;
    }

    public void LoadFromView(View v)
    {
        DarkNumberPicker picker = v.findViewById(R.id.teamsGroupId);
        currentId = picker.getValue();

        Spinner spinner = v.findViewById(R.id.teamsGroupType);
        currentType = spinner.getSelectedItem().toString();
    }

    public Integer GetId()
    {
        return currentId;
    }

    public String GetType()
    {
        return currentType;
    }
}
