package org.hotteam67.bluetoothscouter;

/**
 * Created by Jakob on 3/26/2017.
 */

public class SchemaHandler
{
    public final class Types
    {
        public static final int TYPE_BOOLEAN = 1;
        public static final int TYPE_STRING = 2;
        public static final int TYPE_INTEGER = 3;
        public static final int TYPE_HEADER = 4;
        public static final int TYPE_CHOICE = 5;
    }

    public static class Variable
    {
        public String Tag;
        public int Type;
        public int Max;
        public int Min;
        public Variable() {}
        public Variable(String tag, int type)
        {
            Tag = tag;
            Type = type;
        }
        public Variable(String tag, int type, int min, int max)
        {
            Tag = tag;
            Type = type;
            Min = min;
            Max = max;
        }
    }
}
