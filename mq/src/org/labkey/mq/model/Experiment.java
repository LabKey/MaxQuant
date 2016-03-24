package org.labkey.mq.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vsharma on 2/3/2016.
 */
public class Experiment extends MqEntity
{
    private int _experimentGroupId;

    private String _experimentName;
    private List<RawFile> _rawfiles;

    public int getExperimentGroupId()
    {
        return _experimentGroupId;
    }

    public void setExperimentGroupId(int experimentGroupId)
    {
        _experimentGroupId = experimentGroupId;
    }

    public String getExperimentName()
    {
        return _experimentName;
    }

    public void setExperimentName(String experimentName)
    {
        _experimentName = experimentName;
    }

    public List<RawFile> getRawfiles()
    {
        return _rawfiles;
    }

    public void setRawfiles(List<RawFile> rawfiles)
    {
        _rawfiles = rawfiles;
    }

    public void addRawfile(RawFile rawfile)
    {
        if(rawfile == null)
            return;

        if(_rawfiles == null)
        {
            _rawfiles = new ArrayList<>();
        }
        _rawfiles.add(rawfile);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Experiment that = (Experiment) o;

        return _experimentName.equals(that._experimentName);

    }

    @Override
    public int hashCode()
    {
        return _experimentName.hashCode();
    }
}
