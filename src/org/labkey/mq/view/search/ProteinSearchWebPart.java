package org.labkey.mq.view.search;

import org.labkey.api.view.JspView;
import org.labkey.mq.MqController;

/**
 * Created by vsharma on 4/26/2016.
 */
public class ProteinSearchWebPart extends JspView<ProteinSearchBean>
{
    public static final String NAME = "MaxQuant Protein Search";

    public ProteinSearchWebPart(MqController.ProteinSearchForm form)
    {
        super("/org/labkey/mq/view/search/proteinSearch.jsp");
        setTitle(NAME);
        setModelBean(new ProteinSearchBean(form));
    }
}
