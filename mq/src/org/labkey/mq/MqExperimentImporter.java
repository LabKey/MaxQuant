package org.labkey.mq;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.FileUtil;
import org.labkey.mq.model.Evidence;
import org.labkey.mq.model.EvidenceIntensitySilac;
import org.labkey.mq.model.EvidenceRatioSilac;
import org.labkey.mq.model.Experiment;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.ModifiedPeptide;
import org.labkey.mq.model.Peptide;
import org.labkey.mq.model.ProteinGroup;
import org.labkey.mq.model.ProteinGroupIntensity;
import org.labkey.mq.model.ProteinGroupRatioSilac;
import org.labkey.mq.model.ProteinGroupSequenceCoverage;
import org.labkey.mq.model.RawFile;
import org.labkey.mq.parser.EvidenceParser;
import org.labkey.mq.parser.ExperimentDesignTemplateParser;
import org.labkey.mq.parser.ModifiedPeptidesParser;
import org.labkey.mq.parser.MqParserException;
import org.labkey.mq.parser.PeptidesParser;
import org.labkey.mq.parser.ProteinGroupsParser;
import org.labkey.mq.query.ModifiedPeptideManager;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vsharma on 2/3/2016.
 */
public class MqExperimentImporter
{
    private User _user;
    private Container _container;
    private final ExpData _expData;
    private String _description;

    private int _experimentGroupId;

    // Use passed in logger for import status, information, and file format problems.  This should
    // end up in the pipeline log.
    protected Logger _log = null;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static Logger _systemLog = Logger.getLogger(MqExperimentImporter.class);
    protected final XarContext _context;

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;
    private static final String IMPORT_STARTED = "Importing... (refresh to check status)";
    private static final String IMPORT_FAILED = "Import failed (see pipeline log)";
    private static final String IMPORT_SUCCEEDED = "";

    public MqExperimentImporter(User user, Container c, String description, ExpData expData, Logger log, XarContext context)
    {
        _context = context;
        _user = user;
        _container = c;

        _expData = expData;

        if (null != description)
            _description = description;
        else
        {
            _description = FileUtil.getBaseName(_expData.getFile().getName());
        }

        _log = (null == log ? _systemLog : log);
    }

    public ExperimentGroup importExperiment(RunInfo runInfo) throws MqParserException
    {
        _experimentGroupId = runInfo.getRunId();

        ExperimentGroup run = MqManager.getExperimentGroup(_experimentGroupId);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == STATUS_SUCCESS)
        {
            _log.info(_expData.getFile() + " has already been imported so it does not need to be imported again");
            return run;
        }

        File experimentDesignTemplatefile = _expData.getFile();
        File experimentDirectory = experimentDesignTemplatefile.getParentFile();

        if(!experimentDirectory.exists())
        {
            throw new MqParserException("Experiment directory " + experimentDirectory + " does not exist.");
        }
        if(!experimentDirectory.isDirectory())
        {
            throw new MqParserException("Not a directory " + experimentDirectory + ".");
        }

        try
        {
            updateRunStatus(IMPORT_STARTED, STATUS_RUNNING);
            _log.info("Starting to import MaxQuant results from " + experimentDirectory);

            // Parse the "ExperimentDesignTemplate.txt" in the given directory
            if(!experimentDesignTemplatefile.exists())
            {
                throw new MqParserException("Could not find file " + experimentDesignTemplatefile.getName() + " in directory " + experimentDirectory + ".");
            }

            ExperimentGroup experimentGroup = new ExperimentDesignTemplateParser().parse(experimentDesignTemplatefile);
            try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
            {
                for(Experiment experiment: experimentGroup.getExperiments())
                {
                    experiment.setExperimentGroupId(_experimentGroupId);
                    experiment.setContainer(_container);
                    Table.insert(_user, MqManager.getTableInfoExperiment(), experiment);

                    for(RawFile rawFile: experiment.getRawfiles())
                    {
                        rawFile.setExperimentId(experiment.getId());
                        rawFile.setContainer(_container);
                        Table.insert(_user, MqManager.getTableInfoRawFile(), rawFile);
                    }
                }

                // TODO: Parse files in the "txt" subdirectory
                File txtDir = new File(experimentDirectory, "txt");
                if(!txtDir.exists())
                {
                    throw new MqParserException("Could not find 'txt' sub-directory in directory.");
                }

                // Parse proteinGroups.txt;
                Map<Integer, Integer> maxQuantProteinGroupIdToDbId = parseProteinGroups(experimentGroup, txtDir);

                // Parse peptides.txt;
                Map<Integer, Integer> maxQuantPeptideIdToDbId = parsePeptides(txtDir, maxQuantProteinGroupIdToDbId);

                // Parse modificationSpecificPeptides.txt
                Map<Integer, Integer> maxQuantModifiedPeptideIdToDbId = parseModifiedPeptides(txtDir, maxQuantPeptideIdToDbId);

                // parse evidence.txt
                parseEvidence(txtDir, experimentGroup, maxQuantPeptideIdToDbId, maxQuantModifiedPeptideIdToDbId);

                transaction.commit();
            }

            _log.info("Completed import of Skyline document from " + run.getFileName());

            updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);

            return MqManager.getExperimentGroup(_experimentGroupId);
        }
        catch (MqParserException mqe)
        {
            logError("MaxQuant import failed due to a missing file.", mqe);
            updateRunStatus(IMPORT_FAILED, STATUS_FAILED);
            throw mqe;
        }
        catch (RuntimeException e)
        {
            updateRunStatus(IMPORT_FAILED, STATUS_FAILED);
            throw e;
        }
        finally
        {
            close();
        }
    }

    private Map<Integer, Integer> parseProteinGroups(ExperimentGroup experimentGroup, File txtDir)
    {
        Map<Integer, Integer> maxQuantProteinGroupIdToDbId = new HashMap<>();

        File proteinGrpsFile = new File(txtDir, ProteinGroupsParser.FILE);
        if(!proteinGrpsFile.exists())
        {
            throw new MqParserException("Could not find proteinGroups.txt in " + txtDir.getPath());
        }
        ProteinGroupsParser pgParser = new ProteinGroupsParser(proteinGrpsFile);
        ProteinGroupsParser.ProteinGroupRow row;
        while((row = pgParser.nextProteinGroup(experimentGroup.getExperiments())) != null)
        {
            ProteinGroup pg = new ProteinGroup();
            pg.setExperimentGroupId(_experimentGroupId);
            pg.setContainer(_container);
            pg.copyFrom(row);

            Table.insert(_user, MqManager.getTableInfoProteinGroup(), pg);
            maxQuantProteinGroupIdToDbId.put(pg.getMaxQuantId(), pg.getId());

            Map<Experiment, Double> experimentCoverages = row.getExperimentCoverages();
            for(Map.Entry<Experiment, Double> entry: experimentCoverages.entrySet())
            {
                ProteinGroupSequenceCoverage coverage = new ProteinGroupSequenceCoverage();
                coverage.setContainer(_container);
                coverage.setExperimentId(entry.getKey().getId());
                coverage.setProteinGroupId(pg.getId());
                coverage.setCoverage(entry.getValue());

                Table.insert(_user, MqManager.getTableInfoProteinGroupSequenceCoverage(), coverage);
            }

            Map<Experiment, Long> experimentIntensities = row.getExperimentIntensities();
            for(Map.Entry<Experiment, Long> entry: experimentIntensities.entrySet())
            {
                ProteinGroupIntensity intensity = new ProteinGroupIntensity();
                intensity.setContainer(_container);
                intensity.setExperimentId(entry.getKey().getId());
                intensity.setProteinGroupId(pg.getId());
                intensity.setIntensity(entry.getValue());

                Table.insert(_user, MqManager.getTableInfoProteinGroupIntensity(), intensity);
            }

            Map<Experiment, ProteinGroupsParser.SilacRatio> ratios = row.getExperimentRatios();
            for(Map.Entry<Experiment, ProteinGroupsParser.SilacRatio> entry: ratios.entrySet())
            {
                ProteinGroupRatioSilac silacRatios = new ProteinGroupRatioSilac();
                silacRatios.setExperimentId(entry.getKey().getId());
                silacRatios.setContainer(_container);
                silacRatios.setProteinGroupId(pg.getId());
                ProteinGroupsParser.SilacRatio silacRatiosRow = entry.getValue();
                silacRatios.setRatioType(silacRatiosRow.getRatioType());
                silacRatios.setRatio(silacRatiosRow.getRatio());
                silacRatios.setRatioNormalized(silacRatiosRow.getRatioNormalized());
                silacRatios.setRatioCount(silacRatiosRow.getRatioCount());

                Table.insert(_user, MqManager.getTableInfoProteinGroupRatiosSilac(), silacRatios);
            }
        }

        return maxQuantProteinGroupIdToDbId;
    }

    private Map<Integer, Integer> parsePeptides(File txtDir, Map<Integer, Integer> maxQuantProteinGroupIdToDbId)
    {
        File peptidesFile = new File(txtDir, PeptidesParser.FILE);
        Map<Integer, Integer> maxQuantPeptideIdToDbId = new HashMap<>();

        if(!peptidesFile.exists())
        {
            throw new MqParserException("Could not find " + peptidesFile.getName() + " in " + txtDir.getPath());
        }
        PeptidesParser pepParser = new PeptidesParser(peptidesFile);
        PeptidesParser.PeptideRow row;
        while((row = pepParser.nextPeptide()) != null)
        {
            Peptide peptide = new Peptide(row);
            peptide.setExperimentGroupId(_experimentGroupId);
            peptide.setContainer(_container);

            Table.insert(_user, MqManager.getTableInfoPeptide(), peptide);

            maxQuantPeptideIdToDbId.put(peptide.getMaxQuantId(), peptide.getId());

            for(int mqProteinGroupId: row.getMaxQuantProteinGroupIds())
            {
                Integer proteinGroupId = maxQuantProteinGroupIdToDbId.get(mqProteinGroupId);
                if(proteinGroupId == null)
                {
                    throw new MqParserException("Could not find database ID form max quant protein group ID " + mqProteinGroupId);
                }
                Map<String, Integer> mapping = new HashMap<>();
                mapping.put("ProteinGroupId", proteinGroupId);
                mapping.put("PeptideId", peptide.getId());

                Table.insert(_user, MqManager.getTableInfoProteinGroupPeptide(), mapping);
            }
        }

        return maxQuantPeptideIdToDbId;
    }

    private Map<Integer, Integer> parseModifiedPeptides(File txtDir, Map<Integer, Integer> maxQuantPeptideIdToDbId)
    {
        File modifiedPeptidesFile = new File(txtDir, ModifiedPeptidesParser.FILE);
        Map<Integer, Integer> maxQuantModifiedPeptideIdToDbId = new HashMap<>();

        if(!modifiedPeptidesFile.exists())
        {
            throw new MqParserException("Could not find " + modifiedPeptidesFile.getName() + " in " + txtDir.getPath());
        }
        ModifiedPeptidesParser pepParser = new ModifiedPeptidesParser(modifiedPeptidesFile);
        ModifiedPeptidesParser.ModifiedPeptideRow row;
        while((row = pepParser.nextModifiedPeptide()) != null)
        {
            ModifiedPeptide modPeptide = new ModifiedPeptide(row);
            int peptideId = maxQuantPeptideIdToDbId.get(row.getMaxQuantPeptideId());
            modPeptide.setPeptideId(peptideId);
            modPeptide.setContainer(_container);

            Table.insert(_user, MqManager.getTableInfoModifiedPeptide(), modPeptide);

            maxQuantModifiedPeptideIdToDbId.put(modPeptide.getMaxQuantId(), modPeptide.getId());
        }

        return maxQuantModifiedPeptideIdToDbId;
    }

    private void parseEvidence(File txtDir, ExperimentGroup experimentGroup,
                               Map<Integer, Integer> maxQuantPeptideIdToDbId,
                               Map<Integer, Integer> maxQuantModifiedPeptideIdToDbId)
    {
        File evidenceFile = new File(txtDir, EvidenceParser.FILE);

        if(!evidenceFile.exists())
        {
            throw new MqParserException("Could not find " + evidenceFile.getName() + " in " + txtDir.getPath());
        }
        EvidenceParser pepParser = new EvidenceParser(evidenceFile);
        EvidenceParser.EvidenceRow row;

        Map<String, Integer> experimentNameToDbId = new HashMap<>();
        Map<String, Integer> rawfileNameToDbId = new HashMap<>();
        for(Experiment experiment: experimentGroup.getExperiments())
        {
            experimentNameToDbId.put(experiment.getExperimentName(), experiment.getId());
            for(RawFile file: experiment.getRawfiles())
            {
                rawfileNameToDbId.put(file.getName(), file.getId());
            }

        }
        while((row = pepParser.nextEvidence()) != null)
        {
            Evidence evidence = new Evidence(row);
            evidence.setContainer(_container);
            int peptideId = maxQuantPeptideIdToDbId.get(row.getMaxQuantPeptideId());
            evidence.setPeptideId(peptideId);
            int modifiedPeptideId = maxQuantModifiedPeptideIdToDbId.get(row.getMaxQuantModifiedPeptideId());
            evidence.setModifiedPeptideId(modifiedPeptideId);
            evidence.setExperimentId(experimentNameToDbId.get(row.getExperiment()));
            evidence.setRawFileId(rawfileNameToDbId.get(row.getRawFile()));
            evidence.setMaxQuantId(row.getMaxQuantId());
            evidence.setMsmsMz(row.getMsmsMz());
            evidence.setCharge(row.getCharge());
            evidence.setMassErrorPpm(row.getMassErrorPpm());
            evidence.setUncalibratedMassErrorPpm(row.getUncalibratedMassErrorPpm());
            evidence.setRetentionTime(row.getRetentionTime());
            evidence.setPep(row.getPep());
            evidence.setMsmsCount(row.getMsmsCount());
            evidence.setScanNumber(row.getScanNumber());
            evidence.setScore(row.getScore());
            evidence.setDeltaScore(row.getDeltaScore());
            evidence.setIntensity(row.getIntensity());
            evidence.setMaxQuantMsmsIds(row.getMsmsIds());
            evidence.setMaxQuantBestMsmsId(row.getBestMsMsId());

            Table.insert(_user, MqManager.getTableInfoEvidence(), evidence);

            // Update the ModifiedPeptide table with the modified sequence.
            // modificationSpecificPeptides.txt does not have modified sequences.
            ModifiedPeptide modPeptide = ModifiedPeptideManager.get(evidence.getModifiedPeptideId());
            if(!StringUtils.isBlank(row.getModifiedSequence()))
            {
                modPeptide.setSequence(row.getModifiedSequence());
                Table.update(_user, MqManager.getTableInfoModifiedPeptide(), modPeptide, modPeptide.getId());
            }

            for(EvidenceParser.SilacRatio ratio: row.getSilacRatios())
            {
                EvidenceRatioSilac erSilac = new EvidenceRatioSilac();
                erSilac.setContainer(_container);
                erSilac.setEvidenceId(evidence.getId());
                erSilac.setRatioType(ratio.getRatioType());
                erSilac.setRatio(ratio.getRatio());
                erSilac.setRatioNormalized(ratio.getRatioNormalized());
                Table.insert(_user, MqManager.getTableInfoEvidenceRatioSilac(), erSilac);
            }

            for(Map.Entry<String, Integer> entry: row.getSilacIntensities().entrySet())
            {
                EvidenceIntensitySilac eiSilac = new EvidenceIntensitySilac();
                eiSilac.setContainer(_container);
                eiSilac.setEvidenceId(evidence.getId());
                eiSilac.setLabelType(entry.getKey());
                eiSilac.setIntensity(entry.getValue());
                Table.insert(_user, MqManager.getTableInfoEvidenceIntensitySilac(), eiSilac);
            }
        }
    }

    private static final ReentrantLock _schemaLock = new ReentrantLock();

    protected RunInfo prepareExperimentGroup()
    {
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            boolean alreadyImported = false;

            // Don't import if we've already imported this file (undeleted run exists matching this file name)
            _experimentGroupId = getExperimentGroup();
            if (_experimentGroupId != -1)
            {
                alreadyImported = true;
            }
            else
            {
                _log.info("Starting import from " + _expData.getFile().getName());
                _experimentGroupId = createExperimentGroup();
            }

            transaction.commit();
            return new RunInfo(_experimentGroupId, alreadyImported);
        }
    }

    private int getExperimentGroup()
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("DataId"), _expData.getRowId());
        filter.addCondition(FieldKey.fromParts("Container"), _container.getId());
        filter.addCondition(FieldKey.fromParts("Deleted"), Boolean.FALSE);
        ExperimentGroup expGrp = new TableSelector(MqManager.getTableInfoExperimentGroup(), filter, null).getObject(ExperimentGroup.class);
        return expGrp != null ? expGrp.getId() : -1;
    }

    private int createExperimentGroup()
    {
        ExperimentGroup expGrp = MqManager.getExperimentGroupByDataId(_expData.getRowId(), _container);
        if (expGrp != null)
        {
            throw new IllegalStateException("Results have already been imported from " + _expData.getFile() + " in " + _container.getPath());
        }

        expGrp = new ExperimentGroup(_expData.getFile().getParent());
        expGrp.setContainer(_container);
        expGrp.setDataId(_expData.getRowId());
        expGrp.setStatus(IMPORT_STARTED);
        expGrp.setFileName(_expData.getFile().getName());
        expGrp.setDescription("MaxQuant experiment");
        expGrp = Table.insert(_user, MqManager.getTableInfoExperimentGroup(), expGrp);
        return expGrp.getId();
    }

    private void updateRunStatus(String status, int statusId)
    {
        updateRunStatus(_experimentGroupId, status, statusId);
    }

    private static void updateRunStatus(int runId, String status, int statusId)
    {
        new SqlExecutor(MqSchema.getSchema()).execute("UPDATE " +
                        MqManager.getTableInfoExperimentGroup() + " SET Status = ?, StatusId = ? WHERE Id = ?",
                status, statusId, runId);
    }

    private void logError(String message, Exception e)
    {
        _systemLog.error(message, e);
        _log.error(message, e);
    }

    private void close()
    {
        // TODO: close connection and prepared statements used for bulk inserts
//        if (null != _conn)
//            TargetedMSManager.getSchema().getScope().releaseConnection(_conn);
    }

    public static class RunInfo implements Serializable
    {
        private final int _runId;
        private final boolean _alreadyImported;

        private RunInfo(int runId, boolean alreadyImported)
        {
            _runId = runId;

            _alreadyImported = alreadyImported;
        }

        public int getRunId()
        {
            return _runId;
        }

        public boolean isAlreadyImported()
        {
            return _alreadyImported;
        }
    }
}
