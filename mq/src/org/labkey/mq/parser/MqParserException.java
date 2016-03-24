package org.labkey.mq.parser;

/**
 * Created by vsharma on 2/3/2016.
 */
public class MqParserException extends RuntimeException
{
    public MqParserException(String message)
    {
        super(message);
    }

    public MqParserException(String fileName, TsvParser.TsvRow row, String message)
    {
        super(fileName + ":" + row.getRowNum() + ": " + message);
    }

    public MqParserException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MqParserException(String expected, String found)
    {
        super("Expected: " + expected+"; Found: " + found);
    }
}
