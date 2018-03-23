package org.labkey.mq;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineProvider;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ViewContext;
import org.labkey.mq.parser.SummaryTemplateParser;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Created by vsharma on 2/5/2016.
 */
public class MqPipelineProvider extends PipelineProvider
{
    public static final String NAME = "MaxQuant";
    public static final String ACTION_LABEL = "Import MaxQuant Results";

    public MqPipelineProvider(Module owningModule)
    {
        super(NAME, owningModule);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        String actionId = createActionId(MqController.MaxQuantUploadAction.class, ACTION_LABEL);
        return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(MqController.MaxQuantUploadAction.class, ACTION_LABEL);
        addAction(actionId, MqController.MaxQuantUploadAction.class, ACTION_LABEL,
            directory, directory.listFiles(new UploadFileFilter()), true, false, includeAll);
    }

    public static class UploadFileFilter extends FileEntryFilter
    {
        public boolean accept(File file)
        {
            return file.getName().equalsIgnoreCase(SummaryTemplateParser.FILE);
        }
    }
}
