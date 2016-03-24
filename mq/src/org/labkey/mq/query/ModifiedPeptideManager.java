package org.labkey.mq.query;

import org.labkey.api.data.TableSelector;
import org.labkey.mq.MqManager;
import org.labkey.mq.model.ModifiedPeptide;

/**
 * Created by vsharma on 3/17/2016.
 */
public class ModifiedPeptideManager
{
    private ModifiedPeptideManager() {}

    public static ModifiedPeptide get(int id)
    {
        return new TableSelector(MqManager.getTableInfoModifiedPeptide()).getObject(id, ModifiedPeptide.class);
    }
}
