package org.labkey.mq.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by vsharma on 2/3/2016.
 */
public class ExperimentGroup extends MqEntity implements Serializable
{
    private Integer _dataId; // FK to exp.data's RowId column
    private String _description;
    private String _fileName;
    private int _statusId;
    private String _status;
    private boolean _deleted;
    private String _experimentRunLSID;
    // TODO: Add MaxQuant version

    private String _locationOnFileSystem;

    private List<Experiment> _experiments;

    public ExperimentGroup() {}

    public ExperimentGroup(String locationOnFileSystem)
    {
        _locationOnFileSystem = locationOnFileSystem;
    }

    public Integer getDataId()
    {
        return _dataId;
    }

    public void setDataId(Integer dataId)
    {
        _dataId = dataId;
    }

    public String getStatus()
    {
        return _status;
    }

    public void setStatus(String status)
    {
        _status = status;
    }

    public int getStatusId()
    {
        return _statusId;
    }

    public void setStatusId(int statusId)
    {
        _statusId = statusId;
    }

    public boolean isDeleted()
    {
        return _deleted;
    }

    public void setDeleted(boolean deleted)
    {
        _deleted = deleted;
    }

    public String getExperimentRunLSID()
    {
        return _experimentRunLSID;
    }

    public void setExperimentRunLSID(String experimentRunLSID)
    {
        _experimentRunLSID = experimentRunLSID;
    }

    public void setLocationOnFileSystem(String locationOnFileSystem)
    {
        _locationOnFileSystem = locationOnFileSystem;
    }

    public String getLocationOnFileSystem()
    {
        return _locationOnFileSystem;
    }

    public List<Experiment> getExperiments()
    {
        return _experiments;
    }

    public void setExperiments(List<Experiment> experiments)
    {
        _experiments = experiments;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
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
