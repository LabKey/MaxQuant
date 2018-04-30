package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.DefaultQueryUpdateService;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.view.ActionURL;
import org.labkey.mq.MqController;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.io.File;
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

        // add explicit delete url to allow for deletion of a partially imported results set (i.e. failed pipeline job)
        ActionURL deleteUrl = new ActionURL(MqController.DeleteExperimentGroupsAction.class, getContainer());
        setDeleteURL(new DetailsURL(deleteUrl));

        ExpSchema expSchema = new ExpSchema(getUserSchema().getUser(), getContainer());
        getColumn("ExperimentRunLSID").setFk(new QueryForeignKey(expSchema, getContainer(), "Runs", "LSID", null, true));
        getColumn("DataId").setFk(new QueryForeignKey(expSchema, getContainer(), "Data", "RowId", null));

        ColumnInfo folderName = addWrapColumn("ParentFolderName", getRealTable().getColumn("LocationOnFileSystem"));
        folderName.setDisplayColumnFactory(colInfo -> new DataColumn(colInfo)
        {
            @Override
            @NotNull
            public String getFormattedValue(RenderContext ctx)
            {
                String result = h(getValue(ctx));
                return new File(result).getParentFile().getName();
            }
        });

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
        columns.add(FieldKey.fromParts("ParentFolderName"));
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

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        return perm.equals(DeletePermission.class) && getContainer().hasPermission(user, perm);
    }

    @Override
    public @Nullable QueryUpdateService getUpdateService()
    {
        return new DefaultQueryUpdateService(this, getRealTable());
    }
}
