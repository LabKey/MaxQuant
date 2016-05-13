package org.labkey.mq.query;

import org.labkey.api.data.TableSelector;
import org.labkey.mq.MqManager;
import org.labkey.mq.model.Peptide;
import org.labkey.mq.model.ProteinGroup;

/**
 * Created by vsharma on 3/17/2016.
 */
public class PeptideManager
{
    private PeptideManager() {}

    public static Peptide get(int id)
    {
        return new TableSelector(MqManager.getTableInfoPeptide()).getObject(id, Peptide.class);
    }
}
