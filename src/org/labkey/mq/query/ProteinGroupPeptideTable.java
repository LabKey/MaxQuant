package org.labkey.mq.query;

import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.util.ArrayList;

/**
 * Created by vsharma on 3/29/2016.
 */
public class ProteinGroupPeptideTable extends FilteredTable<MqSchema>
{
    private static final FieldKey CONTAINER_FAKE_COLUMN_NAME = FieldKey.fromParts("Container");

    public ProteinGroupPeptideTable(MqSchema schema, ContainerFilter cf)
    {
        super(MqManager.getTableInfoProteinGroupPeptide(), schema, cf);
        wrapAllColumns(true);

        getMutableColumn("ProteinGroupId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_PROTEIN_GROUP, "Id", "ProteinIds"));
        getMutableColumn("PeptideId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_PEPTIDE, "Id", "Sequence"));

        SQLFragment sql = new SQLFragment("(").append("SELECT COUNT(e.Id) FROM ");
        sql.append(MqManager.getTableInfoEvidence(), "e");
        sql.append(" WHERE e.PeptideId=");
        sql.append(ExprColumn.STR_TABLE_ALIAS + ".PeptideId").append(")");
        BaseColumnInfo countCol = new ExprColumn(this, "EvidenceCount", sql, JdbcType.INTEGER);
        countCol.setFormat("#,###");
        countCol.setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_EVIDENCE, "PeptideId", "PeptideId"));
        addColumn(countCol);

        //Only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();

        // TODO: The display field in the ProteinGroupId Query FK is set to the ProteinIds column. And that column
        // has a display factory assigned to it.  But the display factory is not getting used.
        // So I have to explicitly add the ProteinGroupId/ProteinIds column to the visible columns list.
        // visibleColumns.add(FieldKey.fromParts("ProteinGroupId", "ProteinIds"));
        visibleColumns.add(FieldKey.fromParts("PeptideId"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Length"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "Mass"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "StartPosition"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "EndPosition"));
        visibleColumns.add(FieldKey.fromParts("PeptideId", "MissedCleavages"));
        visibleColumns.add(FieldKey.fromParts("EvidenceCount"));

        setDefaultVisibleColumns(visibleColumns);
    }

    @Override
    protected void applyContainerFilter(ContainerFilter filter)
    {
        clearConditions(CONTAINER_FAKE_COLUMN_NAME);
        addCondition(createContainerFilterSQL(filter, _userSchema.getContainer()), CONTAINER_FAKE_COLUMN_NAME);
    }

    private SQLFragment createContainerFilterSQL(ContainerFilter filter, Container container)
    {
        SQLFragment sql = new SQLFragment("ProteinGroupId IN (SELECT Id FROM ");
        sql.append(MqManager.getTableInfoProteinGroup(), "pg");
        sql.append(" WHERE ");
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("Container"), container));
        sql.append(")");
        return sql;
    }
}
