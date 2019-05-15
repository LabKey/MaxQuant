package org.labkey.mq.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.BaseColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.mq.MqManager;
import org.labkey.mq.MqSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vsharma on 3/29/2016.
 */
public class EvidenceTable extends DefaultMqTable
{
    public EvidenceTable(MqSchema schema, ContainerFilter cf)
    {
        super(MqManager.getTableInfoEvidence(), schema, cf);

        getMutableColumn("ExperimentId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_EXPERIMENT, "Id", "ExperimentName"));
        getMutableColumn("RawFileId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_RAW_FILE, "Id", "Name"));
        getMutableColumn("PeptideId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_PEPTIDE, "Id", "Sequence"));
        getMutableColumn("ModifiedPeptideId").setFk(QueryForeignKey.from(schema, cf).to(MqSchema.TABLE_MODIFIED_PEPTIDE, "Id", "Sequence"));

        getMutableColumn("PeptideId").setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_PEPTIDE, "PeptideId", "Id"));
        getMutableColumn("ModifiedPeptideId").setDisplayColumnFactory(new QueryLinkDisplayColumnFactory(MqSchema.TABLE_MODIFIED_PEPTIDE, "ModifiedPeptideId", "Id"));

        addColumn(makeSilacIntensityColumn('H'));
        addColumn(makeSilacIntensityColumn('M'));
        addColumn(makeSilacIntensityColumn('L'));
        addColumn(makeSilacRatioColumn("H/L", "Ratio"));
        addColumn(makeSilacRatioColumn("H/L", "RatioNormalized"));
        addColumn(makeSilacRatioColumn("H/M", "Ratio"));
        addColumn(makeSilacRatioColumn("H/M", "RatioNormalized"));
        addColumn(makeSilacRatioColumn("M/L", "Ratio"));
        addColumn(makeSilacRatioColumn("M/L", "RatioNormalized"));

        //only display a subset of the columns by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>();
        List<FieldKey> defaultVisible = getDefaultVisibleColumns();
        for (FieldKey fk: defaultVisible)
        {
            if (fk.getName().equalsIgnoreCase("Container"))
                continue;

            visibleColumns.add(fk);
            if (fk.getName().equalsIgnoreCase("ModifiedPeptideId"))
                visibleColumns.add(FieldKey.fromParts("ModifiedPeptideId", "Modifications"));
        }
        setDefaultVisibleColumns(visibleColumns);
    }

    @NotNull
    private BaseColumnInfo makeSilacIntensityColumn(char labelType)
    {
        SQLFragment sql = new SQLFragment("(").append("SELECT Intensity FROM ");
        sql.append(MqManager.getTableInfoEvidenceIntensitySilac(), "eis");
        sql.append(" WHERE eis.EvidenceId=");
        sql.append(ExprColumn.STR_TABLE_ALIAS + ".Id");
        sql.append(" AND eis.LabelType=?");
        sql.add(labelType);
        sql.append(")");

        BaseColumnInfo intensityHCol = new ExprColumn(this, "Intensity " + labelType, sql, JdbcType.INTEGER);
        intensityHCol.setFormat("#,###,###,###");
        return intensityHCol;
    }

    @NotNull
    private BaseColumnInfo makeSilacRatioColumn(String ratioType, String columnName)
    {
        SQLFragment sql = new SQLFragment("(").append("SELECT ").append(columnName).append(" FROM ");
        sql.append(MqManager.getTableInfoEvidenceRatioSilac(), "ers");
        sql.append(" WHERE ers.EvidenceId=");
        sql.append(ExprColumn.STR_TABLE_ALIAS + ".Id");
        sql.append(" AND ers.RatioType=?");
        sql.add(ratioType);
        sql.append(")");

        BaseColumnInfo ratioColumn = new ExprColumn(this, columnName + " " + ratioType, sql, JdbcType.DOUBLE);
        ratioColumn.setFormat("0.0000");
        return ratioColumn;
    }
}
