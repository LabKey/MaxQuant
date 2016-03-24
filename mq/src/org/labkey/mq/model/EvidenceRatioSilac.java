package org.labkey.mq.model;

/**
 * Created by vsharma on 3/3/2016.
 */
public class EvidenceRatioSilac extends MqEntity
{
    private int _evidenceId;
    private Double _ratio;
    private Double _ratioNormalized;
    private String _ratioType;

    public EvidenceRatioSilac() {}

    public int getEvidenceId()
    {
        return _evidenceId;
    }

    public void setEvidenceId(int evidenceId)
    {
        _evidenceId = evidenceId;
    }

    public Double getRatio()
    {
        return _ratio;
    }

    public void setRatio(Double ratio)
    {
        _ratio = ratio;
    }

    public Double getRatioNormalized()
    {
        return _ratioNormalized;
    }

    public void setRatioNormalized(Double ratioNormalized)
    {
        _ratioNormalized = ratioNormalized;
    }

    public String getRatioType()
    {
        return _ratioType;
    }

    public void setRatioType(String ratioType)
    {
        _ratioType = ratioType;
    }
}
