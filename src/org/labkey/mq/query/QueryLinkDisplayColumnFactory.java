package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.view.ActionURL;
import org.labkey.mq.MqController;
import org.labkey.mq.MqSchema;

import java.util.Set;

/**
 * Created by vsharma on 4/22/2016.
 */
public class QueryLinkDisplayColumnFactory implements DisplayColumnFactory
{

    private final String _tableName;
    private final String fkColumnName;
    private final String _valueColumnName;
    private final String _displayValue;

    public QueryLinkDisplayColumnFactory(String tableName, String valueColumn, String fkColumnName)
    {
        this(tableName, valueColumn, fkColumnName, null);
    }

    public QueryLinkDisplayColumnFactory(String tableName, String valueColumn, String fkColumnName, String displayValue)
    {
        _tableName = tableName;
        _valueColumnName = valueColumn;
        this.fkColumnName = fkColumnName;
        _displayValue = displayValue;
    }

    @Override
    public DisplayColumn createRenderer(ColumnInfo colInfo)
    {
        return new DataColumn(colInfo)
        {
            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(FieldKey.fromParts(_valueColumnName));// Make sure this column is always in the result set.
            }

            @Override
            public String renderURL(RenderContext ctx)
            {
                String fkValue = String.valueOf(ctx.get(_valueColumnName));
                ActionURL url;
                if(_tableName.equalsIgnoreCase(MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE))
                {
                    url = new ActionURL(MqController.ViewProteinPeptidesAction.class, ctx.getContainer());
                    url.addParameter("id", fkValue);
                }
                else if(_tableName.equalsIgnoreCase(MqSchema.TABLE_EVIDENCE))
                {
                    url = new ActionURL(MqController.ViewPeptideEvidenceAction.class, ctx.getContainer());
                    url.addParameter("id", fkValue);
                }
                else if(_tableName.equalsIgnoreCase(MqSchema.TABLE_PROTEIN_GROUP_EXPERIMENT_INFO)
                        || _tableName.equalsIgnoreCase(MqSchema.TABLE_PROTEIN_GROUP_RATIOS_SILAC)
                        || _tableName.equalsIgnoreCase(MqSchema.TABLE_EVIDENCE_INETNSITY_SILAC))
                {
                    url = new ActionURL(MqController.ViewProteinGroupInfoAction.class, ctx.getContainer());
                    url.addParameter("id", fkValue);
                }
                else
                {
                    url = QueryService.get().urlDefault(ctx.getContainer(), QueryAction.executeQuery, MqSchema.NAME, _tableName);
                    url.addParameter("query." + fkColumnName + "~eq", fkValue);
                }
                return url.getLocalURIString();
            }

            @NotNull
            @Override
            public String getFormattedValue(RenderContext ctx)
            {
                return _displayValue == null ? super.getFormattedValue(ctx) : _displayValue;
            }
        };
    }
}
