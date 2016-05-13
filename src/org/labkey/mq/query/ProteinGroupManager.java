package org.labkey.mq.query;

import org.labkey.api.data.TableSelector;
import org.labkey.mq.MqManager;
import org.labkey.mq.model.ModifiedPeptide;
import org.labkey.mq.model.ProteinGroup;

/**
 * Created by vsharma on 3/17/2016.
 */
public class ProteinGroupManager
{
    private ProteinGroupManager() {}

    public static ProteinGroup get(int id)
    {
        return new TableSelector(MqManager.getTableInfoProteinGroup()).getObject(id, ProteinGroup.class);
    }
}
