package org.labkey.mq.model;

/**
 * Created by vsharma on 3/3/2016.
 */
public class ProteinGroupExperimentInfo extends MqEntity
{
    private int _proteinGroupId;
    private int _experimentId;
    private long _intensity;
    private Long _lfqIntensity; // lfq = Label Free Quantification
    private String _labelType;
    private double _coverage;

    public ProteinGroupExperimentInfo() {}

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

    public long getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(long intensity)
    {
        _intensity = intensity;
    }

    public String getLabelType()
    {
        return _labelType;
    }

    public void setLabelType(String labelType)
    {
        _labelType = labelType;
    }

    public double getCoverage()
    {
        return _coverage;
    }

    public void setCoverage(double coverage)
    {
        _coverage = coverage;
    }

    public Long getLfqIntensity()
    {
        return _lfqIntensity;
    }

    public void setLfqIntensity(Long lfqIntensity)
    {
        _lfqIntensity = lfqIntensity;
    }
}
