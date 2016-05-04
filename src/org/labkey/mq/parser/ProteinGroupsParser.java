package org.labkey.mq.parser;

import org.labkey.mq.model.Experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vsharma on 3/2/2016.
 */
public class ProteinGroupsParser extends MaxQuantTsvParser
{
    public static final String FILE = "proteinGroups.txt";

    private static final String ProteinIds = "Protein IDs";
    private static final String MajorityProteinIds = "Majority protein IDs";
    private static final String ProteinNames = "Protein names";
    private static final String GeneNames = "Gene names";
    private static final String FastaHeaders = "Fasta headers";
    private static final String NumProteins = "Number of proteins";
    private static final String NumPeptides = "Peptides";
    private static final String RazorUniqPeptides = "Razor + unique peptides";
    private static final String UniquePeptides = "Unique peptides";
    private static final String SequenceCoverage = "Sequence coverage [%]";
    private static final String Score = "Score";
    private static final String Intensity = "Intensity";
    private static final String MsMsCount = "MS/MS Count"; // Not in SILAC file
    private static final String OnlyIdentifiedBySite = "Only identified by site";
    private static final String Reverse = "Reverse";
    private static final String PotentialContaminant = "Potential contaminant";
    // private static final String MaxQuantPeptideIds = "Peptide IDs";

    public ProteinGroupsParser(File file) throws MqParserException
    {
        super(file);
    }

    public ProteinGroupRow nextProteinGroup(List<Experiment> experiments)
    {
        TsvRow row = super.nextRow();
        if(row == null)
        {
            return null;
        }
        ProteinGroupRow pgRow = new ProteinGroupRow();
        pgRow.setProteinIds(getValue(row, ProteinIds));
        pgRow.setMajorityProteinIds(getValue(row, MajorityProteinIds));
        pgRow.setProteinNames(getValue(row, ProteinNames));
        pgRow.setGeneNames(getValue(row, GeneNames));
        pgRow.setFastaHeaders(getValue(row, FastaHeaders));
        pgRow.setProteinCount(getIntValue(row, NumProteins));
        pgRow.setPeptideCount(getIntValue(row, NumPeptides));
        pgRow.setRazorUniqPeptidecount(getIntValue(row, RazorUniqPeptides));
        pgRow.setUniqPeptideCount(getIntValue(row, UniquePeptides));
        pgRow.setSequenceCoverage(getDoubleValue(row, SequenceCoverage));
        pgRow.setScore(getDoubleValue(row, Score));
        pgRow.setIntensity(getLongValue(row, Intensity));
        pgRow.setMs2Count(tryGetIntValue(row, MsMsCount)); // MS/MS Count column is missing in the SILAC file.
        pgRow.setIdentifiedBySite(getBooleanValue(row, OnlyIdentifiedBySite));
        pgRow.setReverse(getBooleanValue(row, Reverse));
        pgRow.setContaminant(getBooleanValue(row, PotentialContaminant));
        pgRow.setMaxQuantId(getIntValue(row, MaxQuantId));

        for(Experiment experiment: experiments)
        {
            String expName = experiment.getExperimentName();
            // Parse Sequence coverage and intensity columns (and LFQ intensity column in LFQ data)
            Double coverage = getDoubleValue(row, "Sequence coverage " + expName + " [%]");
            Long intensity = getLongValue(row, Intensity + " " + expName);
            Long lqfIntensity = tryGetLongValue(row, "LFQ intensity " + expName);

            ExperimentInfo expInfo = new ExperimentInfo(intensity, coverage);
            expInfo.setLfqIntensity(lqfIntensity);
            pgRow.addExperimentInfo(experiment, expInfo);

            // Parse the Ratio columns - only in SILAC file
            for(String ratioType: Constants.RatioTypes)
            {
                String prefix = Constants.Ratio + " " + ratioType + " ";
                String ratioHeader = prefix + expName;
                String ratioNormHeader = prefix + "normalized " + expName;
                String ratioCountHeader = prefix + "count " + expName;

                SilacRatio ratios = new SilacRatio();
                ratios.setRatioType(ratioType);
                ratios.setRatio(tryGetDoubleValue(row, ratioHeader));
                ratios.setRatioNormalized(tryGetDoubleValue(row, ratioNormHeader));
                ratios.setRatioCount(tryGetIntValue(row, ratioCountHeader));
                if(ratios.hasRatioVals())
                {
                    pgRow.addExperimentRatios(experiment, ratios);
                }
            }

            // Parse (H/M/L) intensity columns - only in SILAC files
            for(String label: Constants.LabelTypes)
            {
                String intensitySilacHeader = Intensity + " "  + label + " " + expName;

                SilacIntensity sIntensity = new SilacIntensity();
                sIntensity.setLabel(label);
                sIntensity.setIntensity(tryGetLongValue(row, intensitySilacHeader));

                pgRow.addSilacExperimentIntensity(experiment, sIntensity);
            }

        }
        return pgRow;
    }

    public static final class ProteinGroupRow extends MaxQuantTsvParser.MaxQuantTsvRow
    {
        private String _proteinIds;
        private String _majorityProteinIds;
        private String _proteinNames;
        private String _geneNames;
        private String _fastaHeaders;
        private int _proteinCount;
        private int _peptideCount;
        private int _uniqPeptideCount;
        private int _razorUniqPeptidecount;
        private double _sequenceCoverage;
        private double _score;
        private long _intensity;
        private Integer _ms2Count;
        private boolean _identifiedBySite;
        private boolean _reverse;
        private boolean _contaminant;

        private Map<Experiment, ExperimentInfo> _experimentIntensity = new HashMap<Experiment, ExperimentInfo>();

        // Ratios for SILAC experiment
        private Map<Experiment, List<SilacRatio>> _experimentRatios = new HashMap<>();

        // Intensities for SILAC experiment
        private Map<Experiment, List<SilacIntensity>> _silacExperimentIntensities = new HashMap<>();

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

        public int getRazorUniqPeptidecount()
        {
            return _razorUniqPeptidecount;
        }

        public void setRazorUniqPeptidecount(int razorUniqPeptidecount)
        {
            _razorUniqPeptidecount = razorUniqPeptidecount;
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

        public void addExperimentInfo(Experiment experiment, ExperimentInfo info)
        {
            _experimentIntensity.put(experiment, info);
        }

        public Map<Experiment, ExperimentInfo> getExperimentInfos()
        {
            return Collections.unmodifiableMap(_experimentIntensity);
        }

        public void addExperimentRatios(Experiment experiment, SilacRatio ratios)
        {
            List<SilacRatio> sRatios = _experimentRatios.get(experiment);
            if(sRatios == null)
            {
                sRatios = new ArrayList<>();
                _experimentRatios.put(experiment, sRatios);
            }
            sRatios.add(ratios);
        }

        public Map<Experiment, List<SilacRatio>> getExperimentRatios()
        {
            return Collections.unmodifiableMap(_experimentRatios);
        }

        public void addSilacExperimentIntensity(Experiment experiment, SilacIntensity intensity)
        {
            List<SilacIntensity> sIntensities = _silacExperimentIntensities.get(experiment);
            if(sIntensities == null)
            {
                sIntensities = new ArrayList<>();
                _silacExperimentIntensities.put(experiment, sIntensities);
            }
            sIntensities.add(intensity);
        }

        public Map<Experiment, List<SilacIntensity>> getSilacExperimentIntensities()
        {
            return Collections.unmodifiableMap(_silacExperimentIntensities);
        }
    }

    public static final class ExperimentInfo
    {
        private final Long _intensity;
        private final Double _coverage;
        private Long _lfqIntensity;

        private ExperimentInfo(Long intensity, Double coverage)
        {
            _intensity =  intensity;
            _coverage = coverage;
        }

        public Long getIntensity()
        {
            return _intensity;
        }

        public Double getCoverage()
        {
            return _coverage;
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

    public static final class SilacRatio extends EvidenceParser.SilacRatio
    {
        private Integer _ratioCount;

        public Integer getRatioCount()
        {
            return _ratioCount;
        }

        public void setRatioCount(Integer ratioCount)
        {
            _ratioCount = ratioCount;
        }

        public boolean hasRatioVals()
        {
            return super.hasRatioVals() || _ratioCount != null;
        }
    }

    public static final class SilacIntensity
    {
        private String _label;
        private Long _intensity;

        public String getLabel()
        {
            return _label;
        }

        public void setLabel(String label)
        {
            _label = label;
        }

        public Long getIntensity()
        {
            return _intensity;
        }

        public void setIntensity(Long intensity)
        {
            _intensity = intensity;
        }
    }
}
