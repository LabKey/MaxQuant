package org.labkey.mq.model;

/**
 * Created by vsharma on 3/3/2016.
 */
public class ProteinGroupSequenceCoverage extends MqEntity
{
    private int _proteinGroupId;
    private int _experimentId;
    private double _coverage;

    public int getProteinGroupId()
    {
        return _proteinGroupId;
    }

    public void setProteinGroupId(int proteinGroupId)
    {
        _proteinGroupId = proteinGroupId;
    }

    public int getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(int experimentId)
    {
        _experimentId = experimentId;
    }

    public double getCoverage()
    {
        return _coverage;
    }

    public void setCoverage(double coverage)
    {
        _coverage = coverage;
    }
}
