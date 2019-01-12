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
 *  A manually themed number picker that can be easily made horizontal or vertical
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
    Button upButton;
    Button downButton;


    Runnable valueChangedListener;
    public void setOnValueChangedListener(Runnable r)
    {
        valueChangedListener = r;
    }

    /**
     * Build the number picker, adding the edittext and two buttons
     */
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
        mainText.setMinEms(2);

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
                if (valueChangedListener != null) valueChangedListener.run();
            }
        });

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        mainText.setLayoutParams(params);
        mainText.setText("0");

        upButton = new Button(getContext());
        upButton.setOnClickListener(v -> setValue(getValue() + 1));
        upButton.setText("+");
        upButton.setLayoutParams(params);

        downButton = new Button(getContext());
        downButton.setOnClickListener(v -> setValue(getValue() - 1));
        downButton.setText("-");
        downButton.setLayoutParams(params);

        addView(upButton);
        addView(mainText);
        addView(downButton);
    }

    /**
     * Configure the minimum of the numberpicker
     * @param value min
     */
    public void setMinimum(int value)
    {
        minimum = value;
    }

    /**
     * Configure the maximum of the numberpicker
     * @param value max
     */
    public void setMaximum(int value)
    {
        maximum = value;
    }


    /**
     * Get the current value of the number picker
     * @return value
     */
    public int getValue()
    {
        if (mainText.getText().toString().trim().isEmpty())
            return 0;

        return Integer.valueOf(mainText.getText().toString());
    }

    /**
     * Set the current value of the number picker
     * @param value value
     */
    public void setValue(Integer value)
    {
        if (value > 0)
            mainText.setText(value.toString());
        else
            mainText.setText("0");
    }

    public void setButtonTextColor(int color) {
        upButton.setTextColor(color);
        downButton.setTextColor(color);
    }
}
