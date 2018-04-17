package com.hotteam67.firebaseviewer.data;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Outliers
{
    public static int getQuartile(int[] array, float quartileType)
    {
        //Calculate the size of the new array based on the quartile specified.
        int newArraySize = (int)((array.length)*(quartileType*25/100));
        //Copy only the number of rows that will fit the new array.
        int[] values =  Arrays.copyOf(array, newArraySize);
        return values[values.length - 1];
    }

    public static boolean IsBelowQuartile(String value, List<String> values, int quartile)
    {
        if ((quartile > 4) || (quartile < 1))
            return false;

        List<Integer> intValues = new ArrayList<>();
        for (String v : values)
        {
            if (v.equals("true") || v.equals("false"))
            {
                intValues.add((Boolean.valueOf(v)) ? 1 : 0);
            }
            else
            {
                try
                {
                    intValues.add(Integer.valueOf(v));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        try
        {
            int[] integerArray = Stream.of(intValues).mapToInt(Integer::intValue).sorted().toArray();

            double q = getQuartile(integerArray, quartile);

            Integer intValue;
            if (value.equals("true") || value.equals("false"))
                intValue = (Boolean.valueOf(value)) ? 1 : 0;
            else
                intValue = Integer.valueOf(value);

            return (q > intValue);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
