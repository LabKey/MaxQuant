package org.labkey.mq.model;

/**
 * Created by vsharma on 2/3/2016.
 */
public class RawFile extends MqEntity
{
    private int _experimentId;

    private String _name;
    private String _fraction;

    public RawFile() {}

    public RawFile(String name, String fraction)
    {
        _name = name;
        _fraction = fraction;
    }
    public int getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(int experimentId)
    {
        _experimentId = experimentId;
    }

    public String getName()
    {
        return _name;
    }

    public String getFraction()
    {
        return _fraction;
    }
}
