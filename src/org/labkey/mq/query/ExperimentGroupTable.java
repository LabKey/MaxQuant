package org.labkey.mq.query;

import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.view.ActionURL;
import org.labkey.mq.MqController;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.mq.MqSchema.TABLE_PEPTIDE;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP;

public class ExperimentGroupTable extends DefaultMqTable
{
    public ExperimentGroupTable(MqSchema schema)
    {
        super(MqManager.getTableInfoExperimentGroup(), schema);

        setTitleColumn("Id");
        setDescription("Contains a row per MaxQuant experiment loaded in this folder.");

        ActionURL detailsUrl = new ActionURL(MqController.ViewProteinGroupsAction.class, getContainer());
        setDetailsURL(new DetailsURL(detailsUrl, "id", FieldKey.fromParts("Id")));

        // only allow delete
        setInsertURL(AbstractTableInfo.LINK_DISABLER);
        setImportURL(AbstractTableInfo.LINK_DISABLER);
        setUpdateURL(AbstractTableInfo.LINK_DISABLER);

        ExpSchema expSchema = new ExpSchema(getUserSchema().getUser(), getContainer());
        getColumn("ExperimentRunLSID").setFk(new QueryForeignKey(expSchema, getContainer(), "Runs", "LSID", null, true));

        // ProteinGroup count column
        SQLFragment sql = new SQLFragment("(").append(getRunProteinGroupCountSQL()).append(")");
        ColumnInfo countCol = new ExprColumn(this, "ProteinGroups", sql, JdbcType.INTEGER);
        countCol.setFormat("#,###");
        countCol.setDisplayColumnFactory(new MqSchema.CountColumnDisplayFactory(TABLE_PROTEIN_GROUP));
        addColumn(countCol);

        // Peptide count
        sql = new SQLFragment("(").append(getRunPeptideCountSQL()).append(")");
        countCol = new ExprColumn(this, "Peptides", sql, JdbcType.INTEGER);
        countCol.setFormat("#,###");
        countCol.setDisplayColumnFactory(new MqSchema.CountColumnDisplayFactory(TABLE_PEPTIDE));
        addColumn(countCol);

        List<FieldKey> columns = new ArrayList<>();
        columns.add(FieldKey.fromParts("ExperimentRunLSID", "Flag"));
        columns.add(FieldKey.fromParts("ExperimentRunLSID", "Links"));
        columns.add(FieldKey.fromParts("Id"));
        columns.add(FieldKey.fromParts("Container"));
        columns.add(FieldKey.fromParts("Filename"));
        columns.add(FieldKey.fromParts("Status"));
        columns.add(FieldKey.fromParts("ProteinGroups"));
        columns.add(FieldKey.fromParts("Peptides"));
        columns.add(FieldKey.fromParts("ExperimentRunLSID", "Created"));
        columns.add(FieldKey.fromParts("ExperimentRunLSID", "CreatedBy"));
        setDefaultVisibleColumns(columns);
    }

    public static SQLFragment getRunProteinGroupCountSQL()
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT COUNT(pg.id) FROM ");
        sqlFragment.append(MqManager.getTableInfoProteinGroup(), "pg");
        sqlFragment.append(" WHERE pg.ExperimentGroupId = ");
        sqlFragment.append(ExprColumn.STR_TABLE_ALIAS + ".Id");
        return sqlFragment;
    }

    public static SQLFragment getRunPeptideCountSQL()
    {
        SQLFragment sqlFragment = new SQLFragment("SELECT COUNT(p.id) FROM ");
        sqlFragment.append(MqManager.getTableInfoPeptide(), "p");
        sqlFragment.append(" WHERE p.ExperimentGroupId = ");
        sqlFragment.append(ExprColumn.STR_TABLE_ALIAS + ".Id");
        return sqlFragment;
    }
}
