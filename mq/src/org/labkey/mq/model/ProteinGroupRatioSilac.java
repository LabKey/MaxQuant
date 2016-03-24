package org.labkey.mq.model;

/**
 * Created by vsharma on 3/3/2016.
 */
public class ProteinGroupRatioSilac extends MqEntity
{
    private int _proteinGroupId;
    private int _experimentId;
    private int _intensity;
    private String _ratioType;
    private double _ratio;
    private double _ratioNormalized;
    private int _ratioCount;

    public ProteinGroupRatioSilac() {}

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

    public int getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(int intensity)
    {
        _intensity = intensity;
    }

    public String getRatioType()
    {
        return _ratioType;
    }

    public void setRatioType(String ratioType)
    {
        _ratioType = ratioType;
    }

    public double getRatio()
    {
        return _ratio;
    }

    public void setRatio(double ratio)
    {
        _ratio = ratio;
    }

    public double getRatioNormalized()
    {
        return _ratioNormalized;
    }

    public void setRatioNormalized(double ratioNormalized)
    {
        _ratioNormalized = ratioNormalized;
    }

    public int getRatioCount()
    {
        return _ratioCount;
    }

    public void setRatioCount(int ratioCount)
    {
        _ratioCount = ratioCount;
    }
}
