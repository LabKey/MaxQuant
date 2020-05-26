package org.labkey.mq.model;


import org.labkey.mq.parser.EvidenceParser;

/**
 * Created by vsharma on 3/16/2016.
 */
public class Evidence extends MqEntity
{
    private int _id;
    private int _peptideId;
    private Integer _modifiedPeptideId;
    private int _experimentId;
    private int _rawFileId;

    private Double _msmsMz;
    private int _charge;
    private Double _massErrorPpm;
    private Double _uncalibratedMassErrorPpm;
    private double _retentionTime;
    private Double _pep;
    private int _msmsCount;
    private Integer _scanNumber;
    private Double _score;
    private Double _deltaScore;
    private Long _intensity;
    private String _maxQuantMsmsIds;
    private Integer _maxQuantBestMsmsId;

    private int _maxQuantId;

    public Evidence() {}

    public Evidence(EvidenceParser.EvidenceRow row)
    {
        _maxQuantId = row.getMaxQuantId();
        _msmsMz = row.getMsmsMz();
        _charge = row.getCharge();
        _massErrorPpm = row.getMassErrorPpm();
        _uncalibratedMassErrorPpm = row.getUncalibratedMassErrorPpm();
        _retentionTime = row.getRetentionTime();
        _pep = row.getPep();
        _msmsCount = row.getMsmsCount();
        _scanNumber = row.getScanNumber();
        _score = row.getScore();
        _deltaScore = row.getDeltaScore();
        _intensity = row.getIntensity();
        _maxQuantMsmsIds = row.getMsmsIds();
        _maxQuantBestMsmsId = row.getBestMsMsId();
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

    public int getPeptideId()
    {
        return _peptideId;
    }

    public void setPeptideId(int peptideId)
    {
        _peptideId = peptideId;
    }

    public Integer getModifiedPeptideId()
    {
        return _modifiedPeptideId;
    }

    public void setModifiedPeptideId(Integer modifiedPeptideId)
    {
        _modifiedPeptideId = modifiedPeptideId;
    }

    public int getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(int experimentId)
    {
        _experimentId = experimentId;
    }

    public int getRawFileId()
    {
        return _rawFileId;
    }

    public void setRawFileId(int rawFileId)
    {
        _rawFileId = rawFileId;
    }

    public Double getMsmsMz()
    {
        return _msmsMz;
    }

    public void setMsmsMz(Double msmsMz)
    {
        _msmsMz = msmsMz;
    }

    public int getCharge()
    {
        return _charge;
    }

    public void setCharge(int charge)
    {
        _charge = charge;
    }

    public Double getMassErrorPpm()
    {
        return _massErrorPpm;
    }

    public void setMassErrorPpm(Double massErrorPpm)
    {
        _massErrorPpm = massErrorPpm;
    }

    public Double getUncalibratedMassErrorPpm()
    {
        return _uncalibratedMassErrorPpm;
    }

    public void setUncalibratedMassErrorPpm(Double uncalibratedMassErrorPpm)
    {
        _uncalibratedMassErrorPpm = uncalibratedMassErrorPpm;
    }

    public double getRetentionTime()
    {
        return _retentionTime;
    }

    public void setRetentionTime(double retentionTime)
    {
        _retentionTime = retentionTime;
    }

    public Double getPep()
    {
        return _pep;
    }

    public void setPep(Double pep)
    {
        _pep = pep;
    }

    public int getMsmsCount()
    {
        return _msmsCount;
    }

    public void setMsmsCount(int msmsCount)
    {
        _msmsCount = msmsCount;
    }

    public Integer getScanNumber()
    {
        return _scanNumber;
    }

    public void setScanNumber(Integer scanNumber)
    {
        _scanNumber = scanNumber;
    }

    public Double getScore()
    {
        return _score;
    }

    public void setScore(Double score)
    {
        _score = score;
    }

    public Double getDeltaScore()
    {
        return _deltaScore;
    }

    public void setDeltaScore(Double deltaScore)
    {
        _deltaScore = deltaScore;
    }

    public Long getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Long intensity)
    {
        _intensity = intensity;
    }

    public String getMaxQuantMsmsIds()
    {
        return _maxQuantMsmsIds;
    }

    public void setMaxQuantMsmsIds(String maxQuantMsmsIds)
    {
        _maxQuantMsmsIds = maxQuantMsmsIds;
    }

    public Integer getMaxQuantBestMsmsId()
    {
        return _maxQuantBestMsmsId;
    }

    public void setMaxQuantBestMsmsId(Integer maxQuantBestMsmsId)
    {
        _maxQuantBestMsmsId = maxQuantBestMsmsId;
    }

    public int getMaxQuantId()
    {
        return _maxQuantId;
    }

    public void setMaxQuantId(int maxQuantId)
    {
        _maxQuantId = maxQuantId;
    }
}
