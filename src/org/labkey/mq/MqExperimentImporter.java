package org.labkey.mq;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.Table;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.security.User;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.mq.model.Evidence;
import org.labkey.mq.model.EvidenceIntensitySilac;
import org.labkey.mq.model.EvidenceRatioSilac;
import org.labkey.mq.model.EvidenceTMT;
import org.labkey.mq.model.Experiment;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.ModifiedPeptide;
import org.labkey.mq.model.ModifiedPeptideTMT;
import org.labkey.mq.model.Peptide;
import org.labkey.mq.model.PeptideTMT;
import org.labkey.mq.model.ProteinGroup;
import org.labkey.mq.model.ProteinGroupExperimentInfo;
import org.labkey.mq.model.ProteinGroupIntensitySilac;
import org.labkey.mq.model.ProteinGroupRatioSilac;
import org.labkey.mq.model.ProteinGroupTMT;
import org.labkey.mq.model.RawFile;
import org.labkey.mq.model.TMTChannel;
import org.labkey.mq.model.TMTInfo;
import org.labkey.mq.parser.EvidenceParser;
import org.labkey.mq.parser.SummaryTemplateParser;
import org.labkey.mq.parser.ModifiedPeptidesParser;
import org.labkey.mq.parser.MqParserException;
import org.labkey.mq.parser.PeptidesParser;
import org.labkey.mq.parser.ProteinGroupsParser;
import org.labkey.mq.query.ModifiedPeptideManager;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
    private Map<Integer, Integer> _maxQuantProteinGroupIdToDbId;
    private Map<Integer, Integer> _maxQuantPeptideIdToDbId;
    private Map<Integer, Integer> _maxQuantModifiedPeptideIdToDbId;
    private Map<Integer, Integer> _maxQuantTMTChannelToDbId;

    // Use passed in logger for import status, information, and file format problems.  This should end up in the pipeline log.
    protected Logger _log;
    private PipelineJob _contextJob;

    // Use system logger for bugs & system problems, and in cases where we don't have a pipeline logger
    protected static Logger _systemLog = Logger.getLogger(MqExperimentImporter.class);
    protected final XarContext _context;

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;
    private static final String IMPORT_STARTED = "Importing... (refresh to check status)";
    private static final String IMPORT_FAILED = "Import failed (see pipeline log)";
    private static final String IMPORT_SUCCEEDED = "";
    private static final int TRANSACTION_ROW_COUNT = 10000;

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
        _contextJob = context.getJob();
    }

    public ExperimentGroup importExperiment(RunInfo runInfo) throws MqParserException
    {
        _experimentGroupId = runInfo.getRunId();

        ExperimentGroup run = MqManager.getExperimentGroup(_experimentGroupId, _container);

        // Skip if run was already fully imported
        if (runInfo.isAlreadyImported() && run != null && run.getStatusId() == STATUS_SUCCESS)
        {
            _log.info(_expData.getFile() + " has already been imported so it does not need to be imported again");
            return run;
        }

        File summaryTemplatefile = _expData.getFile();
        File experimentDirectory = summaryTemplatefile.getParentFile();

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
            if(!summaryTemplatefile.exists())
            {
                throw new MqParserException("Could not find file " + summaryTemplatefile.getName() + " in directory " + experimentDirectory + ".");
            }

            ExperimentGroup experimentGroup = new SummaryTemplateParser().parse(summaryTemplatefile);
            String derivedExperimentName = null;
            for(Experiment experiment: experimentGroup.getExperiments())
            {
                if(experiment.isDerivedExperimentName()){
                    derivedExperimentName = experiment.getExperimentName();
                }
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

            File txtDir = new File(experimentDirectory, "txt");
            if(!txtDir.exists())
            {
                txtDir= experimentDirectory;
            }

            // Parse proteinGroups.txt;
            parseProteinGroups(txtDir, experimentGroup);

            // Parse peptides.txt;
            parsePeptides(txtDir, experimentGroup);

            // Parse modificationSpecificPeptides.txt (note: this file is optional)
            parseModifiedPeptides(txtDir, experimentGroup);

            // parse evidence.txt
            parseEvidence(txtDir, experimentGroup, derivedExperimentName);

            _log.info("Completed import of MaxQuant document from " + run.getFileName());

            updateRunStatus(IMPORT_SUCCEEDED, STATUS_SUCCESS);

            return MqManager.getExperimentGroup(_experimentGroupId, _container);
        }
        catch (MqParserException mqe)
        {
            logError("MaxQuant import failed.", mqe);
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

    private void parseProteinGroups(File txtDir, ExperimentGroup experimentGroup)
    {
        File proteinGrpsFile = new File(txtDir, ProteinGroupsParser.FILE);
        if(!proteinGrpsFile.exists())
        {
            throw new MqParserException("Could not find proteinGroups.txt in " + txtDir.getPath());
        }

        logFileProcessingStart(proteinGrpsFile);

        int count = 0;
        ProteinGroupsParser pgParser = new ProteinGroupsParser(proteinGrpsFile);
        ProteinGroupsParser.ProteinGroupRow row;
        _maxQuantProteinGroupIdToDbId = new HashMap<>();
        _maxQuantTMTChannelToDbId = new HashMap<>();
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            while((row = pgParser.nextProteinGroup(experimentGroup.getExperiments())) != null)
            {
                ProteinGroup pg = new ProteinGroup();
                pg.setExperimentGroupId(_experimentGroupId);
                pg.setContainer(_container);
                pg.copyFrom(row);

                Table.insert(_user, MqManager.getTableInfoProteinGroup(), pg);
                _maxQuantProteinGroupIdToDbId.put(pg.getMaxQuantId(), pg.getId());

                Map<Experiment, ProteinGroupsParser.ExperimentInfo> experimentCoverages = row.getExperimentInfos();
                for(Map.Entry<Experiment, ProteinGroupsParser.ExperimentInfo> entry: experimentCoverages.entrySet())
                {
                    ProteinGroupExperimentInfo info = new ProteinGroupExperimentInfo();
                    info.setContainer(_container);
                    info.setExperimentId(entry.getKey().getId());
                    info.setProteinGroupId(pg.getId());
                    ProteinGroupsParser.ExperimentInfo expInfo = entry.getValue();
                    info.setCoverage(expInfo.getCoverage());
                    info.setIntensity(expInfo.getIntensity());
                    info.setLfqIntensity(expInfo.getLfqIntensity());

                    Table.insert(_user, MqManager.getTableInfoProteinGroupExperimentInfo(), info);
                }

                Map<Experiment, List<ProteinGroupsParser.SilacRatio>> ratios = row.getExperimentRatios();
                for(Map.Entry<Experiment, List<ProteinGroupsParser.SilacRatio>> entry: ratios.entrySet())
                {
                    List<ProteinGroupsParser.SilacRatio> sRatios = entry.getValue();
                    for(ProteinGroupsParser.SilacRatio ratio: sRatios)
                    {
                        ProteinGroupRatioSilac silacRatios = new ProteinGroupRatioSilac();
                        silacRatios.setExperimentId(entry.getKey().getId());
                        silacRatios.setContainer(_container);
                        silacRatios.setProteinGroupId(pg.getId());
                        silacRatios.setRatioType(ratio.getRatioType());
                        silacRatios.setRatio(ratio.getRatio());
                        silacRatios.setRatioNormalized(ratio.getRatioNormalized());
                        silacRatios.setRatioCount(ratio.getRatioCount());

                        Table.insert(_user, MqManager.getTableInfoProteinGroupRatiosSilac(), silacRatios);
                    }
                }

                Map<Experiment, List<ProteinGroupsParser.SilacIntensity>> silacIntensities = row.getSilacExperimentIntensities();
                for(Map.Entry<Experiment, List<ProteinGroupsParser.SilacIntensity>> entry: silacIntensities.entrySet())
                {
                    List<ProteinGroupsParser.SilacIntensity> intensities = entry.getValue();
                    for(ProteinGroupsParser.SilacIntensity sInt: intensities)
                    {
                        if(sInt.getIntensity() == null)
                        {
                            continue; // There will be no value for non-SILAC experiments.
                        }
                        ProteinGroupIntensitySilac silacIntensity = new ProteinGroupIntensitySilac();
                        silacIntensity.setExperimentId(entry.getKey().getId());
                        silacIntensity.setContainer(_container);
                        silacIntensity.setProteinGroupId(pg.getId());
                        silacIntensity.setLabelType(sInt.getLabel());
                        silacIntensity.setIntensity(sInt.getIntensity());

                        Table.insert(_user, MqManager.getTableInfoProteinGroupIntensitySilac(), silacIntensity);
                    }
                }

                for (TMTInfo tmtInfo : row.getTMTInfos())
                {
                    // ensure that the distinct tagNumbers are added to the TMTChannel table
                    if (_maxQuantTMTChannelToDbId.get(tmtInfo.getTagNumber()) == null)
                    {
                        TMTChannel tmtChannel = new TMTChannel();
                        tmtChannel.setContainer(_container);
                        tmtChannel.setExperimentGroupId(_experimentGroupId);
                        tmtChannel.setTagNumber(tmtInfo.getTagNumber());
                        Table.insert(_user, MqManager.getTableInfoTMTChannel(), tmtChannel);
                        _maxQuantTMTChannelToDbId.put(tmtInfo.getTagNumber(), tmtChannel.getId());
                    }

                    // update the record with the DB id for the TMTChannel
                    Integer tmtChannelId = _maxQuantTMTChannelToDbId.get(tmtInfo.getTagNumber());
                    tmtInfo.setTMTChannelId(tmtChannelId);

                    ProteinGroupTMT proteinGroupTMT = new ProteinGroupTMT();
                    proteinGroupTMT.setContainer(_container);
                    proteinGroupTMT.setProteinGroupId(pg.getId());
                    proteinGroupTMT.copyFrom(tmtInfo);
                    Table.insert(_user, MqManager.getTableInfoProteinGroupTMT(), proteinGroupTMT);
                }

                count++;
                if (count % TRANSACTION_ROW_COUNT == 0)
                {
                    transaction.commitAndKeepConnection();
                    _log.info("Parsed " + count + " protein groups");
                }
            }

            transaction.commit();
        }

        logFileProcessingEnd(proteinGrpsFile.getPath(), count + " protein groups");
    }

    private void logFileProcessingStart(File file)
    {
        _log.info("Parsing results in " + file.getPath());
        if (_contextJob != null)
            _contextJob.setStatus("IMPORTING " + file.getName(), "Task started at: " + DateUtil.nowISO());
    }

    private void logFileProcessingEnd(String file, String message)
    {
        _log.info("Finished parsing results in " + file + ". " + message);
    }

    private void parsePeptides(File txtDir, ExperimentGroup experimentGroup)
    {
        File peptidesFile = new File(txtDir, PeptidesParser.FILE);
        if(!peptidesFile.exists())
        {
            throw new MqParserException("Could not find " + peptidesFile.getName() + " in " + txtDir.getPath());
        }

        logFileProcessingStart(peptidesFile);

        int count = 0;
        PeptidesParser pepParser = new PeptidesParser(peptidesFile);
        PeptidesParser.PeptideRow row;
        _maxQuantPeptideIdToDbId = new HashMap<>();
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            while((row = pepParser.nextPeptide(experimentGroup.getExperiments())) != null)
            {
                Peptide peptide = new Peptide(row);
                peptide.setExperimentGroupId(_experimentGroupId);
                peptide.setContainer(_container);

                Table.insert(_user, MqManager.getTableInfoPeptide(), peptide);
                _maxQuantPeptideIdToDbId.put(peptide.getMaxQuantId(), peptide.getId());

                for(int mqProteinGroupId: row.getMaxQuantProteinGroupIds())
                {
                    Integer proteinGroupId = _maxQuantProteinGroupIdToDbId.get(mqProteinGroupId);
                    if (proteinGroupId == null)
                        throw new MqParserException("Could not find database ID for MaxQuant protein group ID " + mqProteinGroupId);

                    Map<String, Integer> mapping = new HashMap<>();
                    mapping.put("ProteinGroupId", proteinGroupId);
                    mapping.put("PeptideId", peptide.getId());

                    Table.insert(_user, MqManager.getTableInfoProteinGroupPeptide(), mapping);
                }

                for (TMTInfo tmtInfo : row.getTMTInfos())
                {
                    // update the record with the DB id for the TMTChannel
                    Integer tmtChannelId = _maxQuantTMTChannelToDbId.get(tmtInfo.getTagNumber());
                    if (tmtChannelId == null)
                        throw new MqParserException("Could not find database ID for MaxQuant TMT Channel " + tmtInfo.getTagNumber());
                    tmtInfo.setTMTChannelId(tmtChannelId);

                    PeptideTMT peptideTMT = new PeptideTMT();
                    peptideTMT.setContainer(_container);
                    peptideTMT.setPeptideId(peptide.getId());
                    peptideTMT.copyFrom(tmtInfo);
                    Table.insert(_user, MqManager.getTableInfoPeptideTMT(), peptideTMT);
                }

                count++;
                if (count % TRANSACTION_ROW_COUNT == 0)
                {
                    transaction.commitAndKeepConnection();
                    _log.info("Parsed " + count + " peptides");
                }
            }

            transaction.commit();
        }

        logFileProcessingEnd(peptidesFile.getPath(), count + " peptides");
    }

    private void parseModifiedPeptides(File txtDir, ExperimentGroup experimentGroup)
    {
        File modifiedPeptidesFile = new File(txtDir, ModifiedPeptidesParser.FILE);
        if (!modifiedPeptidesFile.exists())
            return;

        logFileProcessingStart(modifiedPeptidesFile);

        ModifiedPeptidesParser pepParser = new ModifiedPeptidesParser(modifiedPeptidesFile);
        ModifiedPeptidesParser.ModifiedPeptideRow row;
        _maxQuantModifiedPeptideIdToDbId = new HashMap<>();

        int count = 0;
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            while((row = pepParser.nextModifiedPeptide(experimentGroup.getExperiments())) != null)
            {
                ModifiedPeptide modPeptide = new ModifiedPeptide(row);
                Integer peptideId = _maxQuantPeptideIdToDbId.get(row.getMaxQuantPeptideId());
                if (peptideId == null)
                    throw new MqParserException("Could not find database ID for MaxQuant peptide ID " + row.getMaxQuantPeptideId());

                modPeptide.setPeptideId(peptideId);
                modPeptide.setContainer(_container);

                Table.insert(_user, MqManager.getTableInfoModifiedPeptide(), modPeptide);
                _maxQuantModifiedPeptideIdToDbId.put(modPeptide.getMaxQuantId(), modPeptide.getId());

                for (TMTInfo tmtInfo : row.getTMTInfos())
                {
                    // update the record with the DB id for the TMTChannel
                    Integer tmtChannelId = _maxQuantTMTChannelToDbId.get(tmtInfo.getTagNumber());
                    if (tmtChannelId == null)
                        throw new MqParserException("Could not find database ID for MaxQuant TMT Channel " + tmtInfo.getTagNumber());
                    tmtInfo.setTMTChannelId(tmtChannelId);

                    ModifiedPeptideTMT modifiedPeptideTMT = new ModifiedPeptideTMT();
                    modifiedPeptideTMT.setContainer(_container);
                    modifiedPeptideTMT.setModifiedPeptideId(modPeptide.getId());
                    modifiedPeptideTMT.copyFrom(tmtInfo);
                    Table.insert(_user, MqManager.getTableInfoModifiedPeptideTMT(), modifiedPeptideTMT);
                }

                count++;
                if (count % TRANSACTION_ROW_COUNT == 0)
                {
                    transaction.commitAndKeepConnection();
                    _log.info("Parsed " + count + " modified peptides");
                }
            }

            transaction.commit();
        }

        logFileProcessingEnd(modifiedPeptidesFile.getPath(), count + " modified peptides");
    }

    private void parseEvidence(File txtDir, ExperimentGroup experimentGroup, String derivedExperimentName)
    {
        File evidenceFile = new File(txtDir, EvidenceParser.FILE);

        if(!evidenceFile.exists())
        {
            return;
        }

        logFileProcessingStart(evidenceFile);

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

        int count = 0;
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            while((row = pepParser.nextEvidence(experimentGroup.getExperiments(), derivedExperimentName)) != null)
            {
                Evidence evidence = new Evidence(row);
                evidence.setContainer(_container);
                Integer peptideId = _maxQuantPeptideIdToDbId.get(row.getMaxQuantPeptideId());
                if (peptideId == null)
                    throw new MqParserException("Could not find database ID for MaxQuant peptide ID " + row.getMaxQuantPeptideId());
                evidence.setPeptideId(peptideId);

                if (_maxQuantModifiedPeptideIdToDbId != null)
                {
                    Integer modifiedPeptideId = _maxQuantModifiedPeptideIdToDbId.get(row.getMaxQuantModifiedPeptideId());
                    if (modifiedPeptideId == null)
                        throw new MqParserException("Could not find database ID for MaxQuant modified peptide ID " + row.getMaxQuantModifiedPeptideId());
                    evidence.setModifiedPeptideId(modifiedPeptideId);
                }

                Integer experimentId = experimentNameToDbId.get(row.getExperiment());
                if (experimentId == null)
                    throw new IllegalArgumentException("Unable to find experiment with name:" + row.getExperiment());
                evidence.setExperimentId(experimentId);
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
                if(evidence.getModifiedPeptideId() != null)
                {
                    ModifiedPeptide modPeptide = ModifiedPeptideManager.get(evidence.getModifiedPeptideId(), _container);
                    if (!StringUtils.isBlank(row.getModifiedSequence()))
                    {
                        modPeptide.setSequence(row.getModifiedSequence());
                        Table.update(_user, MqManager.getTableInfoModifiedPeptide(), modPeptide, modPeptide.getId());
                    }
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

                for(Map.Entry<String, Long> entry: row.getSilacIntensities().entrySet())
                {
                    EvidenceIntensitySilac eiSilac = new EvidenceIntensitySilac();
                    eiSilac.setContainer(_container);
                    eiSilac.setEvidenceId(evidence.getId());
                    eiSilac.setLabelType(entry.getKey());
                    eiSilac.setIntensity(entry.getValue());
                    Table.insert(_user, MqManager.getTableInfoEvidenceIntensitySilac(), eiSilac);
                }

                for (TMTInfo tmtInfo : row.getTMTInfos())
                {
                    // update the record with the DB id for the TMTChannel
                    Integer tmtChannelId = _maxQuantTMTChannelToDbId.get(tmtInfo.getTagNumber());
                    if (tmtChannelId == null)
                        throw new MqParserException("Could not find database ID for MaxQuant TMT Channel " + tmtInfo.getTagNumber());
                    tmtInfo.setTMTChannelId(tmtChannelId);

                    EvidenceTMT evidenceTMT = new EvidenceTMT();
                    evidenceTMT.setContainer(_container);
                    evidenceTMT.setEvidenceId(evidence.getId());
                    evidenceTMT.copyFrom(tmtInfo);
                    Table.insert(_user, MqManager.getTableInfoEvidenceTMT(), evidenceTMT);
                }

                count++;
                if (count % TRANSACTION_ROW_COUNT == 0)
                {
                    transaction.commitAndKeepConnection();
                    _log.info("Parsed " + count + " evidence");
                }
            }
            
            transaction.commit();
        }

        logFileProcessingEnd(evidenceFile.getPath(), count + " evidence");
    }

    private static final ReentrantLock _schemaLock = new ReentrantLock();

    protected RunInfo prepareExperimentGroup()
    {
        try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction(_schemaLock))
        {
            boolean alreadyImported = false;

            // Don't import if we've already imported this file (undeleted run exists matching this file name)
            _experimentGroupId = getExperimentGroupId();
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

    private int getExperimentGroupId()
    {
        ExperimentGroup expGrp = MqManager.getExperimentGroupByDataId(_expData.getRowId(), _container);
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
