package org.labkey.mq;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.parser.SummaryTemplateParser;

import java.io.File;
import java.util.Arrays;

/**
 * Created by vsharma on 2/3/2016.
 */
public class MqDatahandler extends AbstractExperimentDataHandler
{

    @Nullable
    @Override
    public DataType getDataType()
    {
        return null;
    }

    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {

    }

    @Override
    public ActionURL getContentURL(ExpData data)
    {
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        ExperimentGroup experimentGroup = MqManager.getExperimentGroupByDataId(data.getRowId(), container);
        if (experimentGroup != null)
        {
            deleteRun(container, user, experimentGroup);
        }
        data.delete(user);
    }

    private void deleteRun(Container container, User user, ExperimentGroup experimentGroup)
    {
        MqManager.markDeleted(Arrays.asList(experimentGroup.getId()), container, user);
        MqManager.purgeDeletedExperimentGroups();
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {

    }

    @Nullable
    @Override
    public Priority getPriority(ExpData data)
    {
        String url = data.getDataFileUrl();
        if (url == null)
            return null;

        File file = new File(url);
        String filename = file.getName();
        if (SummaryTemplateParser.FILE.equals(filename.toLowerCase()))
            return Priority.HIGH;

        return null;
    }
}
