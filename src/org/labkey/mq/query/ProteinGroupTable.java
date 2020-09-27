package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.Link;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.mq.MqController;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

/**
 * Created by vsharma on 3/29/2016.
 */
public class ProteinGroupTable extends DefaultMqTable
{
    public ProteinGroupTable(MqSchema schema, ContainerFilter cf)
    {
        super(MqManager.getTableInfoProteinGroup(), schema, cf);

        getMutableColumn("ExperimentGroupId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_EXPERIMENT_GROUP, "ExperimentGroup", "ExperimentGroup"));

        getMutableColumn("ProteinIds").setDisplayColumnFactory(new UniProtLinkDisplayFactory());
        getMutableColumn("MajorityProteinIds").setDisplayColumnFactory(new UniProtLinkDisplayFactory());
        getMutableColumn("FastaHeaders").setDisplayColumnFactory(new MultiLineDisplayFactory());
        getMutableColumn("ProteinNames").setDisplayColumnFactory(new MultiLineDisplayFactory());
        getMutableColumn("GeneNames").setDisplayColumnFactory(new MultiLineDisplayFactory());
        getMutableColumn("PeptideCount").setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE, "Id", "ProteinGroupId"));

        ActionURL detailsUrl = new ActionURL(MqController.ViewProteinGroupInfoAction.class, getContainer());
        setDetailsURL(new DetailsURL(detailsUrl, "id", FieldKey.fromParts("Id")));
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
                public HtmlString getFormattedHtml(RenderContext ctx)
                {
                    String multiValues = (String)getValue(ctx);

                    String[] values = multiValues.split(";");
                    HtmlStringBuilder sb = HtmlStringBuilder.of();
                    HtmlString separator = HtmlString.EMPTY_STRING;
                    for(String value: values)
                    {
                        sb.append(separator);
                        sb.append(value);
                        separator = HtmlString.unsafe("<br>");
                    }
                    return sb.getHtmlString();
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
                private HtmlString _separator = HtmlString.unsafe("<br>");
                // The HTML encoded value
                @Override @NotNull
                public HtmlString getFormattedHtml(RenderContext ctx)
                {
                    String multiValues = (String)getValue(ctx);

                    String[] values = multiValues.split(";");
                    HtmlStringBuilder sb = HtmlStringBuilder.of();
                    HtmlString separator = HtmlString.EMPTY_STRING;
                    for(String value: values)
                    {
                        sb.append(separator);
                        if(UniprotAccPattern.matcher(value).matches())
                        {
                            sb.append(new Link.LinkBuilder(value).clearClasses().href("http://www.uniprot.org/uniprot/" + value));
                        }
                        else
                        {
                            sb.append(value);
                        }
                        separator = _separator;
                    }
                    return sb.getHtmlString();
                }

                @Override
                public void renderDetailsCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    _separator = HtmlString.of(", ");
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
