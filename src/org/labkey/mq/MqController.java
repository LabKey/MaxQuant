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
import org.jetbrains.annotations.NotNull;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.action.QueryViewAction;
import org.labkey.api.action.RedirectAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.Peptide;
import org.labkey.mq.model.ProteinGroup;
import org.labkey.mq.parser.EvidenceParser;
import org.labkey.mq.parser.SummaryTemplateParser;
import org.labkey.mq.parser.ModifiedPeptidesParser;
import org.labkey.mq.parser.PeptidesParser;
import org.labkey.mq.parser.ProteinGroupsParser;
import org.labkey.mq.query.PeptideManager;
import org.labkey.mq.query.ProteinGroupManager;
import org.labkey.mq.view.search.ProteinSearchWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.labkey.mq.MqSchema.TABLE_PEPTIDE;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP;

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

    // ------------------------------------------------------------------------
    // Action to download MaxQuant files
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class DownloadFileAction extends SimpleViewAction<DownloadFileForm>
    {
        public ModelAndView getView(DownloadFileForm form, BindException errors) throws Exception
        {
            if (form.getExperimentGroupId() < 0)
            {
                throw new NotFoundException("No experimentGroup ID specified.");
            }
            if(StringUtils.isBlank(form.getFileName()))
            {
                throw new NotFoundException("No file name specified.");
            }

            ExperimentGroup expGrp = MqManager.getExperimentGroup(form.getExperimentGroupId());
            if(expGrp == null)
            {
                throw new NotFoundException("Could not find an experiment group with ID " + form.getExperimentGroupId());
            }
            if(!expGrp.getContainer().equals(getContainer()))
            {
                ActionURL url = getViewContext().cloneActionURL();
                url.setContainer(expGrp.getContainer());
                throw new RedirectException(url);
            }

            ExpRun expRun = ExperimentService.get().getExpRun(expGrp.getExperimentRunLSID());
            if (expRun == null)
            {
                throw new NotFoundException("Experiment " + expGrp.getExperimentRunLSID() + " does not exist in the exp schema.");
            }

            List<? extends ExpData> inputDatas = expRun.getAllDataUsedByRun();
            if(inputDatas == null || inputDatas.isEmpty())
            {
                throw new NotFoundException("No input data found for experiment group "+expRun.getRowId());
            }
            // The first file will be experimentalDesignTemplate.txt
            File file = expRun.getAllDataUsedByRun().get(0).getFile();
            if (file == null)
            {
                throw new NotFoundException("No data files found for experiment group " + expGrp.getId());
            }

            File experimentDir = file.getParentFile();
            if(!NetworkDrive.exists(experimentDir))
            {
                throw new NotFoundException("Experiment directory " + experimentDir + " does not exist.");
            }

            if(form.getFileName().equals(SummaryTemplateParser.FILE)
                    && file.getName().equals(SummaryTemplateParser.FILE))
            {
                PageFlowUtil.streamFile(getViewContext().getResponse(), file, true);
                return null;
            }

            File txtDir = new File(experimentDir, "txt");
            if(!NetworkDrive.exists(txtDir))
            {
                throw new NotFoundException("Sub-directory " + txtDir + " does not exist.");
            }

            file = new File(txtDir, form.getFileName());
            if(!file.exists())
            {
                throw new NotFoundException("File " + file.getName() + " not found in directory " + txtDir.getPath());
            }

            PageFlowUtil.streamFile(getViewContext().getResponse(), file, true);
            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    public static class DownloadFileForm
    {
        private int _experimentGroupId;
        private String _fileName;

        public int getExperimentGroupId()
        {
            return _experimentGroupId;
        }

        public void setExperimentGroupId(int experimentGroupId)
        {
            _experimentGroupId = experimentGroupId;
        }

        public String getFileName()
        {
            return _fileName;
        }

        public void setFileName(String fileName)
        {
            _fileName = fileName;
        }
    }

    // ------------------------------------------------------------------------
    // Protein search action
    // ------------------------------------------------------------------------
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
    @RequiresPermission(ReadPermission.class)
    public class ViewProteinGroupsAction extends SimpleViewAction<IdForm>
    {
        private ExperimentGroup _experimentGroup;

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (_experimentGroup != null)
            {
                root.addChild("MaxQuant Runs", getShowRunsUrl());
                root.addChild("Protein Groups for Experiment Group " + _experimentGroup.getId());
            }
            return root;
        }

        @Override
        public void validate(IdForm form, BindException errors)
        {
            _experimentGroup = MqManager.getExperimentGroup(form.getId());
            if (_experimentGroup == null)
                errors.reject(ERROR_MSG, "Experiment group with ID " + form.getId() + " does not exist.");
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // Files listing
            HtmlView fileDownloadView = getDownloadLinksView(form.getId());
            fileDownloadView.setTitle("Files");
            fileDownloadView.setFrame(WebPartView.FrameType.PORTAL);

            // Details of the Experiment group
            JspView exptDetailsView = getExperimentGroupDetailsView(_experimentGroup);
            VBox detailsBox = new VBox(exptDetailsView, fileDownloadView);
            detailsBox.setTitle("Experiment Group Details");
            detailsBox.setFrame(WebPartView.FrameType.PORTAL);

            // ProteinGroups table
            QuerySettings settings = new QuerySettings(getViewContext(), "ProteinGroups", "ProteinGroup");
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ExperimentGroupId"), form.getId()));
            QueryView proteinGrpsGridView = new QueryView(new MqSchema(getUser(), getContainer()), settings, errors);
            proteinGrpsGridView.setTitle("Protein Groups");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(proteinGrpsGridView);
            return view;
        }
    }

    // ------------------------------------------------------------------------
    // View peptides in an experiment group
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ViewPeptidesAction extends SimpleViewAction<IdForm>
    {
        private ExperimentGroup _experimentGroup;

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (_experimentGroup != null)
            {
                root.addChild("MaxQuant Runs", getShowRunsUrl());
                root.addChild("Experiment Protein Groups", getProteinGroupsUrl(_experimentGroup.getId()));
                root.addChild("Peptides for Experiment Group " + _experimentGroup.getId());
            }
            return root;
        }

        @Override
        public void validate(IdForm form, BindException errors)
        {
            _experimentGroup = MqManager.getExperimentGroup(form.getId());
            if (_experimentGroup == null)
                errors.reject(ERROR_MSG, "Experiment group with ID " + form.getId() + " does not exist.");
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // Files listing
            HtmlView fileDownloadView = getDownloadLinksView(form.getId());
            fileDownloadView.setTitle("Files");
            fileDownloadView.setFrame(WebPartView.FrameType.PORTAL);

            // Details of the Experiment group
            JspView exptDetailsView = getExperimentGroupDetailsView(_experimentGroup);
            VBox detailsBox = new VBox(exptDetailsView, fileDownloadView);
            detailsBox.setTitle("Experiment Group Details");
            detailsBox.setFrame(WebPartView.FrameType.PORTAL);

            // Peptides table
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", "Peptide");
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ExperimentGroupId"), form.getId()));
            QueryView proteinGrpsGridView = new QueryView(new MqSchema(getUser(), getContainer()), settings, errors);
            proteinGrpsGridView.setTitle("Peptides");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(proteinGrpsGridView);
            return view;
        }
    }

    // ------------------------------------------------------------------------
    // View peptides in a protein group
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ViewProteinPeptidesAction extends SimpleViewAction<IdForm>
    {
        private int _experimentGroupId;
        private ProteinGroup _proteinGroup;

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (_proteinGroup != null)
            {
                root.addChild("MaxQuant Runs", getShowRunsUrl());
                root.addChild("Experiment Protein Groups", getProteinGroupsUrl(_experimentGroupId));
                root.addChild("Peptides for Protein Group " + _proteinGroup.getId());
            }
            return root;
        }

        @Override
        public void validate(IdForm form, BindException errors)
        {
            _proteinGroup = ProteinGroupManager.get(form.getId());
            if (_proteinGroup == null)
                errors.reject(ERROR_MSG, "Protein group with ID " + form.getId() + " does not exist.");
            else
                _experimentGroupId = _proteinGroup.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // Files listing
            HtmlView fileDownloadView = getDownloadLinksView(_experimentGroupId, PeptidesParser.FILE);
            fileDownloadView.setTitle("Files");
            fileDownloadView.setFrame(WebPartView.FrameType.PORTAL);

            // Details of the  ProteinGroup
            DetailsView exptDetailsView = getProteinGroupDetailsView(form.getId());
            VBox detailsBox = new VBox(exptDetailsView, fileDownloadView);
            detailsBox.setTitle("Protein Group Details");
            detailsBox.setFrame(WebPartView.FrameType.PORTAL);

            // ProteinGroupPeptide table
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", "ProteinGroupPeptide");
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ProteinGroupId"), form.getId()));
            QueryView peptidesGridView = new QueryView(new MqSchema(getUser(), getContainer()), settings, errors);
            peptidesGridView.setTitle("Peptides");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(peptidesGridView);
            return view;
        }
    }

    // ------------------------------------------------------------------------
    // View details for a protein group
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ViewProteinGroupInfoAction extends SimpleViewAction<IdForm>
    {
        private int _experimentGroupId;
        private ProteinGroup _proteinGroup;

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (_proteinGroup != null)
            {
                root.addChild("MaxQuant Runs", getShowRunsUrl());
                root.addChild("Experiment Protein Groups", getProteinGroupsUrl(_experimentGroupId));
                root.addChild("Experiment Details for Protein Group " + _proteinGroup.getId());
            }
            return root;
        }

        @Override
        public void validate(IdForm form, BindException errors)
        {
            _proteinGroup = ProteinGroupManager.get(form.getId());
            if (_proteinGroup == null)
                errors.reject(ERROR_MSG, "Protein group with ID " + form.getId() + " does not exist.");
            else
                _experimentGroupId = _proteinGroup.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // Files listing
            HtmlView fileDownloadView = getDownloadLinksView(_experimentGroupId, ProteinGroupsParser.FILE);
            fileDownloadView.setTitle("Files");
            fileDownloadView.setFrame(WebPartView.FrameType.PORTAL);

            // Details of the  ProteinGroup
            DetailsView exptDetailsView = getProteinGroupDetailsView(form.getId());
            VBox detailsBox = new VBox(exptDetailsView, fileDownloadView);
            detailsBox.setTitle("Protein Group Details");
            detailsBox.setFrame(WebPartView.FrameType.PORTAL);

            // ProteinGroupExperimentInfo table
            QuerySettings s1 = new QuerySettings(getViewContext(), "IntensityAndCoverage", "ProteinGroupExperimentInfo");
            s1.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ProteinGroupId"), form.getId()));
            s1.setMaxRows(50);
            QueryView protGrpExpInfoView = new QueryView(new MqSchema(getUser(), getContainer()), s1, errors);
            protGrpExpInfoView.setTitle("Intensity And Coverage");

            // ProteinGroupRatiosSilac table
            QuerySettings s2 = new QuerySettings(getViewContext(), "SilacRatios", "ProteinGroupRatiosSilac");
            s2.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ProteinGroupId"), form.getId()));
            s2.setMaxRows(50);
            QueryView silacRatiosView = new QueryView(new MqSchema(getUser(), getContainer()), s2, errors);
            silacRatiosView.setTitle("Silac Ratios");

            // ProteinGroupIntensitySilac table
            QuerySettings s3 = new QuerySettings(getViewContext(), "SilacIntensities", "ProteinGroupIntensitySilac");
            s3.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ProteinGroupId"), form.getId()));
            s3.setMaxRows(50);
            QueryView silacInteisitiesView = new QueryView(new MqSchema(getUser(), getContainer()), s3, errors);
            silacInteisitiesView.setTitle("Silac Intensities");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(protGrpExpInfoView);
            view.addView(silacRatiosView);
            view.addView(silacInteisitiesView);
            return view;
        }
    }

    // ------------------------------------------------------------------------
    // View evidence list for a peptide
    // ------------------------------------------------------------------------
    @RequiresPermission(ReadPermission.class)
    public class ViewPeptideEvidenceAction extends SimpleViewAction<IdForm>
    {
        private int _experimentGroupId;
        private Peptide _peptide;

        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            if (_peptide != null)
            {
                root.addChild("MaxQuant Runs", getShowRunsUrl());
                root.addChild("Experiment Protein Groups", getProteinGroupsUrl(_experimentGroupId));
                root.addChild("Evidence for Peptide " + _peptide.getId());
            }
            return null;
        }

        @Override
        public void validate(IdForm form, BindException errors)
        {
            _peptide = PeptideManager.get(form.getId());
            if (_peptide == null)
                errors.reject(ERROR_MSG, "Peptide with ID " + form.getId() + " does not exist.");
            else
                _experimentGroupId = _peptide.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors) throws Exception
        {
            if (errors.hasErrors())
                return new SimpleErrorView(errors);

            // Files listing
            HtmlView fileDownloadView = getDownloadLinksView(_experimentGroupId, EvidenceParser.FILE);
            fileDownloadView.setTitle("Files");
            fileDownloadView.setFrame(WebPartView.FrameType.PORTAL);

            // Details of the  Peptide
            DetailsView exptDetailsView = getPeptideDetailsView(form.getId());
            VBox detailsBox = new VBox(exptDetailsView, fileDownloadView);
            detailsBox.setTitle("Peptide Details");
            detailsBox.setFrame(WebPartView.FrameType.PORTAL);

            // Evidence table
            QuerySettings settings = new QuerySettings(getViewContext(), "Evidence", "Evidence");
            settings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("PeptideId"), form.getId()));
            QueryView evidenceGridView = new QueryView(new MqSchema(getUser(), getContainer()), settings, errors);
            evidenceGridView.setTitle("Evidence");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(evidenceGridView);
            return view;
        }
    }

    public static class IdForm
    {
        private int _id;

        public int getId()
        {
            return _id;
        }

        public void setId(int id)
        {
            _id = id;
        }
    }

    private ActionURL getShowRunsUrl()
    {
        return PageFlowUtil.urlProvider(ExperimentUrls.class).getShowRunsURL(getContainer(), new MqExperimentRunType());
    }

    private ActionURL getProteinGroupsUrl(int id)
    {
        ActionURL url = new ActionURL(ViewProteinGroupsAction.class, getContainer());
        url.addParameter("id", id);
        return url;
    }

    private String[] allFiles = new String[] {SummaryTemplateParser.FILE, ProteinGroupsParser.FILE,
            PeptidesParser.FILE, ModifiedPeptidesParser.FILE, EvidenceParser.FILE};
    @NotNull
    private HtmlView getDownloadLinksView(int experimentGroupId, String... files)
    {
        if(files == null || files.length == 0)
        {
            files = allFiles;
        }

        ActionURL downloadFileUrl = new ActionURL(DownloadFileAction.class, getContainer());
        downloadFileUrl.addParameter("experimentGroupId", experimentGroupId);
        StringBuilder html = new StringBuilder();
        html.append("<ul>");

        for(String file: files)
        {
            if(file.equals(SummaryTemplateParser.FILE))
            {
                downloadFileUrl.replaceParameter("fileName", SummaryTemplateParser.FILE);
                html.append("<li><a href=\"" + downloadFileUrl.getLocalURIString() + "\">" + SummaryTemplateParser.FILE + "</a></li>");
            }
            else if(file.equals(ProteinGroupsParser.FILE))
            {
                downloadFileUrl.replaceParameter("fileName", ProteinGroupsParser.FILE);
                html.append("<li><a href=\"" + downloadFileUrl.getLocalURIString() + "\">proteinGroups.txt</a></li>");
            }
            else if(file.equals(PeptidesParser.FILE))
            {
                downloadFileUrl.replaceParameter("fileName", PeptidesParser.FILE);
                html.append("<li><a href=\"" + downloadFileUrl.getLocalURIString() + "\">peptides.txt</a></li>");
            }
            else if(file.equals(ModifiedPeptidesParser.FILE))
            {
                downloadFileUrl.replaceParameter("fileName", ModifiedPeptidesParser.FILE);
                html.append("<li><a href=\"" + downloadFileUrl.getLocalURIString() + "\">modificationSpecificPeptides.txt</a></li>");
            }
            else if(file.equals(EvidenceParser.FILE))
            {
                downloadFileUrl.replaceParameter("fileName", EvidenceParser.FILE);
                html.append("<li><a href=\"" + downloadFileUrl.getLocalURIString() + "\">evidence.txt</a></li>");
            }
        }
        html.append("</ul");
        return new HtmlView(html.toString());
    }

    @NotNull
    private JspView getExperimentGroupDetailsView(ExperimentGroup experimentGroup)
    {
        return new JspView<>("/org/labkey/mq/view/experimentGroupDetails.jsp", experimentGroup);
    }

    @NotNull
    private DetailsView getProteinGroupDetailsView(int proteinGroupid)
    {
        MqSchema schema = new MqSchema(getUser(), getContainer());
        TableInfo table = schema.createTable(TABLE_PROTEIN_GROUP);

        Collection<FieldKey> columns = new ArrayList<>();
        columns.add(FieldKey.fromParts("ProteinIds"));
        columns.add(FieldKey.fromParts("MajorityProteinIds"));
        columns.add(FieldKey.fromParts("ProteinNames"));
        columns.add(FieldKey.fromParts("GeneNames"));

        return getDetailsView(table, columns, proteinGroupid);
    }


    @NotNull
    private DetailsView getPeptideDetailsView(int peptideId)
    {
        MqSchema schema = new MqSchema(getUser(), getContainer());
        TableInfo table = schema.createTable(TABLE_PEPTIDE);

        Collection<FieldKey> columns = new ArrayList<>();
        columns.add(FieldKey.fromParts("Sequence"));
        columns.add(FieldKey.fromParts("Length"));
        columns.add(FieldKey.fromParts("Mass"));
        columns.add(FieldKey.fromParts("StartPosition"));
        columns.add(FieldKey.fromParts("EndPosition"));
        columns.add(FieldKey.fromParts("MissedCleavages"));
        columns.add(FieldKey.fromParts("ExperimentGroupId"));

        return getDetailsView(table, columns, peptideId);
    }

    @NotNull
    private DetailsView getDetailsView(TableInfo table, Collection<FieldKey> columns, int pkId)
    {
        DataRegion dataRegion = new DataRegion();
        dataRegion.setTable(table);
        Map<FieldKey, ColumnInfo> columnMap = QueryService.get().getColumns(table, columns);
        dataRegion.addColumns(new ArrayList<>(columnMap.values()));
        dataRegion.setButtonBar(new ButtonBar()); // No buttons in the button bar
        return new DetailsView(dataRegion, pkId);
    }
}