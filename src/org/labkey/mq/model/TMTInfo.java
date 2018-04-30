package org.labkey.mq.model;

public class TMTInfo extends MqEntity
{
    private Integer _tmtChannelId;
    private Integer _experimentId;
    private Double _reporterIntensity;
    private Double _reporterIntensityCorrected;
    private Integer _reporterIntensityCount;

    private Integer _tagNumber;

    public Integer getTagNumber()
    {
        return _tagNumber;
    }

    public void setTagNumber(Integer tagNumber)
    {
        _tagNumber = tagNumber;
    }

    public Integer getTMTChannelId()
    {
        return _tmtChannelId;
    }

    public void setTMTChannelId(Integer tmtChannelId)
    {
        _tmtChannelId = tmtChannelId;
    }

    public Integer getExperimentId()
    {
        return _experimentId;
    }

    public void setExperimentId(Integer experimentId)
    {
        _experimentId = experimentId;
    }

    public Double getReporterIntensity()
    {
        return _reporterIntensity;
    }

    public void setReporterIntensity(Double reporterIntensity)
    {
        _reporterIntensity = reporterIntensity;
    }

    public Double getReporterIntensityCorrected()
    {
        return _reporterIntensityCorrected;
    }

    public void setReporterIntensityCorrected(Double reporterIntensityCorrected)
    {
        _reporterIntensityCorrected = reporterIntensityCorrected;
    }

    public Integer getReporterIntensityCount()
    {
        return _reporterIntensityCount;
    }

    public void setReporterIntensityCount(Integer reporterIntensityCount)
    {
        _reporterIntensityCount = reporterIntensityCount;
    }

    public void copyFrom(TMTInfo tmtInfo)
    {
        setTMTChannelId(tmtInfo.getTMTChannelId());
        setExperimentId(tmtInfo.getExperimentId());
        setReporterIntensity(tmtInfo.getReporterIntensity());
        setReporterIntensityCorrected(tmtInfo.getReporterIntensityCorrected());
        setReporterIntensityCount(tmtInfo.getReporterIntensityCount());
    }

    public String getMissingFields()
    {
        String missing = "", sep = "";
        if (getReporterIntensity() == null)
        {
            missing += "Reporter intensity";
            sep = ", ";
        }
        if (getReporterIntensityCorrected() == null)
        {
            missing += sep + "Reporter intensity corrected";
            sep = ", ";
        }
        if (getReporterIntensityCount() == null)
        {
            missing += sep + "Reporter intensity count";
        }
        return missing.length() > 0 ? missing : null;
    }
}
