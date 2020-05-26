package org.labkey.mq;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.util.FileUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.mq.model.ExperimentGroup;

import java.sql.SQLException;

/**
 * Created by vsharma on 2/5/2016.
 */
public class MqImportPipelineJob extends PipelineJob
{
    private final ExpData _expData;
    private MqExperimentImporter.RunInfo _runInfo;

    @JsonCreator
    protected MqImportPipelineJob(@JsonProperty("_expData") ExpData expData, @JsonProperty("_runInfo") MqExperimentImporter.RunInfo runInfo)
    {
        super();
        _expData = expData;
        _runInfo = runInfo;
    }

    public MqImportPipelineJob(ViewBackgroundInfo info, ExpData expData, MqExperimentImporter.RunInfo runInfo, PipeRoot root) throws SQLException
    {
        super(MqPipelineProvider.NAME, info, root);
        _expData = expData;
        _runInfo = runInfo;

        String basename = FileUtil.getBaseName(_expData.getFile(), 1);
        setLogFile(FT_LOG.newFile(_expData.getFile().getParentFile(), basename));
    }

    @Override
    public ActionURL getStatusHref()
    {
//        if (_runInfo.getRunId() > 0)
//        {
//            return TargetedMSController.getShowRunURL(getContainer(), _runInfo.getRunId());
//        }
        return null;
    }

    @Override
    public String getDescription()
    {
        return "MaxQuant import - " + _expData.getFile().getName();
    }

    @Override
    public void run()
    {
        if (!setStatus("LOADING"))
        {
            return;
        }

        boolean completeStatus = false;
        try
        {
            XarContext context = new XarContext(this);
            MqExperimentImporter importer = new MqExperimentImporter(getUser(), getContainer(), getDescription(), _expData, getLogger(), context);
            ExperimentGroup expGrp = importer.importExperiment(_runInfo);

            ExpRun expRun = MqManager.ensureWrapped(expGrp, getUser());

            setStatus(TaskStatus.complete);
            completeStatus = true;
        }
        catch (Exception e)
        {
            getLogger().error("MaxQuant import failed", e);
        }
        finally
        {
            if (!completeStatus)
            {
                setStatus(TaskStatus.error);
            }
        }
    }
}
