package org.labkey.mq.parser;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created by vsharma on 3/16/2016.
 */
public class MaxQuantTsvParser extends TsvParser
{
    protected static final String MaxQuantId = "id";

    public MaxQuantTsvParser(File file) throws MqParserException
    {
        super(file);
    }

    protected  boolean getBooleanValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        return val.equals("+");
    }

    protected int getIntValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse integer value in \"" + column + "\": " + val);
        }
    }

    protected Integer tryGetIntValue(TsvRow row, String column)
    {
        return getIntValue(row, column, null);
    }

    protected Integer getIntValue(TsvRow row, String column, Integer defaultVal)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return defaultVal;
        }

        int idx = val.indexOf(".");
        if (idx != -1)
        {
            val = val.substring(0, idx);
        }
        try
        {
            return Integer.valueOf(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse integer value in \"" + column + "\": " + val);
        }
    }

    protected long getLongValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        try
        {
            return Math.round(Double.parseDouble(val));
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row,  "Cannot parse long value in \"" + column + "\": " + val);
        }
    }

    protected Long tryGetLongValue(TsvRow row, String column)
    {
        return getLongValue(row, column, null);
    }

    protected Long getLongValue(TsvRow row, String column, Long defaultVal)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return defaultVal;
        }

        int idx = val.indexOf(".");
        if (idx != -1)
        {
            val = val.substring(0, idx);
        }

        try
        {
            return Long.parseLong(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row,  "Cannot parse long value in \"" + column + "\": " + val);
        }
    }

    protected double getDoubleValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        try
        {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse double value in \"" + column + "\": " + val);
        }
    }

    protected Double tryGetDoubleValue(TsvRow row, String column)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return null;
        }
        try
        {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse double value in \"" + column + "\": " + val);
        }
    }

    @NotNull
    protected String getValue(TsvRow row, String column)
    {
        String val = row.getValue(column);
        if (val == null)
        {
            throw new MqParserException(super.getFileName(), row, " Could not find value for \"" + column + "\"" );
        }
        return val;
    }

    protected String tryGetValue(TsvRow row, String column)
    {
        String val = row.getValue(column);
        return StringUtils.isBlank(val) ? null : val.trim();
    }


    public static class MaxQuantTsvRow
    {
        private int _maxQuantId;

        public int getMaxQuantId()
        {
            return _maxQuantId;
        }

        public void setMaxQuantId(int maxQuantId)
        {
            _maxQuantId = maxQuantId;
        }
    }
}
