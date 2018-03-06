/*
 * Copyright (c) 2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.mq;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.FolderTypeManager;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.mq.view.search.ProteinSearchWebPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MqModule extends DefaultModule
{
    public static final String NAME = "mq";
    public static final ExperimentRunType EXP_RUN_TYPE = new MqExperimentRunType();

    // Protocol prefix for importing .zip archives from Skyline
    public static final String IMPORT_MQ_PROTOCOL_OBJECT_PREFIX = "MaxQuant.Import";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return 18.11;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    protected void init()
    {
        addController(MqController.NAME, MqController.class);
        MqSchema.register(this);
    }

    @Override
    public void doStartup(ModuleContext moduleContext)
    {
        // add a container listener so we'll know when our container is deleted:
        ContainerManager.addContainerListener(new MqContainerListener());

        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MqPipelineProvider(this));

        ExperimentService.get().registerExperimentDataHandler(new MqDatahandler());

        ExperimentService.get().registerExperimentRunTypeSource(new ExperimentRunTypeSource()
        {
            @NotNull
            public Set<ExperimentRunType> getExperimentRunTypes(@Nullable Container container)
            {
                if (container == null || container.getActiveModules().contains(MqModule.this))
                {
                    return Collections.singleton(EXP_RUN_TYPE);
                }
                return Collections.emptySet();
            }
        });

        //register the MaxQuant folder type
        FolderTypeManager.get().registerFolderType(this, new MqFolderType(this));
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory proteinSearchWebPart = new BaseWebPartFactory(ProteinSearchWebPart.NAME)
        {
            @Override
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                return new ProteinSearchWebPart(new MqController.ProteinSearchForm());
            }
        };


//        BaseWebPartFactory runsFactory = new BaseWebPartFactory(TARGETED_MS_RUNS_WEBPART_NAME)
//        {
//            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
//            {
//                return new TargetedMSRunsWebPartView(portalCtx);
//            }
//        };
        List<WebPartFactory> webpartFactoryList = new ArrayList<>();
        webpartFactoryList.add(proteinSearchWebPart);
        return webpartFactoryList;
    }

    @Override
    @NotNull
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton(MqSchema.NAME);
    }
}