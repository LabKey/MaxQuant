package org.labkey.mq.model;

/**
 * Created by vsharma on 3/3/2016.
 */
public class ProteinGroupIntensity extends MqEntity
{
    private int _proteinGroupId;
    private int _experimentId;
    private long _intensity;
    private String _labelType;

    public ProteinGroupIntensity() {}

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
}
