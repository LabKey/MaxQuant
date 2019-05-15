package org.labkey.mq;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.view.ViewContext;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP;

public class MqProteinSearchViewProvider implements ProteinService.QueryViewProvider<ProteinService.ProteinSearchForm>
{
    @Override
    public String getDataRegionName()
    {
        return "MqMatches";
    }

    @Nullable
    @Override
    public QueryView createView(ViewContext viewContext, ProteinService.ProteinSearchForm form, BindException errors)
    {
        if (!viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MqModule.class)))
            return null;  // only enable this view if the MqModule is active

        QuerySettings settings = new QuerySettings(viewContext, getDataRegionName(), TABLE_PROTEIN_GROUP);

        if (form.isIncludeSubfolders())
            settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

        QueryView result = new QueryView(new MqSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
        {
            @Override
            protected TableInfo createTable()
            {
//                FilteredTable<MqSchema> result = (FilteredTable<MqSchema>) super.createTable();
                FilteredTable<MqSchema> result = (FilteredTable<MqSchema>) getSchema().getTable(getSettings().getQueryName(), getContainerFilter(), true, true);
                String likeOperator = result.getSqlDialect().getCaseInsensitiveLikeOperator();

                // Apply a filter to restrict to the set of matching proteins
                SQLFragment sql = new SQLFragment("Id IN (SELECT pg.Id FROM ");
                sql.append(MqManager.getTableInfoProteinGroup(), "pg");
                sql.append(" WHERE ");
                sql.append(getProteinLabelCondition("pg.ProteinIds", getProteinLabels(form.getIdentifier()), form.isExactMatch(), likeOperator));
                sql.append(" OR ").append(getProteinLabelCondition("pg.MajorityProteinIds", getProteinLabels(form.getIdentifier()), form.isExactMatch(), likeOperator));
                sql.append(" OR ").append(getProteinLabelCondition("pg.ProteinNames", getProteinLabels(form.getIdentifier()), form.isExactMatch(), likeOperator));
                sql.append(" OR ").append(getProteinLabelCondition("pg.GeneNames", getProteinLabels(form.getIdentifier()), form.isExactMatch(), likeOperator));
                sql.append(")");
                result.addCondition(sql);

                List<FieldKey> defaultVisible = result.getDefaultVisibleColumns();
                List<FieldKey> visibleColumns = new ArrayList<>();
                visibleColumns.add(FieldKey.fromParts("ExperimentGroupId", "ExperimentGroup", "FolderName"));
                if (form.isIncludeSubfolders())
                {
                    visibleColumns.add(FieldKey.fromParts("Container"));
                }
                visibleColumns.addAll(defaultVisible);
                // TODO: Avoids mutating the TableInfo, but probably should be moved outside of new QueryView()
                settings.setFieldKeys(visibleColumns);

                return result;
            }
        };
        result.setTitle("MaxQuant Proteins");
        result.setUseQueryViewActionExportURLs(true);
        return result;
    }

    private List<String> getProteinLabels(String labels)
    {
        if(StringUtils.isBlank(labels))
            return Collections.emptyList();

        return Arrays.asList(StringUtils.split(labels, ","));
    }

    private SQLFragment getProteinLabelCondition(String columnName, List<String> labels, boolean exactMatch, String likeOperator)
    {
        SQLFragment sqlFragment = new SQLFragment();
        String separator = "";
        sqlFragment.append("(");
        if (labels.isEmpty())
        {
            sqlFragment.append("1 = 2");
        }
        for (String param : labels)
        {
            sqlFragment.append(separator);
            sqlFragment.append(columnName);
            sqlFragment.append(" ").append(likeOperator).append(" ?");
            sqlFragment.add(exactMatch ? param : "%" + param + "%");
            separator = " OR ";
        }
        sqlFragment.append(")");
        return sqlFragment;
    }
}
