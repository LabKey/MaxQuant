package org.labkey.mq.view.search;

import org.labkey.mq.MqController;

/**
 * Created by vsharma on 4/26/2016.
 */
public class ProteinSearchBean
{
    private final MqController.ProteinSearchForm _form;

    public ProteinSearchBean(MqController.ProteinSearchForm form)
    {
        _form = form;
    }

    public MqController.ProteinSearchForm getForm()
    {
        return _form;
    }
}
