package org.labkey.mq.model;

import org.labkey.mq.parser.PeptidesParser;

/**
 * Created by vsharma on 3/16/2016.
 */
public class Peptide extends MqEntity
{
    private int _id;
    private int _experimentGroupId;

    private int _maxQuantId;
    private String _sequence;
    private int _length;
    private Integer _startPosition;
    private Integer _endPosition;
    private int _missedCleavages;
    private double _mass;

    public Peptide() {}

    public Peptide(PeptidesParser.PeptideRow row)
    {
        _maxQuantId = row.getMaxQuantId();
        _sequence = row.getSequence();
        _length = row.getLength();
        _startPosition = row.getStartPosition();
        _endPosition = row.getEndPosition();
        _missedCleavages = row.getMissedCleavages();
        _mass = row.getMass();
    }

    @Override
    public int getId()
    {
        return _id;
    }

    @Override
    public void setId(int id)
    {
        _id = id;
    }

    public int getExperimentGroupId()
    {
        return _experimentGroupId;
    }

    public void setExperimentGroupId(int experimentGroupId)
    {
        _experimentGroupId = experimentGroupId;
    }

    public int getMaxQuantId()
    {
        return _maxQuantId;
    }

    public void setMaxQuantId(int maxQuantId)
    {
        _maxQuantId = maxQuantId;
    }

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
}
