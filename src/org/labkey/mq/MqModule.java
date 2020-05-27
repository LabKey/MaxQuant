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
import org.labkey.api.protein.ProteinService;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.services.ServiceRegistry;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.mq.parser.SummaryTemplateParser;
import org.labkey.mq.view.ProteinSearchWebPart;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.labkey.mq.MqSchema.TABLE_EXPERIMENT_GROUP_DETAILS;

public class MqModule extends DefaultModule
{
    public static final String NAME = "mq";
    public static final ExperimentRunType EXP_RUN_TYPE = new MqExperimentRunType();
    public static final String SEARCH_WEBPART_NAME = "MaxQuant Protein Search";
    public static final String EXPERIMENT_GROUPS_WEBPART_NAME = "Experiment Groups";
    public static final String IMPORT_MQ_PROTOCOL_OBJECT_PREFIX = "MaxQuant.Import";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 20.000;
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
            @Override
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

        ProteinService proteinService = ServiceRegistry.get().getService(ProteinService.class);
        if (proteinService != null)
        {
            proteinService.registerProteinSearchView(new MqProteinSearchViewProvider());
            proteinService.registerProteinSearchFormView(new ProteinSearchWebPart.ProteinSearchFormViewProvider());
        }
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        BaseWebPartFactory proteinSearchWebPart = new BaseWebPartFactory(SEARCH_WEBPART_NAME)
        {
            @Override
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                return new ProteinSearchWebPart(new ProteinService.ProteinSearchForm()
                {
                    @Override
                    public int[] getSeqId()
                    {
                        return new int[0];
                    }

                    @Override
                    public boolean isExactMatch()
                    {
                        return true;
                    }
                });
            }
        };

        BaseWebPartFactory expGroupsSearchWebPart = new BaseWebPartFactory(EXPERIMENT_GROUPS_WEBPART_NAME)
        {
            @Override
            public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
            {
                UserSchema schema = QueryService.get().getUserSchema(portalCtx.getUser(), portalCtx.getContainer(), MqSchema.NAME);
                if (null == schema)
                    return new HtmlView(EXPERIMENT_GROUPS_WEBPART_NAME, "Schema 'mq' could not be found.");

                BindException errors = new BindException(new Object(), "dummy");
                QuerySettings settings = new QuerySettings(portalCtx, "ExperimentGroups", TABLE_EXPERIMENT_GROUP_DETAILS);
                QueryView view = schema.createView(portalCtx, settings, errors);
                view.setTitle(EXPERIMENT_GROUPS_WEBPART_NAME);
                return view;
            }
        };

        List<WebPartFactory> webpartFactoryList = new ArrayList<>();
        webpartFactoryList.add(proteinSearchWebPart);
        webpartFactoryList.add(expGroupsSearchWebPart);
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

    @NotNull
    @Override
    public Set<Class> getUnitTests()
    {
        return Collections.singleton(SummaryTemplateParser.TestCase.class);
    }
}