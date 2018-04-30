package org.labkey.mq.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.mq.MqManager;
import org.labkey.mq.model.ProteinGroup;

/**
 * Created by vsharma on 3/17/2016.
 */
public class ProteinGroupManager
{
    private ProteinGroupManager() {}

    public static ProteinGroup get(int id, Container c)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        return new TableSelector(MqManager.getTableInfoProteinGroup(), filter, null).getObject(id, ProteinGroup.class);
    }

    public static boolean hasData(TableInfo table, int id, Container c)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        filter.addCondition(FieldKey.fromParts("ProteinGroupId"), id);
        return new TableSelector(table, filter, null).exists();
    }
}
