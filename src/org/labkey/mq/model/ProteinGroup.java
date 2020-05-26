package org.labkey.mq.model;

import org.labkey.mq.parser.ProteinGroupsParser;

/**
 * Created by vsharma on 3/2/2016.
 */
public class ProteinGroup extends MqEntity
{
    private int _id;
    private int _experimentGroupId;

    private String _proteinIds;
    private String _majorityProteinIds;
    private String _proteinNames;
    private String _geneNames;
    private String _fastaHeaders;
    private int _maxQuantId;
    private int _proteinCount;
    private int _peptideCount;
    private int _uniqPeptideCount;
    private int _razorUniqPeptideCount;
    private double _sequenceCoverage;
    private double _score;
    private long _intensity;
    private Integer _ms2Count;
    private boolean _identifiedBySite;
    private boolean _reverse;
    private boolean _contaminant;

    public ProteinGroup()
    {
    }

    public void copyFrom(ProteinGroupsParser.ProteinGroupRow row)
    {
        _maxQuantId = row.getMaxQuantId();
        _proteinIds = row.getProteinIds();
        _majorityProteinIds = row.getMajorityProteinIds();
        _proteinNames = row.getProteinNames();
        _geneNames = row.getGeneNames();
        _fastaHeaders = row.getFastaHeaders();
        _proteinCount = row.getProteinCount();
        _peptideCount = row.getPeptideCount();
        _uniqPeptideCount = row.getUniqPeptideCount();
        _razorUniqPeptideCount = row.getRazorUniqPeptidecount();
        _sequenceCoverage = row.getSequenceCoverage();
        _score = row.getScore();
        _intensity = row.getIntensity();
        _ms2Count = row.getMs2Count();
        _identifiedBySite = row.isIdentifiedBySite();
        _reverse = row.isReverse();
        _contaminant = row.isContaminant();
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

    public String getProteinIds()
    {
        return _proteinIds;
    }

    public void setProteinIds(String proteinIds)
    {
        _proteinIds = proteinIds;
    }

    public String getMajorityProteinIds()
    {
        return _majorityProteinIds;
    }

    public void setMajorityProteinIds(String majorityProteinIds)
    {
        _majorityProteinIds = majorityProteinIds;
    }

    public String getProteinNames()
    {
        return _proteinNames;
    }

    public void setProteinNames(String proteinNames)
    {
        _proteinNames = proteinNames;
    }

    public String getGeneNames()
    {
        return _geneNames;
    }

    public void setGeneNames(String geneNames)
    {
        _geneNames = geneNames;
    }

    public String getFastaHeaders()
    {
        return _fastaHeaders;
    }

    public void setFastaHeaders(String fastaHeaders)
    {
        _fastaHeaders = fastaHeaders;
    }

    public int getProteinCount()
    {
        return _proteinCount;
    }

    public void setProteinCount(int proteinCount)
    {
        _proteinCount = proteinCount;
    }

    public int getPeptideCount()
    {
        return _peptideCount;
    }

    public void setPeptideCount(int peptideCount)
    {
        _peptideCount = peptideCount;
    }

    public int getUniqPeptideCount()
    {
        return _uniqPeptideCount;
    }

    public void setUniqPeptideCount(int uniqPeptideCount)
    {
        _uniqPeptideCount = uniqPeptideCount;
    }

    public int getRazorUniqPeptideCount()
    {
        return _razorUniqPeptideCount;
    }

    public void setRazorUniqPeptideCount(int razorUniqPeptideCount)
    {
        _razorUniqPeptideCount = razorUniqPeptideCount;
    }

    public double getSequenceCoverage()
    {
        return _sequenceCoverage;
    }

    public void setSequenceCoverage(double sequenceCoverage)
    {
        _sequenceCoverage = sequenceCoverage;
    }

    public double getScore()
    {
        return _score;
    }

    public void setScore(double score)
    {
        _score = score;
    }

    public long getIntensity()
    {
        return _intensity;
    }

    public void setIntensity(long intensity)
    {
        _intensity = intensity;
    }

    public Integer getMs2Count()
    {
        return _ms2Count;
    }

    public void setMs2Count(Integer ms2Count)
    {
        _ms2Count = ms2Count;
    }

    public boolean isIdentifiedBySite()
    {
        return _identifiedBySite;
    }

    public void setIdentifiedBySite(boolean identifiedBySite)
    {
        _identifiedBySite = identifiedBySite;
    }

    public boolean isReverse()
    {
        return _reverse;
    }

    public void setReverse(boolean reverse)
    {
        _reverse = reverse;
    }

    public boolean isContaminant()
    {
        return _contaminant;
    }

    public void setContaminant(boolean contaminant)
    {
        _contaminant = contaminant;
    }
}
