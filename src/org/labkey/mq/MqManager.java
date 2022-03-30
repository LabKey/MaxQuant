/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.mq;

import org.apache.logging.log4j.Logger;
import org.fhcrc.cpas.exp.xml.ExperimentArchiveDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.AbstractFileXarSource;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.XarFormatException;
import org.labkey.api.exp.XarSource;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.parser.SummaryTemplateParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MqManager
{
    private static final Logger _log = LogHelper.getLogger(MqManager.class, "MaxQuant utility code");

    private MqManager()
    {
        // prevent external construction with a private default constructor
    }

    public static TableInfo getTableInfoExperimentGroup()
    {
        return getSchema().getTable(MqSchema.TABLE_EXPERIMENT_GROUP);
    }

    public static TableInfo getTableInfoExperiment()
    {
        return getSchema().getTable(MqSchema.TABLE_EXPERIMENT);
    }

    public static TableInfo getTableInfoRawFile()
    {
        return getSchema().getTable(MqSchema.TABLE_RAW_FILE);
    }

    public static TableInfo getTableInfoProteinGroup()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP);
    }

    public static TableInfo getTableInfoProteinGroupTMT()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP_TMT);
    }

    public static TableInfo getTableInfoProteinGroupExperimentInfo()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP_EXPERIMENT_INFO);
    }

    public static TableInfo getTableInfoProteinGroupRatiosSilac()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP_RATIOS_SILAC);
    }

    public static TableInfo getTableInfoProteinGroupIntensitySilac()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP_INTENSITY_SILAC);
    }

    public static TableInfo getTableInfoPeptide()
    {
        return getSchema().getTable(MqSchema.TABLE_PEPTIDE);
    }

    public static TableInfo getTableInfoPeptideTMT()
    {
        return getSchema().getTable(MqSchema.TABLE_PEPTIDE_TMT);
    }

    public static TableInfo getTableInfoProteinGroupPeptide()
    {
        return getSchema().getTable(MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE);
    }

    public static TableInfo getTableInfoModifiedPeptide()
    {
        return getSchema().getTable(MqSchema.TABLE_MODIFIED_PEPTIDE);
    }

    public static TableInfo getTableInfoModifiedPeptideTMT()
    {
        return getSchema().getTable(MqSchema.TABLE_MODIFIED_PEPTIDE_TMT);
    }

    public static TableInfo getTableInfoEvidence()
    {
        return getSchema().getTable(MqSchema.TABLE_EVIDENCE);
    }

    public static TableInfo getTableInfoEvidenceTMT()
    {
        return getSchema().getTable(MqSchema.TABLE_EVIDENCE_TMT);
    }

    public static TableInfo getTableInfoEvidenceIntensitySilac()
    {
        return getSchema().getTable(MqSchema.TABLE_EVIDENCE_INETNSITY_SILAC);
    }

    public static TableInfo getTableInfoEvidenceRatioSilac()
    {
        return getSchema().getTable(MqSchema.TABLE_EVIDENCE_RATIO_SILAC);
    }

    public static TableInfo getTableInfoTMTChannel()
    {
        return getSchema().getTable(MqSchema.TABLE_TMT_CHANNEL);
    }

    public static DbSchema getSchema()
    {
        return MqSchema.getSchema();
    }

    public static ExperimentGroup getExperimentGroupByDataId(int dataId, Container c)
    {
        ExperimentGroup[] expGrps = getExperimentGroups("DataId = ? AND Deleted = ? AND Container = ?", dataId, Boolean.FALSE, c.getId());
        if(null == expGrps || expGrps.length == 0)
        {
            return null;
        }
        if(expGrps.length == 1)
        {
            return expGrps[0];
        }
        throw new IllegalStateException("There is more than one non-deleted ExperimentGroup for dataId " + dataId);
    }

    public static ExperimentGroup getExperimentGroup(int experimentGroupId, Container c)
    {
        ExperimentGroup run = null;

        ExperimentGroup[] runs = getExperimentGroups("Id = ? AND deleted = ? AND Container = ?", experimentGroupId, Boolean.FALSE, c.getId());

        if (runs != null && runs.length == 1)
        {
            run = runs[0];
        }

        return run;
    }

    private static ExperimentGroup[] getExperimentGroups(String whereClause, Object... params)
    {
        SQLFragment sql = new SQLFragment("SELECT * FROM ");
        sql.append(getTableInfoExperimentGroup(), "ef");
        sql.append(" WHERE ");
        sql.append(whereClause);
        sql.addAll(params);
        return new SqlSelector(getSchema(), sql).getArray(ExperimentGroup.class);
    }

    public static Integer addRunToQueue(ViewBackgroundInfo info,
                                        final File file,
                                        PipeRoot root) throws SQLException, IOException, XarFormatException
    {
        String description = "MaxQuant results import - " + file.getParent();
        User user = info.getUser();
        Container container = info.getContainer();
        XarContext xarContext = new XarContext(description, container, user);

        // If an entry does not already exist for this data file in exp.data create it now.
        // This will happen if a file was copied to the pipeline directory instead
        // of being uploaded via the files browser.
        ExpData expData = ExperimentService.get().getExpDataByURL(file, container);
        if(expData == null)
        {
            XarSource source = new AbstractFileXarSource("Wrap MaxQuant Run", container, user)
            {
                @Override
                public Path getLogFilePath()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Path getRootPath()
                {
                    return file.getParentFile().toPath();
                }

                @Override
                public ExperimentArchiveDocument getDocument()
                {
                    throw new UnsupportedOperationException();
                }
            };

            expData = ExperimentService.get().createData(file.toURI(), source);
        }

        MqExperimentImporter importer = new MqExperimentImporter(user, container, description, expData, null, xarContext);
        MqExperimentImporter.RunInfo runInfo = importer.prepareExperimentGroup();

        MqImportPipelineJob job = new MqImportPipelineJob(info, expData, runInfo, root);
        try
        {
            PipelineService.get().queueJob(job);
            return PipelineService.get().getJobId(user, container, job.getJobGUID());
        }
        catch (PipelineValidationException e)
        {
            throw new IOException(e);
        }
    }

    public static ExpRun ensureWrapped(ExperimentGroup expGrp, User user) throws ExperimentException
    {
        ExpRun expRun;
        if (expGrp.getExperimentRunLSID() != null)
        {
            expRun = ExperimentService.get().getExpRun(expGrp.getExperimentRunLSID());
            if (expRun != null && expRun.getContainer().equals(expGrp.getContainer()))
            {
                return expRun;
            }
        }
        return wrapRun(expGrp, user);
    }

    private static ExpRun wrapRun(ExperimentGroup run, User user) throws ExperimentException
    {
        try (DbScope.Transaction transaction = ExperimentService.get().getSchema().getScope().ensureTransaction())
        {
            Container container = run.getContainer();

            // Make sure that we have a protocol in this folder
            String protocolPrefix = MqModule.IMPORT_MQ_PROTOCOL_OBJECT_PREFIX;

            Lsid lsid = new Lsid("Protocol.Folder-" + container.getRowId(), protocolPrefix);
            ExpProtocol protocol = ExperimentService.get().getExpProtocol(lsid.toString());
            if (protocol == null)
            {
                protocol = ExperimentService.get().createExpProtocol(container, ExpProtocol.ApplicationType.ProtocolApplication, "MaxQuant Import", lsid.toString());
                protocol.setMaxInputMaterialPerInstance(0);
                protocol = ExperimentService.get().insertSimpleProtocol(protocol, user);
            }

            ExpData expData = ExperimentService.get().getExpData(run.getDataId());
            File exptDesignTemplate = expData.getFile();

            ExpRun expRun = ExperimentService.get().createExperimentRun(container, run.getDescription());
            expRun.setProtocol(protocol);
            expRun.setFilePathRoot(exptDesignTemplate.getParentFile());
            ViewBackgroundInfo info = new ViewBackgroundInfo(container, user, null);

            Map<ExpData, String> inputDatas = new HashMap<>();
            Map<ExpData, String> outputDatas = new HashMap<>();

            outputDatas.put(expData, SummaryTemplateParser.FILE);

            expRun = ExperimentService.get().saveSimpleExperimentRun(expRun,
                    Collections.emptyMap(),
                    inputDatas,
                    Collections.emptyMap(),
                    outputDatas,
                    Collections.emptyMap(),
                    info, _log, false);

            run.setExperimentRunLSID(expRun.getLSID());
            updateExperimentGroup(run, user);

            transaction.commit();
            return expRun;
        }
    }

    public static void updateExperimentGroup(ExperimentGroup expGrp, User user)
    {
        Table.update(user, getTableInfoExperimentGroup(), expGrp, expGrp.getId());
    }

    // pulled out into separate method so could be called by itself from data handlers
    public static void markDeleted(@Nullable List<Integer> runIds, Container c, User user)
    {
        SQLFragment markDeleted = new SQLFragment("UPDATE " + getTableInfoExperimentGroup() + " SET ExperimentRunLSID = NULL, Deleted=?, Modified=? ", Boolean.TRUE, new Date());
        SimpleFilter where = new SimpleFilter();
        where.addCondition(FieldKey.fromParts("Container"), c.getId());
        if (runIds != null)
            where.addInClause(FieldKey.fromParts("Id"), runIds);
        markDeleted.append(where.getSQLFragment(getSqlDialect()));

        new SqlExecutor(getSchema()).execute(markDeleted);
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static void purgeDeletedExperimentGroups()
    {
        String experimentGroupIdWhereClause = " WHERE ExperimentGroupId IN (SELECT Id FROM " + getTableInfoExperimentGroup() + " WHERE Deleted = ?)";
        String experimentIdWhereClause = " WHERE ExperimentId IN (SELECT Id FROM " + getTableInfoExperiment() + experimentGroupIdWhereClause + ")";
        String proteinGroupIdWhereClause = " WHERE ProteinGroupId IN (SELECT Id FROM " + getTableInfoProteinGroup() + experimentGroupIdWhereClause + ")";
        String peptideIdWhereClause = " WHERE PeptideId IN (SELECT Id FROM " + getTableInfoPeptide() + experimentGroupIdWhereClause + ")";
        String modifiedPeptideIdWhereClause = " WHERE ModifiedPeptideId IN (SELECT Id FROM " + getTableInfoModifiedPeptide() + peptideIdWhereClause + ")";
        String evidenceIdWhereClause = " WHERE EvidenceId IN (SELECT Id FROM " + getTableInfoEvidence() + experimentIdWhereClause + ")";

        // Delete from the EvidenceIntensitySilac
        execute("DELETE FROM " + getTableInfoEvidenceIntensitySilac() + evidenceIdWhereClause, true);

        // Delete from the EvidenceRatioSilac
        execute("DELETE FROM " + getTableInfoEvidenceRatioSilac() + evidenceIdWhereClause, true);

        // Delete from the EvidenceTMT
        execute("DELETE FROM " + getTableInfoEvidenceTMT() + evidenceIdWhereClause, true);

        // Delete from the Evidence
        execute("DELETE FROM " + getTableInfoEvidence() + experimentIdWhereClause, true);

        // Delete from ProteinGroupExperimentInfo
        execute("DELETE FROM " + getTableInfoProteinGroupExperimentInfo() + proteinGroupIdWhereClause, true);

        // Delete from ProteinGroupRatiosSilac
        execute("DELETE FROM " + getTableInfoProteinGroupRatiosSilac() + proteinGroupIdWhereClause, true);

        // Delete from ProteinGroupIntensitySilac
        execute("DELETE FROM " + getTableInfoProteinGroupIntensitySilac() + proteinGroupIdWhereClause, true);

        // Delete from ProteinGroupPeptide
        execute("DELETE FROM " + getTableInfoProteinGroupPeptide() + proteinGroupIdWhereClause, true);

        // Delete from ProteinGroupTMT
        execute("DELETE FROM " + getTableInfoProteinGroupTMT() + proteinGroupIdWhereClause, true);

        // Delete from ProteinGroup
        execute("DELETE FROM " + getTableInfoProteinGroup() + experimentGroupIdWhereClause, true);

        // Delete from ModifiedPeptideTMT
        execute("DELETE FROM " + getTableInfoModifiedPeptideTMT() + modifiedPeptideIdWhereClause, true);

        // Delete from ModifiedPeptide
        execute("DELETE FROM " + getTableInfoModifiedPeptide() + peptideIdWhereClause, true);

        // Delete from PeptideTMT
        execute("DELETE FROM " + getTableInfoPeptideTMT() + peptideIdWhereClause, true);

        // Delete from Peptide
        execute("DELETE FROM " + getTableInfoPeptide() + experimentGroupIdWhereClause, true);

        // Delete from the RawFile
        execute("DELETE FROM " + getTableInfoRawFile() + experimentIdWhereClause, true);

        // Delete from TMTChannel
        execute("DELETE FROM " + getTableInfoTMTChannel() + experimentGroupIdWhereClause, true);

        // Delete from Experiment
        execute("DELETE FROM " + getTableInfoExperiment() + experimentGroupIdWhereClause, true);

        // Delete from ExperimentGroup
        execute("DELETE FROM " + getTableInfoExperimentGroup() + " WHERE Deleted = ?", true);
    }

    private static void execute(String sql, @NotNull Object... parameters)
    {
        new SqlExecutor(getSchema()).execute(sql, parameters);
    }
}
