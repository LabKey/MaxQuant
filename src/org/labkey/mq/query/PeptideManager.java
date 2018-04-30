package org.labkey.mq.query;

import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableSelector;
import org.labkey.mq.MqManager;
import org.labkey.mq.model.Peptide;

/**
 * Created by vsharma on 3/17/2016.
 */
public class PeptideManager
{
    private PeptideManager() {}

    public static Peptide get(int id, Container c)
    {
        SimpleFilter filter = SimpleFilter.createContainerFilter(c);
        return new TableSelector(MqManager.getTableInfoPeptide(), filter, null).getObject(id, Peptide.class);
    }
}
