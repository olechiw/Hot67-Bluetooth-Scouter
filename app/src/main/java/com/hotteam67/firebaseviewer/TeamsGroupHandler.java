package com.hotteam67.firebaseviewer;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import com.annimon.stream.Stream;

import org.hotteam67.common.DarkNumberPicker;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamsGroupHandler
{

    public static final String TEAM_GROUP_QUALS = "Qualification Match";
    public static final String TEAM_GROUP_ELIMS = "Alliance";
    public static final String TEAM_GROUP_CUSTOM = "Custom";

    private Integer currentId;
    private String currentType = TEAM_GROUP_QUALS;
    private Context context;
    private List<String> customTeams = new ArrayList<>();

    public TeamsGroupHandler(Context c)
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
        List<String> itemsArrayList = Arrays.asList(context.getResources().getStringArray(R.array.teamGroupTypesArray));
        spinner.setSelection(itemsArrayList.indexOf(currentType));

        ListView customTeamsView = v.findViewById(R.id.customTeamsOutput);
        ImageButton addButton = v.findViewById(R.id.addButton);
        ImageButton removeButton = v.findViewById(R.id.removeButton);
        EditText input = v.findViewById(R.id.inputEditText);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_list_item_1,
                customTeams);
        customTeamsView.setAdapter(adapter);

        addButton.setOnClickListener(x ->
        {
            if (input.getText().toString().trim().isEmpty() || input.getText().toString().equals("0"))
                return;
            adapter.add(input.getText().toString());
            input.setText("");
        });
        removeButton.setOnClickListener(x ->
        {
            adapter.remove(adapter.getItem(adapter.getCount() - 1));
        });


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                if (spinner.getItemAtPosition(i).equals(TEAM_GROUP_CUSTOM))
                {
                    picker.setVisibility(View.INVISIBLE);
                    v.findViewById(R.id.customTeamsLayout).setVisibility(View.VISIBLE);
                    v.findViewById(R.id.customTeamsOutput).setVisibility(View.VISIBLE);
                }
                else
                {
                    picker.setVisibility(View.VISIBLE);
                    v.findViewById(R.id.customTeamsLayout).setVisibility(View.INVISIBLE);
                    v.findViewById(R.id.customTeamsOutput).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {

            }
        });

        if (currentType.equals(TEAM_GROUP_CUSTOM))
            picker.setVisibility(View.INVISIBLE);
        else
        {
            v.findViewById(R.id.customTeamsLayout).setVisibility(View.INVISIBLE);
            v.findViewById(R.id.customTeamsOutput).setVisibility(View.INVISIBLE);
        }
        return v;
    }

    public void LoadFromView(View v)
    {
        if (v == null)
            return;
        DarkNumberPicker picker = v.findViewById(R.id.teamsGroupId);
        currentId = picker.getValue();

        Spinner spinner = v.findViewById(R.id.teamsGroupType);
        currentType = spinner.getSelectedItem().toString();

        if (currentType.equals(TEAM_GROUP_CUSTOM))
        {
            customTeams = new ArrayList<>();
            ListView customTeamsView = v.findViewById(R.id.customTeamsOutput);
            if (customTeamsView.getAdapter() instanceof ArrayAdapter)
            {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) customTeamsView.getAdapter();
                for (int i = 0; i < adapter.getCount(); ++i)
                {
                    customTeams.add(adapter.getItem(i));
                }
            }
        }
    }

    public Integer GetId()
    {
        return currentId;
    }

    public String GetType()
    {
        return currentType;
    }

    public List<String> GetCustomTeams()
    {
        return customTeams;
    }

    public void SetCustomTeams(List<String> teams)
    {
        customTeams = teams;
    }

    public void SetId(Integer id) { currentId = id; }

    public void SetType(String type) { currentType = type; }
}
