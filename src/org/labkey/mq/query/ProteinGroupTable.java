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
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

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
        proteinIdsColumn.setDisplayColumnFactory(new UniProtLinkDisplayFactory());

        ColumnInfo majorityProteinIdsCol = getColumn(FieldKey.fromParts("MajorityProteinIds"));
        majorityProteinIdsCol.setDisplayColumnFactory(new UniProtLinkDisplayFactory());

        ColumnInfo fastaHeadersCol = getColumn(FieldKey.fromParts("FastaHeaders"));
        fastaHeadersCol.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo proteinNamesCol = getColumn(FieldKey.fromParts("ProteinNames"));
        proteinNamesCol.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo geneNames = getColumn(FieldKey.fromParts("GeneNames"));
        geneNames.setDisplayColumnFactory(new MultiLineDisplayFactory());

        ColumnInfo peptideCountCol = getColumn(FieldKey.fromParts("PeptideCount"));
        peptideCountCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE, "Id", "ProteinGroupId"));

        ColumnInfo intensityAndCoverageCol = addWrapColumn("ExperimentDetails", getRealTable().getColumn(FieldKey.fromParts("Id")));
        intensityAndCoverageCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PROTEIN_GROUP_EXPERIMENT_INFO, "Id", "ProteinGroupId", "Link"));
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

    public static final class UniProtLinkDisplayFactory implements DisplayColumnFactory
    {
        // Source http://www.uniprot.org/help/accession_numbers
        private static final Pattern UniprotAccPattern = Pattern.compile("[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}-?\\d*");
        @Override
        public DisplayColumn createRenderer(ColumnInfo colInfo)
        {
            return new DataColumn(colInfo)
            {
                private String _separator = "<br>";
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
                        value = PageFlowUtil.filter(value);
                        if(UniprotAccPattern.matcher(value).matches())
                        {
                            sb.append("<a href=\"http://www.uniprot.org/uniprot/");
                            sb.append(value);
                            sb.append("\">").append(value);
                            sb.append("</a>");
                        }
                        else
                        {
                            sb.append(value);
                        }
                        separator = _separator;
                    }
                    return sb.toString();
                }

                @Override
                public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    _separator = ", ";
                    super.renderDetailsCellContents(ctx, out);
                }
            };
        }
    }

    // Need to override this if we want to use this table in grid/details views.
    // AbstractTableInfo.hasPermission returns false, and you will see a message like
    // "You do not have permission to read this data"
    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return ReadPermission.class.equals(perm) && getContainer().hasPermission(user, perm);
    }
}
