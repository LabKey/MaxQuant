package org.labkey.mq.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.util.ArrayList;

/**
 * Created by vsharma on 3/29/2016.
 */
public class PeptideTable extends DefaultMqTable
{
    public PeptideTable(MqSchema schema)
    {
        super(MqManager.getTableInfoPeptide(), schema);

        getColumn("ExperimentGroupId").setFk(new QueryForeignKey(schema, null, MqSchema.TABLE_EXPERIMENT_GROUP, "ExperimentGroup", "ExperimentGroup"));

        SQLFragment sql = new SQLFragment("(").append("SELECT COUNT(e.Id) FROM ");
        sql.append(MqManager.getTableInfoEvidence(), "e");
        sql.append(" WHERE e.PeptideId=");
        sql.append(ExprColumn.STR_TABLE_ALIAS + ".Id").append(")");
        ColumnInfo countCol = new ExprColumn(this, "EvidenceCount", sql, JdbcType.INTEGER);
        countCol.setFormat("#,###");
        countCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_EVIDENCE, "Id", "PeptideId"));
        addColumn(countCol);

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        visibleColumns.add(FieldKey.fromParts("ExperimentGroupId"));
        visibleColumns.add(FieldKey.fromParts("Id"));
        visibleColumns.add(FieldKey.fromParts("Sequence"));
        visibleColumns.add(FieldKey.fromParts("Length"));
        visibleColumns.add(FieldKey.fromParts("Mass"));
        visibleColumns.add(FieldKey.fromParts("StartPosition"));
        visibleColumns.add(FieldKey.fromParts("EndPosition"));
        visibleColumns.add(FieldKey.fromParts("MissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("EvidenceCount"));
        setDefaultVisibleColumns(visibleColumns);
    }
}
