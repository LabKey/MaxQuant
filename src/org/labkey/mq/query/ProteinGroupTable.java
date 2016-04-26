package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

/**
 * Created by vsharma on 3/29/2016.
 */
public class ProteinGroupTable extends FilteredTable<MqSchema>
{
    public ProteinGroupTable(final MqSchema schema)
    {
        super(MqManager.getTableInfoProteinGroup(), schema);
        wrapAllColumns(true);

        ColumnInfo experimentGroupCol = getColumn(FieldKey.fromParts("ExperimentGroupId"));
        experimentGroupCol.setFk(new QueryForeignKey(schema, null, MqSchema.TABLE_EXPERIMENT_GROUP, "ExperimentGroup", "ExperimentGroup"));

        ColumnInfo proteinIdsColumn = getColumn(FieldKey.fromParts("ProteinIds"));
        proteinIdsColumn.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo majorityProteinIdsCol = getColumn(FieldKey.fromParts("MajorityProteinIds"));
        majorityProteinIdsCol.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo fastaHeadersCol = getColumn(FieldKey.fromParts("FastaHeaders"));
        fastaHeadersCol.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo proteinNamesCol = getColumn(FieldKey.fromParts("ProteinNames"));
        proteinNamesCol.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo geneNames = getColumn(FieldKey.fromParts("GeneNames"));
        geneNames.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo peptideCountCol = getColumn(FieldKey.fromParts("PeptideCount"));
        peptideCountCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE, "Id", "ProteinGroupId"));

        ColumnInfo intensityAndCoverageCol = addWrapColumn("IntensityAndCoverage", getRealTable().getColumn(FieldKey.fromParts("Id")));
        intensityAndCoverageCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PG_INTENSITY_COVERAGE, "Id", "ProteinGroupId", "Link"));

        ColumnInfo proteinGrpSilacRatiosCol = addWrapColumn("SilacRatios", getRealTable().getColumn(FieldKey.fromParts("Id")));
        proteinGrpSilacRatiosCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PROTEIN_GROUP_RATIOS_SILAC, "Id", "ProteinGroupId", "Link")
        {

        });
    }

    public static final class MultiLineDisplayFactory implements DisplayColumnFactory
    {
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new DataColumn(colInfo)
            {
                // The HTML encoded value
                @Override @NotNull
                public String getFormattedValue(RenderContext ctx)
                {
                    String multiValues = (String)getValue(ctx);

                    String[] values = multiValues.split(";");
                    StringBuilder sb = new StringBuilder();
                    String separator = "";
                    for(String value: values)
                    {
                        sb.append(separator);
                        sb.append(PageFlowUtil.filter(value));
                        separator = "<br>";
                    }
                    return sb.toString();
                }
            };
        }
    }
}
