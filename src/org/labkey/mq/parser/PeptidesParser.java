package org.labkey.mq.parser;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Created by vsharma on 3/16/2016.
 */
public class PeptidesParser extends MaxQuantTsvParser
{
    public static final String FILE = "peptides.txt";

    private static final String Sequence = "Sequence";
    private static final String Length = "Length";
    private static final String MissedCleavages = "Missed cleavages";
    private static final String Mass = "Mass";
    private static final String StartPosition = "Start position";
    private static final String EndPosition = "End position";
    //private static final String UniqueToGroups = "Unique (Groups)";
    //private static final String UniqueToProtein = "Unique (Proteins)";
    private static final String ProteinGroupIds = "Protein group IDs";

    public PeptidesParser(File file) throws MqParserException
    {
        super(file);
    }

    public PeptideRow nextPeptide()
    {
        TsvRow row = super.nextRow();
        if(row == null)
        {
            return null;
        }
        PeptideRow pepRow = new PeptideRow();
        pepRow.setSequence(getValue(row, Sequence));
        pepRow.setLength(getIntValue(row, Length));
        pepRow.setStartPosition(getIntValue(row, StartPosition, null));
        pepRow.setEndPosition(getIntValue(row, EndPosition, null));
        pepRow.setMissedCleavages(getIntValue(row, MissedCleavages));
        pepRow.setMass(getDoubleValue(row, Mass));
        pepRow.setMaxQuantId(getIntValue(row, MaxQuantId));
        pepRow.setMaxQuantProteinGroupIds(getValue(row, ProteinGroupIds));
        return pepRow;
    }

    public static final class PeptideRow extends MaxQuantTsvParser.MaxQuantTsvRow
    {
        private String _sequence;
        private int _length;
        private Integer _startPosition;
        private Integer _endPosition;
        private int _missedCleavages;
        private double _mass;
        private int[] _maxQuantProteinGroupIds;

        public String getSequence()
        {
            return _sequence;
        }

        public void setSequence(String sequence)
        {
            _sequence = sequence;
        }

        public int getLength()
        {
            return _length;
        }

        public void setLength(int length)
        {
            _length = length;
        }

        public Integer getStartPosition()
        {
            return _startPosition;
        }

        public void setStartPosition(Integer startPosition)
        {
            _startPosition = startPosition;
        }

        public Integer getEndPosition()
        {
            return _endPosition;
        }

        public void setEndPosition(Integer endPosition)
        {
            _endPosition = endPosition;
        }

        public int getMissedCleavages()
        {
            return _missedCleavages;
        }

        public void setMissedCleavages(int missedCleavages)
        {
            _missedCleavages = missedCleavages;
        }

        public double getMass()
        {
            return _mass;
        }

        public void setMass(double mass)
        {
            _mass = mass;
        }

        public int[] getMaxQuantProteinGroupIds()
        {
            return _maxQuantProteinGroupIds == null ? new int[0]: _maxQuantProteinGroupIds;
        }

        public void setMaxQuantProteinGroupIds(String idsString)
        {
            if (!StringUtils.isBlank(idsString))
            {
                String[] ids = StringUtils.split(idsString, ";");
                _maxQuantProteinGroupIds = new int[ids.length];
                int i = 0;
                for (String id : ids)
                {
                    _maxQuantProteinGroupIds[i++] = Integer.parseInt(id);
                }
            }
        }
    }
}

