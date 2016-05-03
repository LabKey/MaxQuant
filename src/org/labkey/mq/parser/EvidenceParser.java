package org.labkey.mq.parser;


import org.labkey.mq.model.Experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vsharma on 3/16/2016.
 */
public class EvidenceParser extends MaxQuantTsvParser
{
    public static final String FILE = "evidence.txt";

    private static final String ModifiedSequence = "Modified sequence";
    private static final String RawFile = "Raw file";
    private static final String Experiment = "Experiment";
    private static final String MsmsMz = "MS/MS m/z";
    private static final String Charge = "Charge";
    private static final String MassErrorPpm = "Mass Error [ppm]";
    private static final String UncalibratedMassErrorPpm = "Uncalibrated Mass Error [ppm]";
    private static final String RetentionTime = "Retention time";
    private static final String Pep = "PEP";
    private static final String MsmsCount = "MS/MS Count";
    private static final String MsmsScanNumber = "MS/MS Scan Number";
    private static final String Score = "Score";
    private static final String DeltaScore = "Delta score";
    private static final String Intensity = "Intensity";
    private static final String PeptideId = "Peptide ID";
    private static final String ModifiedPeptideId = "Mod. peptide ID";
    private static final String MsmsIds = "MS/MS IDs";
    private static final String BestMsmsId = "Best MS/MS";

    public EvidenceParser(File file) throws MqParserException
    {
        super(file);
    }

    public EvidenceRow nextEvidence()
    {
        TsvRow row = super.nextRow();
        if(row == null)
        {
            return null;
        }
        EvidenceRow evidenceRow = new EvidenceRow();
        evidenceRow.setModifiedSequence(getValue(row, ModifiedSequence));
        evidenceRow.setRawFile(getValue(row, RawFile));
        evidenceRow.setExperiment(getValue(row, Experiment));
        evidenceRow.setMsmsMz(tryGetDoubleValue(row, MsmsMz)); // Found missing MS/MS m/z in LFQ data
        evidenceRow.setCharge(getIntValue(row, Charge));
        evidenceRow.setMassErrorPpm(getDoubleValue(row, MassErrorPpm));
        evidenceRow.setUncalibratedMassErrorPpm(getDoubleValue(row, UncalibratedMassErrorPpm));
        evidenceRow.setRetentionTime(getDoubleValue(row, RetentionTime));
        evidenceRow.setPep(tryGetDoubleValue(row, Pep)); // Found missing values in LFQ data
        evidenceRow.setMsmsCount(getIntValue(row, MsmsCount));
        evidenceRow.setScanNumber(tryGetIntValue(row, MsmsScanNumber)); // Found missing values in LFQ data
        evidenceRow.setScore(tryGetDoubleValue(row, Score)); // Found NaN values in LFQ data
        evidenceRow.setDeltaScore(tryGetDoubleValue(row, DeltaScore)); // Found NaN values in LFQ data
        evidenceRow.setIntensity(tryGetLongValue(row, Intensity));
        evidenceRow.setMaxQuantPeptideId(getIntValue(row, PeptideId));
        evidenceRow.setMaxQuantModifiedPeptideId(getIntValue(row, ModifiedPeptideId));
        evidenceRow.setMsmsIds(tryGetValue(row, MsmsIds));
        evidenceRow.setBestMsMsId(tryGetIntValue(row, BestMsmsId)); // Found missing values in LFQ data
        evidenceRow.setMaxQuantId(getIntValue(row, MaxQuantId));

        for(String ratioType: Constants.RatioTypes)
        {
            String ratioHeader = Constants.Ratio + " " + ratioType;
            String ratioNormHeader = Constants.Ratio + " " + ratioType + " normalized";

            SilacRatio ratio = new SilacRatio();
            ratio.setRatioType(ratioType);
            ratio.setRatio(tryGetDoubleValue(row, ratioHeader));
            ratio.setRatioNormalized(tryGetDoubleValue(row, ratioNormHeader));
            if(ratio.hasRatioVals())
            {
                evidenceRow.addSilacRatio(ratio);
            }
        }

        for(String labeltype: Constants.LabelTypes)
        {
            String ratioHeader = Intensity + " " + labeltype;
            Long intensity = tryGetLongValue(row, ratioHeader);
            if(intensity != null)
            {
                evidenceRow.addSilacIntensity(labeltype, intensity);
            }
        }
        return evidenceRow;
    }

    public static final class EvidenceRow extends MaxQuantTsvRow
    {
        private String _modifiedSequence; // We will use this value to update ModifiedPeptide.Sequence
        private String _rawFile;
        private String _experiment;
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
        private int _maxQuantPeptideId;
        private int _maxQuantModifiedPeptideId;
        private String _msmsIds;
        private Integer _bestMsMsId;

        private Map<String, Long> _silacIntensities = new HashMap<String, Long>();
        private List<SilacRatio> _silacRatios = new ArrayList<>();

        // Ratios for SILAC experiment
        private Map<Experiment, SilacRatio> _experimentRatios = new HashMap<>();

        public String getModifiedSequence()
        {
            return _modifiedSequence;
        }

        public void setModifiedSequence(String modifiedSequence)
        {
            _modifiedSequence = modifiedSequence;
        }

        public String getRawFile()
        {
            return _rawFile;
        }

        public void setRawFile(String rawFile)
        {
            _rawFile = rawFile;
        }

        public String getExperiment()
        {
            return _experiment;
        }

        public void setExperiment(String experiment)
        {
            _experiment = experiment;
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

        public int getMaxQuantPeptideId()
        {
            return _maxQuantPeptideId;
        }

        public void setMaxQuantPeptideId(int maxQuantPeptideId)
        {
            _maxQuantPeptideId = maxQuantPeptideId;
        }

        public int getMaxQuantModifiedPeptideId()
        {
            return _maxQuantModifiedPeptideId;
        }

        public void setMaxQuantModifiedPeptideId(int maxQuantModifiedPeptideId)
        {
            _maxQuantModifiedPeptideId = maxQuantModifiedPeptideId;
        }

        public String getMsmsIds()
        {
            return _msmsIds;
        }

        public void setMsmsIds(String msmsIds)
        {
            _msmsIds = msmsIds;
        }

        public Integer getBestMsMsId()
        {
            return _bestMsMsId;
        }

        public void setBestMsMsId(Integer bestMsMsId)
        {
            _bestMsMsId = bestMsMsId;
        }

        public void addSilacRatio(SilacRatio ratio)
        {
            _silacRatios.add(ratio);
        }

        public List<SilacRatio> getSilacRatios()
        {
            return Collections.unmodifiableList(_silacRatios);
        }

        public void addSilacIntensity(String label, Long intensity)
        {
            _silacIntensities.put(label, intensity);
        }

        public Map<String, Long> getSilacIntensities()
        {
            return Collections.unmodifiableMap(_silacIntensities);
        }
    }

    public static class SilacRatio
    {
        private String _ratioType;
        private Double _ratio;
        private Double _ratioNormalized;

        public String getRatioType()
        {
            return _ratioType;
        }

        public void setRatioType(String ratioType)
        {
            _ratioType = ratioType;
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

        public boolean hasRatioVals()
        {
            return _ratio != null || _ratioNormalized != null;
        }
    }
}

