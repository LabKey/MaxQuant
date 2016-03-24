package org.labkey.mq.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vsharma on 3/2/2016.
 */
public class TsvParser
{
    private Map<Integer, String> _colHeaderMap;
    private BufferedReader _reader;
    private int _lineNum;
    private String _fileName;

    public TsvParser(File file) throws MqParserException
    {
        if(file == null)
        {
            throw new MqParserException("file is null.");
        }
        if(!file.exists())
        {
            throw new MqParserException("File does not exist: " + file.getPath());
        }

        try
        {
            _fileName = file.getName();
            _reader = new BufferedReader(new FileReader(file));
            parseHeader(_reader.readLine());
        }
        catch(IOException e)
        {
            throw new MqParserException("Error reading file " + file.getParent(), e);
        }
    }

    String getFileName()
    {
        return _fileName;
    }

    private void parseHeader(String line)
    {
        if(line == null)
        {
            throw new MqParserException("No header line found in file");
        }
        String[] parts = getParts(line);
        _colHeaderMap = new HashMap<>();
        int index = 0;
        for(String part: parts)
        {
            _colHeaderMap.put(index++, part);
        }
        _lineNum++;
    }

    public TsvRow nextRow()
    {
        try
        {
            String line = _reader.readLine();
            if(line == null)
            {
                close();
                return null;
            }
            return TsvRow.makeRow(_colHeaderMap, line, _lineNum++);
        }
        catch(IOException e)
        {
            close();
            throw new MqParserException("Error reading file", e);
        }
    }

    public void close()
    {
        if(_reader != null)
        {
            try {_reader.close();} catch (IOException ignored){}
        }
    }

    public static final class TsvRow
    {
        private Map<String, String> _values;
        private final int _rowNum;

        public TsvRow(int rowNum)
        {
            _values = new HashMap<>();
            _rowNum = rowNum;
        }

        public static final TsvRow makeRow(Map<Integer, String> colHeader, String line, int lineNum)
        {
            String[] vals = TsvParser.getParts(line);

            // TODO: Uncomment this? Does this happen only if the columns at the end are blank?
//            if(vals.length != colHeader.size())
//            {
//                throw new MqParserException("Expected " + colHeader.size() + " values in row, found " + vals.length + " at line number " + lineNum);
//            }

            TsvRow row = new TsvRow(lineNum);
            for(int i = 0; i < vals.length; i++)
            {
                String val = vals[i];
                String header = colHeader.get(i);
                if(header == null)
                {
                    throw new MqParserException("Could not find a header at column index " + i);
                }

                if(row._values.get(header) != null)
                {
                    throw new MqParserException("Duplicate column header " + header + " at index " + i);
                }

                row._values.put(header, val);
            }
            return row;
        }

        public String getValue(String column)
        {
            return _values.get(column);
        }

        public int getRowNum()
        {
            return _rowNum;
        }
    }

    private static String[] getParts(String line) throws MqParserException
    {
        return line.split("\\t");
    }
}
