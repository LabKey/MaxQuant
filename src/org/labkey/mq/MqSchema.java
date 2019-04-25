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
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbSchemaType;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.module.Module;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.mq.query.EvidenceTable;
import org.labkey.mq.query.ExperimentGroupTable;
import org.labkey.mq.query.PeptideTable;
import org.labkey.mq.query.ProteinGroupPeptideTable;
import org.labkey.mq.query.ProteinGroupTable;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MqSchema extends SimpleUserSchema
{
    public static final String NAME = "mq";
    public static final String SCHEMA_DESCR = "Contains data imported from MaxQuant results";

    // Tables
    public static final String TABLE_EXPERIMENT_GROUP = "ExperimentGroup";
    public static final String TABLE_EXPERIMENT_GROUP_DETAILS = "ExperimentGroupDetails";
    public static final String TABLE_EXPERIMENT = "Experiment";
    public static final String TABLE_RAW_FILE = "RawFile";
    public static final String TABLE_PROTEIN_GROUP = "ProteinGroup";
    public static final String TABLE_PROTEIN_GROUP_TMT = "ProteinGroupTMT";
    public static final String TABLE_PROTEIN_GROUP_EXPERIMENT_INFO = "ProteinGroupExperimentInfo";
    public static final String TABLE_PROTEIN_GROUP_RATIOS_SILAC = "ProteinGroupRatiosSilac";
    public static final String TABLE_PROTEIN_GROUP_INTENSITY_SILAC = "ProteinGroupIntensitySilac";
    public static final String TABLE_PEPTIDE = "Peptide";
    public static final String TABLE_PEPTIDE_TMT = "PeptideTMT";
    public static final String TABLE_PROTEIN_GROUP_PEPTIDE = "ProteinGroupPeptide";
    public static final String TABLE_MODIFIED_PEPTIDE = "ModifiedPeptide";
    public static final String TABLE_MODIFIED_PEPTIDE_TMT = "ModifiedPeptideTMT";
    public static final String TABLE_EVIDENCE = "Evidence";
    public static final String TABLE_EVIDENCE_TMT = "EvidenceTMT";
    public static final String TABLE_EVIDENCE_INETNSITY_SILAC = "EvidenceIntensitySilac";
    public static final String TABLE_EVIDENCE_RATIO_SILAC = "EvidenceRatioSilac";
    public static final String TABLE_TMT_CHANNEL = "TMTChannel";

    public static final String QUERY_EVIDENCE_RATIO_SILAC_PIVOT = "EvidenceRatioSilacPivot";
    public static final String QUERY_PROTEIN_GROUP_TMT_PIVOT = "ProteinGroupTMTPivot";
    public static final String QUERY_PEPTIDE_TMT_PIVOT = "PeptideTMTPivot";
    public static final String QUERY_MODIFIED_PEPTIDE_TMT_PIVOT = "ModifiedPeptideTMTPivot";
    public static final String QUERY_EVIDENCE_TMT_PIVOT = "EvidenceTMTPivot";

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
        super(NAME, SCHEMA_DESCR, user, container, getSchema());
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
    public TableInfo createTable(String name, ContainerFilter cf)
    {
        if (TABLE_EXPERIMENT_GROUP.equalsIgnoreCase(name) || "Runs".equalsIgnoreCase(name))
        {
            return getRunsTable();
        }
        else if (TABLE_EXPERIMENT_GROUP_DETAILS.equalsIgnoreCase(name))
        {
            return new ExperimentGroupTable(this, cf);
        }
        else if (TABLE_PROTEIN_GROUP.equalsIgnoreCase(name))
        {
            return new ProteinGroupTable(this, cf);
        }
        else if (TABLE_PROTEIN_GROUP_PEPTIDE.equalsIgnoreCase(name))
        {
            return new ProteinGroupPeptideTable(this, cf);
        }
        else if (TABLE_PEPTIDE.equalsIgnoreCase(name))
        {
            return new PeptideTable(this, cf);
        }
        else if (TABLE_EVIDENCE.equalsIgnoreCase(name))
        {
            return new EvidenceTable(this, cf);
        }
        else if (!getTableNames().contains(name))
        {
            return null;
        }

        SimpleUserSchema.SimpleTable<MqSchema> table = new SimpleUserSchema.SimpleTable<>(this, createSourceTable(name), cf).init();
        table.setReadOnly(true);

        if (TABLE_PROTEIN_GROUP_EXPERIMENT_INFO.equalsIgnoreCase(name) || TABLE_PROTEIN_GROUP_RATIOS_SILAC.equalsIgnoreCase(name)
                || TABLE_PROTEIN_GROUP_INTENSITY_SILAC.equalsIgnoreCase(name) || TABLE_PROTEIN_GROUP_TMT.equalsIgnoreCase(name)
            )
        {

            List<FieldKey> defaultVisible = table.getDefaultVisibleColumns();
            List<FieldKey> visibleColumns = new ArrayList<>();
            for (FieldKey fk: defaultVisible)
            {
                if (fk.getName().equalsIgnoreCase("ProteinGroupId"))
                    continue;
                visibleColumns.add(fk);
            }
            table.setDefaultVisibleColumns(visibleColumns);
        }

        return table;
    }

    private ExpRunTable getRunsTable()
    {
        // Start with a standard experiment run table
        ExpRunTable result = _expSchema.getRunsTable();

        result.setDescription("Contains a row per MaxQuant experiment loaded in this folder.");

        // Filter to just the runs with the Targeted MS protocol
        result.setProtocolPatterns(PROTOCOL_PATTERN_PREFIX + MqModule.IMPORT_MQ_PROTOCOL_OBJECT_PREFIX + "%");

        return result;
    }

    public static class CountColumnDisplayFactory implements DisplayColumnFactory
    {
        private final String _tableName;

        public CountColumnDisplayFactory(String tableName)
        {
            _tableName = tableName;
        }

        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new DataColumn(colInfo)
            {
                @Override
                public String renderURL(RenderContext ctx)
                {
                    ActionURL url = getQueryURL(ctx, _tableName);
                    return url.getLocalURIString();
                }
            };
        }

        private ActionURL getQueryURL(RenderContext ctx, String tableName)
        {
            Integer exptGrpId = (Integer)ctx.get(FieldKey.fromParts("Id"));

            Class<? extends Controller> actionClass = null;
            if (tableName.equals(MqSchema.TABLE_PROTEIN_GROUP))
                actionClass = MqController.ViewProteinGroupsAction.class;
            else if (tableName.equals(TABLE_PEPTIDE))
                actionClass = MqController.ViewPeptidesAction.class;

            ActionURL url = new ActionURL(actionClass, ctx.getContainer());
            url.addParameter("id", String.valueOf(exptGrpId));
            return url;
        }
    }

    public Set<String> getTableNames()
    {
        CaseInsensitiveHashSet hs = new CaseInsensitiveHashSet();
        hs.add(TABLE_EXPERIMENT_GROUP);
        hs.add(TABLE_EXPERIMENT_GROUP_DETAILS);
        hs.add(TABLE_EXPERIMENT);
        hs.add(TABLE_RAW_FILE);
        hs.add(TABLE_PROTEIN_GROUP);
        hs.add(TABLE_PROTEIN_GROUP_TMT);
        hs.add(TABLE_PROTEIN_GROUP_EXPERIMENT_INFO);
        hs.add(TABLE_PROTEIN_GROUP_RATIOS_SILAC);
        hs.add(TABLE_PROTEIN_GROUP_INTENSITY_SILAC);
        hs.add(TABLE_PEPTIDE);
        hs.add(TABLE_PEPTIDE_TMT);
        hs.add(TABLE_PROTEIN_GROUP_PEPTIDE);
        hs.add(TABLE_MODIFIED_PEPTIDE);
        hs.add(TABLE_MODIFIED_PEPTIDE_TMT);
        hs.add(TABLE_EVIDENCE);
        hs.add(TABLE_EVIDENCE_TMT);
        hs.add(TABLE_EVIDENCE_INETNSITY_SILAC);
        hs.add(TABLE_EVIDENCE_RATIO_SILAC);
        hs.add(TABLE_TMT_CHANNEL);
        return hs;
    }
}
