package org.hotteam67.common;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by Jakob on 4/27/2017.
 */

public class DarkNumberPicker extends LinearLayout
{
    public DarkNumberPicker(android.content.Context context)
    {
        super(context);
        Construct();
    }

    public DarkNumberPicker(android.content.Context context, android.util.AttributeSet attributeSet)
    {
        super(context, attributeSet);
        Construct();
    }

    private EditText mainText;
    private int minimum = 0;
    private int maximum = 100;

    private void Construct()
    {
        mainText = new EditText(getContext());
        mainText.setInputType(InputType.TYPE_CLASS_NUMBER);


        InputFilter filter = (source, start, end, dest, dstart, dend) ->
        {
            if (source.toString().trim().isEmpty())
                return source;
            int i;
            try
            {
                i = Integer.valueOf(source.toString());
            } catch (Exception e) {
                return String.valueOf(minimum);
            }

            //android.util.Log.d("[DarkPicker]", "Integer value: " + i);

            if ((i >= minimum && i <= maximum))
                return String.valueOf(i);
            else if (i < minimum)
            {
                return String.valueOf(minimum);
            }
            else
            {
                return String.valueOf(maximum);
            }
        };
        mainText.setFilters(new InputFilter[] { filter });

        mainText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty())
                    return;
                int i = 0;
                try
                {
                    i = Integer.valueOf(String.valueOf(mainText.getText().toString()));
                } catch (Exception e) {
                    mainText.setText(String.valueOf(minimum));
                }

                //android.util.Log.d("[DarkPicker]", "Integer value: " + i);

                if (i < minimum)
                {
                    mainText.setText(String.valueOf(minimum));
                }
                else if (i > maximum)
                {
                    mainText.setText(String.valueOf(maximum));
                }
            }
        });

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mainText.setLayoutParams(params);
        mainText.setText("0");

        Button upButton = new Button(getContext());
        upButton.setOnClickListener(v -> setValue(getValue() + 1));
        upButton.setText("+");
        upButton.setLayoutParams(params);

        Button downButton = new Button(getContext());
        downButton.setOnClickListener(v -> setValue(getValue() - 1));
        downButton.setText("-");
        downButton.setLayoutParams(params);

        addView(upButton);
        addView(mainText);
        addView(downButton);
    }

    public void setMinimum(int value)
    {
        minimum = value;
    }

    public void setMaximum(int value)
    {
        maximum = value;
    }


    public int getValue()
    {
        if (mainText.getText().toString().trim().isEmpty())
            return 0;

        return Integer.valueOf(mainText.getText().toString());
    }

    public void setValue(Integer value)
    {
        if (value > 0)
            mainText.setText(value.toString());
        else
            mainText.setText("0");
    }
}
