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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.mq.view.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MqController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(MqController.class);
    public static final String NAME = "mq";

    public MqController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/mq/view/hello.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    // ------------------------------------------------------------------------
    // MaxQuant results upload action
    // ------------------------------------------------------------------------
    @RequiresPermission(InsertPermission.class)
    public class MaxQuantUploadAction extends RedirectAction<MaxQuantPipelinePathForm>
    {
        public ActionURL getSuccessURL(MaxQuantPipelinePathForm form)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
        }

        public void validateCommand(MaxQuantPipelinePathForm form, Errors errors)
        {
        }

        public boolean doAction(MaxQuantPipelinePathForm form, BindException errors) throws Exception
        {
            for (File file : form.getValidatedFiles(getContainer()))
            {
                if (!file.isFile())
                {
                    throw new NotFoundException("Expected a file but found a directory: " + file.getName());
                }

                ViewBackgroundInfo info = getViewBackgroundInfo();
                try
                {
                    MqManager.addRunToQueue(info, file, form.getPipeRoot(getContainer()));
                }
                catch (IOException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
                catch (SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }
    }

    public static class MaxQuantPipelinePathForm extends PipelinePathForm
    {
        @Override
        public List<File> getValidatedFiles(Container c)
        {
            List<File> files = super.getValidatedFiles(c);
            List<File> resolvedFiles = new ArrayList<>(files.size());
            for(File file: files)
            {
                resolvedFiles.add(FileUtil.resolveFile(file));  // Strips out ".." and "." from the path
            }
            return resolvedFiles;
        }
    }

    @RequiresPermission(ReadPermission.class)
    public class ProteinSearchAction extends QueryViewAction<ProteinSearchForm, QueryView>
    {
        public ProteinSearchAction()
        {
            super(ProteinSearchForm.class);
        }

        @Override
        protected QueryView createQueryView(ProteinSearchForm form, BindException errors, boolean forExport, String dataRegion) throws Exception
        {
            return createProteinSearchView(form, errors);
        }

        @Override
        protected ModelAndView getHtmlView(final ProteinSearchForm form, BindException errors) throws Exception
        {
            VBox result = new VBox(new ProteinSearchWebPart(form));

            if (form.isPopulated())
                result.addView(createProteinSearchView(form, errors));

            return result;
        }

        private QueryView createProteinSearchView(final ProteinSearchForm form, BindException errors)
        {
            if (! getContainer().getActiveModules().contains(ModuleLoader.getInstance().getModule(MqModule.class)))
                return null;

            ViewContext viewContext = getViewContext();
            QuerySettings settings = new QuerySettings(viewContext, "MqMatches", "ProteinGroup");

            if (form.isIncludeSubfolders())
                settings.setContainerFilterName(ContainerFilter.Type.CurrentAndSubfolders.name());

            QueryView result = new QueryView(new MqSchema(viewContext.getUser(), viewContext.getContainer()), settings, errors)
            {
                @Override
                protected TableInfo createTable()
                {
                    FilteredTable<MqSchema> result = (FilteredTable<MqSchema>) super.createTable();

                    SimpleFilter filter = new SimpleFilter();

                    if (!StringUtils.isBlank(form.getProteinId()))
                    {
                        // filter.addClause(new SimpleFilter.OrClause(new CompareType.CompareClause(FieldKey.fromString("ProteinIds"), CompareType.CONTAINS, form.getProteinId())));
                        filter.addCondition(FieldKey.fromString("ProteinIds"), form.getProteinId(), CompareType.CONTAINS);
                    }
                    if (!StringUtils.isBlank(form.getMajorityProteinId()))
                    {
                        filter.addCondition(FieldKey.fromString("MajorityProteinIds"), form.getMajorityProteinId(), CompareType.CONTAINS);
                    }
                    if (!StringUtils.isBlank(form.getProteinName()))
                    {
                        filter.addCondition(FieldKey.fromString("ProteinNames"), form.getProteinName(), CompareType.CONTAINS);
                    }
                    if (!StringUtils.isBlank(form.getGeneName()))
                    {
                        filter.addCondition(FieldKey.fromString("GeneNames"), form.getGeneName(), CompareType.CONTAINS);
                    }

                    result.addCondition(filter);

                    List<FieldKey> defaultVisible = result.getDefaultVisibleColumns();
                    List<FieldKey> visibleColumns = new ArrayList<>();
                    visibleColumns.add(FieldKey.fromParts("ExperimentGroupId", "ExperimentGroup", "FolderName"));
                    if (form.isIncludeSubfolders())
                    {
                        visibleColumns.add(FieldKey.fromParts("Container"));
                    }
                    visibleColumns.addAll(defaultVisible);
                    result.setDefaultVisibleColumns(visibleColumns);
                    return result;
                }
            };
            result.setTitle("MaxQuant Proteins");
            result.setUseQueryViewActionExportURLs(true);
            return result;
        }

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Modification Search Results");
        }
    }

    public static class ProteinSearchForm extends QueryViewAction.QueryExportForm implements HasViewContext
    {
        private ViewContext _context;
        private String _proteinId;
        private String _majorityProteinId;
        private String _proteinName;
        private String _geneName;
        private boolean _includeSubfolders;

        public ViewContext getContext()
        {
            return _context;
        }

        public void setContext(ViewContext context)
        {
            _context = context;
        }

        public String getProteinId()
        {
            return _proteinId;
        }

        public void setProteinId(String proteinId)
        {
            _proteinId = proteinId;
        }

        public String getMajorityProteinId()
        {
            return _majorityProteinId;
        }

        public void setMajorityProteinId(String majorityProteinId)
        {
            _majorityProteinId = majorityProteinId;
        }

        public String getProteinName()
        {
            return _proteinName;
        }

        public void setProteinName(String proteinName)
        {
            _proteinName = proteinName;
        }

        public String getGeneName()
        {
            return _geneName;
        }

        public void setGeneName(String geneName)
        {
            _geneName = geneName;
        }

        public boolean isIncludeSubfolders()
        {
            return _includeSubfolders;
        }

        public void setIncludeSubfolders(boolean includeSubfolders)
        {
            _includeSubfolders = includeSubfolders;
        }

        public boolean isPopulated()
        {
            return !StringUtils.isBlank(_proteinId)
                    || !StringUtils.isBlank(_majorityProteinId)
                    || !StringUtils.isBlank(_proteinName)
                    || !StringUtils.isBlank(_geneName);
        }
    }
    // ------------------------------------------------------------------------
    // View protein groups in an experiment group
    // ------------------------------------------------------------------------
//    public class ViewProteinGroupsAction extends QueryViewAction<ExperimentIdForm>
//    {
//        @Override
//        public ModelAndView getView(Object o, BindException errors) throws Exception
//        {
//            return null;
//        }
//
//        @Override
//        public NavTree appendNavTrail(NavTree root)
//        {
//            return null;
//        }
//
//        @Override
//        protected QueryView createQueryView(QueryExportForm form, BindException errors, boolean forExport, @Nullable String dataRegion) throws Exception
//        {
//            ViewContext viewContext = getViewContext();
//            QuerySettings settings = new QuerySettings(viewContext, "TargetedMSMatches", "Precursor");
//
//            QueryView view = new QueryView(QueryService.get().getUserSchema(getUser(), getContainer(), MqSchema.NAME));
//            view.
//            return view;
//        }
//    }

    public static class ExperimentIdForm
    {
        private int _experimentGroupId;

        public int getExperimentGroupId()
        {
            return _experimentGroupId;
        }

        public void setExperimentGroupId(int experimentGroupId)
        {
            _experimentGroupId = experimentGroupId;
        }
    }

}