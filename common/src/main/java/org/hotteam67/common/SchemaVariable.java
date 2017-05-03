package org.hotteam67.common;

/**
 * Created by Jakob on 4/28/2017.
 */

public class SchemaVariable
{
    public String Tag;
    public int Type;
    public int Max;
    public int Min;
    public SchemaVariable() {}
    public SchemaVariable(String tag, int type)
    {
        Tag = tag;
        Type = type;
    }
    public SchemaVariable(String tag, int type, int min, int max)
    {
        Tag = tag;
        Type = type;
        Min = min;
        Max = max;
    }
}
