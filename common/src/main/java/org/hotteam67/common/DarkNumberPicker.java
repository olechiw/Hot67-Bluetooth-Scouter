package org.hotteam67.common;

import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.widget.*;
import android.view.*;

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

    EditText mainText;
    Button upButton;
    Button downButton;
    int minimum = 0;
    int maximum = 100;

    public void Construct()
    {
        mainText = new EditText(getContext());
        mainText.setInputType(InputType.TYPE_CLASS_NUMBER);


        InputFilter filter = new InputFilter()
        {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
            {
                int i = 0;
                try
                {
                    i = Integer.valueOf(String.valueOf(source.toString()));
                } catch (Exception e) {
                    //android.util.Log.e("[DarkPicker]", "Failed to create integer value of : " + source.toString(), e);
                }

                //android.util.Log.d("[DarkPicker]", "Integer value: " + i);

                if ((i >= minimum && i <= maximum))
                    return null;
                else
                {
                    return String.valueOf(minimum);
                }
            }
        };
        mainText.setFilters(new InputFilter[] { filter });

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mainText.setLayoutParams(params);
        mainText.setText("0");

        upButton = new Button(getContext());
        upButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setValue(getValue() + 1);
            }
        });
        upButton.setText("+");
        upButton.setLayoutParams(params);

        downButton = new Button(getContext());
        downButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int i = getValue() - 1;
                if (i < minimum)
                    setValue(maximum);
                else
                    setValue(i);
            }
        });
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
        return Integer.valueOf(mainText.getText().toString());
        /*
        if (!mainText.getText().toString().trim().isEmpty())

        else
            return 0;
            */
    }

    public void setValue(Integer value)
    {
        if (value > 0)
            mainText.setText(value.toString());
        else
            mainText.setText("0");
    }
}
