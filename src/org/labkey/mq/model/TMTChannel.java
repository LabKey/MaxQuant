package org.labkey.mq.model;

public class TMTChannel extends MqEntity
{
    private Integer _experimentGroupId;
    private Integer _tagNumber;

    public Integer getExperimentGroupId()
    {
        return _experimentGroupId;
    }

    public void setExperimentGroupId(Integer experimentGroupId)
    {
        _experimentGroupId = experimentGroupId;
    }

    public Integer getTagNumber()
    {
        return _tagNumber;
    }

    public void setTagNumber(Integer tagNumber)
    {
        _tagNumber = tagNumber;
    }
}
