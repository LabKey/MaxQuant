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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MqSchema extends UserSchema
{
    public static final String NAME = "mq";
    public static final String SCHEMA_DESCR = "Contains data imported from MaxQuant results";

    // Tables
    public static final String TABLE_EXPERIMENT_GROUP = "ExperimentGroup";
    public static final String TABLE_EXPERIMENT = "Experiment";
    public static final String TABLE_RAW_FILE = "RawFile";
    public static final String TABLE_PROTEIN_GROUP = "ProteinGroup";
    public static final String TABLE_PROTEIN_GROUP_SEQUENCE_COVERAGE = "ProteinGroupSequenceCoverage";
    public static final String TABLE_PROTEIN_GROUP_INTENSITY = "ProteinGroupIntensity";
    public static final String TABLE_PROTEIN_GROUP_RATIOS_SILAC = "ProteinGroupRatiosSilac";
    public static final String TABLE_PEPTIDE = "Peptide";
    public static final String TABLE_PROTEIN_GROUP_PEPTIDE = "ProteinGroupPeptide";
    public static final String TABLE_MODIFIED_PEPTIDE = "ModifiedPeptide";
    public static final String TABLE_EVIDENCE = "Evidence";
    public static final String TABLE_EVIDENCE_INETNSITY_SILAC = "EvidenceIntensitySilac";
    public static final String TABLE_EVIDENCE_RATIO_SILAC = "EvidenceRatioSilac";

    private static final String PROTOCOL_PATTERN_PREFIX = "urn:lsid:%:Protocol.%:";

    private ExpSchema _expSchema;

    static public void register(Module module)
    {
        DefaultSchema.registerProvider(NAME, new DefaultSchema.SchemaProvider(module)
        {
            public QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new MqSchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public MqSchema(User user, Container container)
    {
        super(NAME, SCHEMA_DESCR, user, container, MqManager.getSchema());
        _expSchema = new ExpSchema(user, container);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(NAME, DbSchemaType.Module);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    @Nullable
    @Override
    protected TableInfo createTable(String name)
    {
        if (TABLE_EXPERIMENT_GROUP.equalsIgnoreCase(name))
        {
            return getExperimentGroupTable();
        }
        else if(TABLE_EXPERIMENT.equalsIgnoreCase(name)
                || TABLE_RAW_FILE.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP_SEQUENCE_COVERAGE.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP_INTENSITY.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP_RATIOS_SILAC.equalsIgnoreCase(name)
                || TABLE_PEPTIDE.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP_PEPTIDE.equalsIgnoreCase(name)
                || TABLE_MODIFIED_PEPTIDE.equalsIgnoreCase(name)
                || TABLE_EVIDENCE.equalsIgnoreCase(name)
                || TABLE_EVIDENCE_INETNSITY_SILAC.equalsIgnoreCase(name)
                || TABLE_EVIDENCE_RATIO_SILAC.equalsIgnoreCase(name)
                )
        {
            FilteredTable table = new FilteredTable<>(getSchema().getTable(name), this);
            table.wrapAllColumns(true);
            return table;
        }

        return null;
    }

    public ExpRunTable getExperimentGroupTable()
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.getRunsTable();
        result.setDescription("Contains a row per MaxQuant experiment loaded in this folder.");

        // Filter to just the runs with the Targeted MS protocol
        result.setProtocolPatterns(PROTOCOL_PATTERN_PREFIX + MqModule.IMPORT_MQ_PROTOCOL_OBJECT_PREFIX + "%");

        // Add a lookup column to the ExperimentGroup table in the schema
        SQLFragment sql = new SQLFragment("(SELECT MIN(expgrp.Id)\n" +
                "\nFROM " + MqManager.getTableInfoExperimentGroup() + " expgrp " +
                "\nWHERE expgrp.ExperimentRunLSID = " + ExprColumn.STR_TABLE_ALIAS + ".LSID AND expgrp.Deleted = ?)");
        sql.add(Boolean.FALSE);
        ColumnInfo mqDetailColumn = new ExprColumn(result, "ExperimentGroup", sql, JdbcType.INTEGER);

        //ActionURL url = TargetedMSController.getShowRunURL(getContainer());
        //final ActionURL downloadUrl = new ActionURL(TargetedMSController.DownloadDocumentAction.class, getContainer());
        mqDetailColumn.setFk(new LookupForeignKey(null, "id", "Id", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                FilteredTable result = new FilteredTable<>(MqManager.getTableInfoExperimentGroup(), MqSchema.this);
                result.addWrapColumn(result.getRealTable().getColumn("Id"));
                result.addWrapColumn(result.getRealTable().getColumn("Created"));
                result.addWrapColumn(result.getRealTable().getColumn("CreatedBy"));
                result.addWrapColumn(result.getRealTable().getColumn("Filename"));
                result.addWrapColumn(result.getRealTable().getColumn("LocationOnFileSystem"));
                return result;
            }
        });
        mqDetailColumn.setHidden(false);
        result.addColumn(mqDetailColumn);


        //adjust the default visible columns
        List<FieldKey> columns = new ArrayList<>(result.getDefaultVisibleColumns());
        columns.remove(FieldKey.fromParts("File"));
        columns.remove(FieldKey.fromParts("Protocol"));
        columns.remove(FieldKey.fromParts("CreatedBy"));
        columns.remove(FieldKey.fromParts("RunGroups"));
        columns.remove(FieldKey.fromParts("Name"));

        columns.add(2, FieldKey.fromParts("ExperimentGroup"));
        columns.add(FieldKey.fromParts("ExperimentGroup", "Filename"));
        columns.add(FieldKey.fromParts("ExperimentGroup", "LocationOnFileSystem"));

        result.setDefaultVisibleColumns(columns);

        return result;
    }

    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_EXPERIMENT_GROUP);
        hs.add(TABLE_EXPERIMENT);
        hs.add(TABLE_RAW_FILE);
        hs.add(TABLE_PROTEIN_GROUP);
        hs.add(TABLE_PROTEIN_GROUP_SEQUENCE_COVERAGE);
        hs.add(TABLE_PROTEIN_GROUP_INTENSITY);
        hs.add(TABLE_PROTEIN_GROUP_RATIOS_SILAC);
        hs.add(TABLE_PEPTIDE);
        hs.add(TABLE_PROTEIN_GROUP_PEPTIDE);
        hs.add(TABLE_MODIFIED_PEPTIDE);
        hs.add(TABLE_EVIDENCE);
        hs.add(TABLE_EVIDENCE_INETNSITY_SILAC);
        hs.add(TABLE_EVIDENCE_RATIO_SILAC);
        return hs;
    }
}
