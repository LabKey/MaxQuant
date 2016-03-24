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

    // SILAC file columns
    private static final String H = "H";
    private static final String M = "M";
    private static final String L = "L";
    private static final String[] Labeltypes = new String[] {H, M, L};
    private static final String HL = "H/L";
    private static final String HM = "H/M";
    private static final String ML = "M/L";
    private static final String[] RatioTypes = new String[] {HL, HM, ML};
    private static final String Ratio = "Ratio";

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
        evidenceRow.setMsmsMz(getDoubleValue(row, MsmsMz));
        evidenceRow.setCharge(getIntValue(row, Charge));
        evidenceRow.setMassErrorPpm(getDoubleValue(row, MassErrorPpm));
        evidenceRow.setUncalibratedMassErrorPpm(getDoubleValue(row, UncalibratedMassErrorPpm));
        evidenceRow.setRetentionTime(getDoubleValue(row, RetentionTime));
        evidenceRow.setPep(getDoubleValue(row, Pep));
        evidenceRow.setMsmsCount(getIntValue(row, MsmsCount));
        evidenceRow.setScanNumber(getIntValue(row, MsmsScanNumber));
        evidenceRow.setScore(getDoubleValue(row, Score));
        evidenceRow.setDeltaScore(getDoubleValue(row, DeltaScore));
        evidenceRow.setIntensity(getIntValue(row, Intensity, null));
        evidenceRow.setMaxQuantPeptideId(getIntValue(row, PeptideId));
        evidenceRow.setMaxQuantModifiedPeptideId(getIntValue(row, ModifiedPeptideId));
        evidenceRow.setMsmsIds(getValue(row, MsmsIds));
        evidenceRow.setBestMsMsId(getIntValue(row, BestMsmsId));
        evidenceRow.setMaxQuantId(getIntValue(row, MaxQuantId));

        for(String ratioType: RatioTypes)
        {
            String ratioHeader = Ratio + " " + ratioType;
            String ratioNormHeader = Ratio + " " + ratioType + " normalized";

            SilacRatio ratio = new SilacRatio();
            ratio.setRatioType(ratioType);
            ratio.setRatio(tryGetDoubleValue(row, ratioHeader));
            ratio.setRatioNormalized(tryGetDoubleValue(row, ratioNormHeader));
            if(ratio.hasRatioVals())
            {
                evidenceRow.addSilacRatio(ratio);
            }
        }

        for(String labeltype: Labeltypes)
        {
            String ratioHeader = Intensity + " " + labeltype;
            Integer intensity = tryGetIntValue(row, ratioHeader);
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
        private double _msmsMz;
        private int _charge;
        private Double _massErrorPpm;
        private Double _uncalibratedMassErrorPpm;
        private double _retentionTime;
        private double _pep;
        private int _msmsCount;
        private int _scanNumber;
        private double _score;
        private double _deltaScore;
        private Integer _intensity;
        private int _maxQuantPeptideId;
        private int _maxQuantModifiedPeptideId;
        private String _msmsIds;
        private int _bestMsMsId;

        private Map<String, Integer> _silacIntensities = new HashMap<String, Integer>();
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

        public int getBestMsMsId()
        {
            return _bestMsMsId;
        }

        public void setBestMsMsId(int bestMsMsId)
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

        public void addSilacIntensity(String label, Integer intensity)
        {
            _silacIntensities.put(label, intensity);
        }

        public Map<String, Integer> getSilacIntensities()
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

