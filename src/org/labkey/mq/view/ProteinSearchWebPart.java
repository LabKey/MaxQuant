package org.labkey.mq.view;

import org.labkey.api.module.ModuleLoader;
import org.labkey.api.protein.ProteinService;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.mq.MqModule;

import static org.labkey.mq.MqModule.SEARCH_WEBPART_NAME;

public class ProteinSearchWebPart extends JspView<ProteinService.ProteinSearchForm>
{
    public ProteinSearchWebPart(ProteinService.ProteinSearchForm form)
    {
        super("/org/labkey/mq/view/proteinSearch.jsp", form);
        setTitle(SEARCH_WEBPART_NAME);
    }

    public static class ProteinSearchFormViewProvider implements ProteinService.FormViewProvider<ProteinService.ProteinSearchForm>
    {
        @Override
        public WebPartView createView(ViewContext viewContext, ProteinService.ProteinSearchForm form)
        {
            if (viewContext.getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MqModule.class)))
            {
                return new ProteinSearchWebPart(form); // enable only if the maxquant module is active.
            }
            return null;
        }
    }
}
