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
import org.labkey.api.action.FormHandlerAction;
import org.labkey.api.action.SimpleErrorView;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.pipeline.PipelineUrls;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DetailsView;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.RedirectException;
import org.labkey.api.view.VBox;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.WebPartView;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.Peptide;
import org.labkey.mq.model.ProteinGroup;
import org.labkey.mq.parser.EvidenceParser;
import org.labkey.mq.parser.ModifiedPeptidesParser;
import org.labkey.mq.parser.PeptidesParser;
import org.labkey.mq.parser.ProteinGroupsParser;
import org.labkey.mq.parser.SummaryTemplateParser;
import org.labkey.mq.query.PeptideManager;
import org.labkey.mq.query.ProteinGroupManager;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.labkey.mq.MqSchema.QUERY_PEPTIDE_TMT_PIVOT;
import static org.labkey.mq.MqSchema.QUERY_PROTEIN_GROUP_TMT_PIVOT;
import static org.labkey.mq.MqSchema.TABLE_PEPTIDE;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP_EXPERIMENT_INFO;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP_INTENSITY_SILAC;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP_PEPTIDE;
import static org.labkey.mq.MqSchema.TABLE_PROTEIN_GROUP_RATIOS_SILAC;

public class MqController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(MqController.class);
    static final String NAME = "mq";

    public MqController()
    {
        setActionResolver(_actionResolver);
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors)
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
    public class MaxQuantUploadAction extends FormHandlerAction<MaxQuantPipelinePathForm>
    {
        @Override
        public void validateCommand(MaxQuantPipelinePathForm form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(MaxQuantPipelinePathForm form, BindException errors) throws Exception
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
                catch (IOException | SQLException e)
                {
                    errors.reject(ERROR_MSG, e.getMessage());
                    return false;
                }
            }

            return true;
        }

        @Override
        public ActionURL getSuccessURL(MaxQuantPipelinePathForm form)
        {
            return PageFlowUtil.urlProvider(PipelineUrls.class).urlBegin(getContainer());
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

            ExperimentGroup expGrp = MqManager.getExperimentGroup(form.getExperimentGroupId(), getContainer());
            if(expGrp == null)
            {
                throw new NotFoundException("Could not find an experiment group with ID " + form.getExperimentGroupId() + " in this container.");
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

    @RequiresPermission(DeletePermission.class)
    public class DeleteExperimentGroupsAction extends FormHandlerAction<QueryForm>
    {
        private ActionURL _returnURL;

        @Override
        public void validateCommand(QueryForm target, Errors errors)
        {
        }

        @Override
        public boolean handlePost(QueryForm form, BindException errors)
        {
            _returnURL = form.getReturnActionURL();

            List<Integer> rowIds = new ArrayList<>();
            for (String selectedIdStr : DataRegionSelection.getSelected(getViewContext(), true))
                rowIds.add(Integer.parseInt(selectedIdStr));

            try (DbScope.Transaction transaction = MqSchema.getSchema().getScope().ensureTransaction())
            {
                // Issue 34093: if the experiment group is part of a successful ExpRun, delete from the run level
                for (Integer rowId : rowIds)
                {
                    ExperimentGroup expGrp = MqManager.getExperimentGroup(rowId, getContainer());
                    if (expGrp != null)
                    {
                        ExpRun expRun = ExperimentService.get().getExpRun(expGrp.getExperimentRunLSID());
                        if (expRun != null)
                            expRun.delete(getUser());
                        else
                            MqManager.markDeleted(Collections.singletonList(expGrp.getId()), getContainer(), getUser());
                    }
                }

                MqManager.purgeDeletedExperimentGroups();
                transaction.commit();
            }

            return true;
        }

        @Override
        public URLHelper getSuccessURL(QueryForm form)
        {
            return _returnURL;
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
            _experimentGroup = MqManager.getExperimentGroup(form.getId(), getContainer());
            if (_experimentGroup == null)
                errors.reject(ERROR_MSG, "Experiment group with ID " + form.getId() + " does not exist in this container.");
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors)
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
            QuerySettings settings = new QuerySettings(getViewContext(), "ProteinGroups", TABLE_PROTEIN_GROUP);
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
            _experimentGroup = MqManager.getExperimentGroup(form.getId(), getContainer());
            if (_experimentGroup == null)
                errors.reject(ERROR_MSG, "Experiment group with ID " + form.getId() + " does not exist in this container.");
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors)
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
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", TABLE_PEPTIDE);
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
            _proteinGroup = ProteinGroupManager.get(form.getId(), getContainer());
            if (_proteinGroup == null)
                errors.reject(ERROR_MSG, "Protein group with ID " + form.getId() + " does not exist in this container.");
            else
                _experimentGroupId = _proteinGroup.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors)
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
            QuerySettings settings = new QuerySettings(getViewContext(), "Peptides", TABLE_PROTEIN_GROUP_PEPTIDE);
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
            _proteinGroup = ProteinGroupManager.get(form.getId(), getContainer());
            if (_proteinGroup == null)
                errors.reject(ERROR_MSG, "Protein group with ID " + form.getId() + " does not exist in this container.");
            else
                _experimentGroupId = _proteinGroup.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors)
        {
            VBox view = new VBox();
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
            view.addView(detailsBox);

            // ProteinGroupExperimentInfo table
            QuerySettings s1 = getQuerySettings("IntensityAndCoverage", TABLE_PROTEIN_GROUP_EXPERIMENT_INFO, form.getId());
            QueryView protGrpExpInfoView = new QueryView(new MqSchema(getUser(), getContainer()), s1, errors);
            protGrpExpInfoView.setTitle("Intensity And Coverage");
            view.addView(protGrpExpInfoView);

            // ProteinGroupTMT pivot query
            if (ProteinGroupManager.hasData(MqManager.getTableInfoProteinGroupTMT(), form.getId(), getContainer()))
            {
                QuerySettings s4 = getQuerySettings(QUERY_PROTEIN_GROUP_TMT_PIVOT, QUERY_PROTEIN_GROUP_TMT_PIVOT, form.getId());
                QueryView tmtView = new QueryView(new MqSchema(getUser(), getContainer()), s4, errors);
                tmtView.setTitle("TMT");
                view.addView(tmtView);
            }

            // ProteinGroupRatiosSilac table
            if (ProteinGroupManager.hasData(MqManager.getTableInfoProteinGroupRatiosSilac(), form.getId(), getContainer()))
            {
                QuerySettings s2 = getQuerySettings("SilacRatios", TABLE_PROTEIN_GROUP_RATIOS_SILAC, form.getId());
                QueryView silacRatiosView = new QueryView(new MqSchema(getUser(), getContainer()), s2, errors);
                silacRatiosView.setTitle("Silac Ratios");
                view.addView(silacRatiosView);
            }

            // ProteinGroupIntensitySilac table
            if (ProteinGroupManager.hasData(MqManager.getTableInfoProteinGroupIntensitySilac(), form.getId(), getContainer()))
            {
                QuerySettings s3 = getQuerySettings("SilacIntensities", TABLE_PROTEIN_GROUP_INTENSITY_SILAC, form.getId());
                QueryView silacInteisitiesView = new QueryView(new MqSchema(getUser(), getContainer()), s3, errors);
                silacInteisitiesView.setTitle("Silac Intensities");
                view.addView(silacInteisitiesView);
            }
            
            return view;
        }

        private QuerySettings getQuerySettings(String dataRegionName, String queryName, int proteinGroupId)
        {
            QuerySettings qs = new QuerySettings(getViewContext(), dataRegionName, queryName);
            qs.setBaseFilter(new SimpleFilter(FieldKey.fromParts("ProteinGroupId"), proteinGroupId));
            qs.setMaxRows(50);
            return qs;
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
            _peptide = PeptideManager.get(form.getId(), getContainer());
            if (_peptide == null)
                errors.reject(ERROR_MSG, "Peptide with ID " + form.getId() + " does not exist.");
            else
                _experimentGroupId = _peptide.getExperimentGroupId();
        }

        @Override
        public ModelAndView getView(IdForm form, BindException errors)
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

            // PeptideTMT pivot query
            QuerySettings tmtPivotSettings = new QuerySettings(getViewContext(), QUERY_PEPTIDE_TMT_PIVOT, QUERY_PEPTIDE_TMT_PIVOT);
            tmtPivotSettings.setBaseFilter(new SimpleFilter(FieldKey.fromParts("PeptideId"), form.getId()));
            QueryView tmtGridView = new QueryView(new MqSchema(getUser(), getContainer()), tmtPivotSettings, errors);
            tmtGridView.setTitle("TMT");

            VBox view = new VBox();
            view.addView(detailsBox);
            view.addView(evidenceGridView);
            view.addView(tmtGridView);
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
        html.append("<ul name=\"downloadLinks\">");

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
        TableInfo table = schema.createTable(TABLE_PROTEIN_GROUP, null);

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
        TableInfo table = schema.createTable(TABLE_PEPTIDE, null);

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