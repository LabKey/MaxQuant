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

import com.drew.lang.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.exp.ExperimentRunType;
import org.labkey.api.exp.ExperimentRunTypeSource;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.view.WebPartFactory;

import java.util.Collection;
import java.util.Collections;
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
        return 15.21;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
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