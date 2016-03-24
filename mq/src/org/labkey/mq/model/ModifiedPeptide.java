package org.labkey.mq.model;


import org.labkey.mq.parser.ModifiedPeptidesParser;

/**
 * Created by vsharma on 3/16/2016.
 */
public class ModifiedPeptide extends MqEntity
{
    private int _id;
    private int _peptideId;

    private int _maxQuantId;
    private String _sequence;
    private String _modifications;
    private double _mass;

    public ModifiedPeptide() {}

    public ModifiedPeptide(ModifiedPeptidesParser.ModifiedPeptideRow row)
    {
        _maxQuantId = row.getMaxQuantId();
        _modifications = row.getModifications();
        _mass = row.getMass();
    }

    public int getId()
    {
        return _id;
    }

    public void setId(int id)
    {
        _id = id;
    }

    public int getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(int peptideId)
    {
        _peptideId = peptideId;
    }

    public String getModifications()
    {
        return _modifications;
    }

    public void setModifications(String modifications)
    {
        _modifications = modifications;
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

    public double getMass()
    {
        return _mass;
    }

    public void setMass(double mass)
    {
        _mass = mass;
    }
}
