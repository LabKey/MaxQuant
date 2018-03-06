package org.labkey.mq;

import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ViewContext;

import java.io.File;

/**
 * Created by vsharma on 2/5/2016.
 */
public class MqPipelineProvider extends PipelineProvider
{
    static String name = "MaxQuant";
    public static String FILE_NAME = "summary.txt";

    public MqPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(MqController.MaxQuantUploadAction.class, "Import MaxQuant Results");
        addAction(actionId, MqController.MaxQuantUploadAction.class, "Import MaxQuant Results",
                directory, directory.listFiles(new UploadFileFilter()), true, false, includeAll);
    }

    public static class UploadFileFilter extends FileEntryFilter
    {
        public boolean accept(File file)
        {
            return file.getName().equalsIgnoreCase(FILE_NAME);
        }
    }
}
