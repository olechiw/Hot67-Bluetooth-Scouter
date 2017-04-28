package org.hotteam67.bluetoothscouter;

import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.widget.NumberPicker;
import android.widget.*;
import android.view.*;
import android.content.*;
import android.util.*;
import android.app.*;
import android.content.res.*;

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
        this.setOrientation(LinearLayout.VERTICAL);

        mainText = new EditText(getContext());
        mainText.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL);
        InputFilter filter = new InputFilter()
        {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
            {
                int i = Integer.valueOf(source.toString());
                if ((minimum < i) && (i < maximum))
                    return null;
                else
                    return "";
            }
        };
        mainText.setFilters(new InputFilter[] { filter });

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mainText.setLayoutParams(params);

        upButton = new Button(getContext());
        upButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (getValue() < maximum)
                    setValue(getValue() + 1);
            }
        });
        upButton.setLayoutParams(params);

        downButton = new Button(getContext());
        downButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (getValue() > minimum)
                    setValue(getValue() - 1);
            }
        });
        downButton.setLayoutParams(params);

        addView(upButton);
        addView(mainText);
        addView(downButton);
    }


    public int getValue()
    {
        return Integer.valueOf(mainText.getText().toString());
    }

    public void setValue(int value)
    {
        mainText.setText(value);
    }
}
