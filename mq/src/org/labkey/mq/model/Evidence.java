package org.labkey.mq.model;


import org.labkey.mq.parser.EvidenceParser;

/**
 * Created by vsharma on 3/16/2016.
 */
public class Evidence extends MqEntity
{
    private int _id;
    private int _peptideId;
    private int _modifiedPeptideId;
    private int _experimentId;
    private int _rawFileId;

    private double _msmsMz;
    private int _charge;
    private double _massErrorPpm;
    private double _uncalibratedMassErrorPpm;
    private double _retentionTime;
    private double _pep;
    private int _msmsCount;
    private int _scanNumber;
    private double _score;
    private double _deltaScore;
    private Integer _intensity;
    private String _maxQuantMsmsIds;
    private int _maxQuantBestMsmsId;

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

    public int getModifiedPeptideId()
    {
        return _modifiedPeptideId;
    }

    public void setModifiedPeptideId(int modifiedPeptideId)
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

    public double getMsmsMz()
    {
        return _msmsMz;
    }

    public void setMsmsMz(double msmsMz)
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

    public double getMassErrorPpm()
    {
        return _massErrorPpm;
    }

    public void setMassErrorPpm(double massErrorPpm)
    {
        _massErrorPpm = massErrorPpm;
    }

    public double getUncalibratedMassErrorPpm()
    {
        return _uncalibratedMassErrorPpm;
    }

    public void setUncalibratedMassErrorPpm(double uncalibratedMassErrorPpm)
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

    public double getPep()
    {
        return _pep;
    }

    public void setPep(double pep)
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

    public int getScanNumber()
    {
        return _scanNumber;
    }

    public void setScanNumber(int scanNumber)
    {
        _scanNumber = scanNumber;
    }

    public double getScore()
    {
        return _score;
    }

    public void setScore(double score)
    {
        _score = score;
    }

    public double getDeltaScore()
    {
        return _deltaScore;
    }

    public void setDeltaScore(double deltaScore)
    {
        _deltaScore = deltaScore;
    }

    public Integer getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(Integer intensity)
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

    public int getMaxQuantBestMsmsId()
    {
        return _maxQuantBestMsmsId;
    }

    public void setMaxQuantBestMsmsId(int maxQuantBestMsmsId)
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
