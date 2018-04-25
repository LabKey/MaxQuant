/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.labkey.api.module.MultiPortalFolderType;
import org.labkey.api.util.HelpTopic;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;

import java.util.Arrays;
import java.util.Collections;

import static org.labkey.mq.MqModule.EXPERIMENT_GROUPS_WEBPART_NAME;
import static org.labkey.mq.MqModule.SEARCH_WEBPART_NAME;

/**
 * User: vsharma
 * Date: 4/1/12
 * Time: 10:58 AM
 */
public class MqFolderType extends MultiPortalFolderType
{
    public static final String NAME = "MaxQuant";

    public MqFolderType(MqModule module)
    {
        super(NAME,
                "Manage results from MaxQuant runs.",
            Collections.<Portal.WebPart>emptyList(),
            Arrays.asList(
                    Portal.getPortalPart(SEARCH_WEBPART_NAME).createWebPart(),
                    Portal.getPortalPart(EXPERIMENT_GROUPS_WEBPART_NAME).createWebPart(),
                    Portal.getPortalPart("Data Pipeline").createWebPart()
            ),
            getDefaultModuleSet(module, getModule("mq"), getModule("Pipeline"), getModule("Experiment")),
            module);
    }

    @Override
    public HelpTopic getHelpTopic()
    {
        return new HelpTopic("MaxQuant");
    }

    @Override
    public String getLabel()
    {
        return "MaxQuant";
    }

     @Override
    public String getStartPageLabel(ViewContext ctx)
    {
        return "MaxQuant Dashboard";
    }
}
