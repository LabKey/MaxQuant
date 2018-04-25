package org.labkey.mq.parser;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.mq.model.Experiment;
import org.labkey.mq.model.TMTInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vsharma on 3/16/2016.
 */
public class MaxQuantTsvParser extends TsvParser
{
    protected static final String MaxQuantId = "id";

    protected static final String TMT_REPORTER_INTENSITY_PREFIX = "Reporter intensity ";
    protected static final String TMT_REPORTER_INTENSITY_CORRECTED_PREFIX = "Reporter intensity corrected ";
    protected static final String TMT_REPORTER_INTENSITY_COUNT_PREFIX = "Reporter intensity count ";

    public MaxQuantTsvParser(File file) throws MqParserException
    {
        super(file);
    }

    protected  boolean getBooleanValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        return val.equals("+");
    }

    protected int getIntValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse integer value in \"" + column + "\": " + val);
        }
    }

    protected Integer tryGetIntValue(TsvRow row, String column)
    {
        return getIntValue(row, column, null);
    }

    protected Integer getIntValue(TsvRow row, String column, Integer defaultVal)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return defaultVal;
        }

        int idx = val.indexOf(".");
        if (idx != -1)
        {
            val = val.substring(0, idx);
        }
        try
        {
            return Integer.valueOf(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse integer value in \"" + column + "\": " + val);
        }
    }

    protected long getLongValue(TsvRow row, String column)
    {
        String val = getValue(row, column);
        try
        {
            return Math.round(Double.parseDouble(val));
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row,  "Cannot parse long value in \"" + column + "\": " + val);
        }
    }

    protected Long tryGetLongValue(TsvRow row, String column)
    {
        return getLongValue(row, column, null);
    }

    protected Long getLongValue(TsvRow row, String column, Long defaultVal)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return defaultVal;
        }

        int idx = val.indexOf(".");
        if (idx != -1)
        {
            val = val.substring(0, idx);
        }

        try
        {
            return Long.parseLong(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row,  "Cannot parse long value in \"" + column + "\": " + val);
        }
    }

    protected Double getDoubleValue(TsvRow row, String column)
    {
        String val = getValue(row, column);

        try
        {
            double parseDouble = Double.parseDouble(val);
            return Double.isNaN(parseDouble) ? null : parseDouble;
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse double value in \"" + column + "\": " + val);
        }
    }

    protected Double tryGetDoubleValue(TsvRow row, String column)
    {
        String val = tryGetValue(row, column);
        if(val == null)
        {
            return null;
        }
        try
        {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e)
        {
            throw new MqParserException(super.getFileName(), row, "Cannot parse double value in \"" + column + "\": " + val);
        }
    }

    @NotNull
    protected String getValue(TsvRow row, String column)
    {
        String val = row.getValue(column);
        if (val == null)
        {
            throw new MqParserException(super.getFileName(), row, " Could not find value for \"" + column + "\"" );
        }
        return val;
    }

    protected String tryGetValue(TsvRow row, String column)
    {
        String val = row.getValue(column);
        return StringUtils.isBlank(val) ? null : val.trim();
    }


    public static class MaxQuantTsvRow
    {
        private int _maxQuantId;
        private List<TMTInfo> _tmtInfos = new ArrayList<>();

        public int getMaxQuantId()
        {
            return _maxQuantId;
        }

        public void setMaxQuantId(int maxQuantId)
        {
            _maxQuantId = maxQuantId;
        }

        public void setTMTInfos(List<TMTInfo> tmtInfos)
        {
            _tmtInfos = tmtInfos;
        }

        public List<TMTInfo> getTMTInfos()
        {
            return Collections.unmodifiableList(_tmtInfos);
        }
    }

    protected List<TMTInfo> getTMTInfosFromRow(TsvRow row, @NotNull List<Experiment> experiments)
    {
        List<TMTInfo> reporterIntensityInfos = new ArrayList<>();

        // starting with 0, look for reporter intensity columns by tag number (until we get to a tag number that doesn't exist)
        int tagNumber = 0;
        while (row.getValue(TMT_REPORTER_INTENSITY_PREFIX + tagNumber) != null)
        {
            reporterIntensityInfos.add(getExperimentTMTInfoFromRow(row, tagNumber, null));

            // also look for the experiment name specific columns
            for (Experiment experiment : experiments)
            {
                String tagPlusExperiment = tagNumber + " " + experiment.getExperimentName();
                if (row.getValue(TMT_REPORTER_INTENSITY_PREFIX + tagPlusExperiment) != null)
                    reporterIntensityInfos.add(getExperimentTMTInfoFromRow(row, tagNumber, experiment));
            }

            tagNumber++;
        }

        return reporterIntensityInfos;
    }

    private TMTInfo getExperimentTMTInfoFromRow(TsvRow row, int tagNumber, @Nullable Experiment experiment)
    {
        String tagPlusSuffix = String.valueOf(tagNumber) + (experiment != null ? " " + experiment.getExperimentName() : "");
        TMTInfo info = new TMTInfo();
        info.setTagNumber(tagNumber);
        info.setReporterIntensity(tryGetDoubleValue(row, TMT_REPORTER_INTENSITY_PREFIX + tagPlusSuffix));
        info.setReporterIntensityCorrected(tryGetDoubleValue(row, TMT_REPORTER_INTENSITY_CORRECTED_PREFIX + tagPlusSuffix));
        info.setReporterIntensityCount(tryGetIntValue(row, TMT_REPORTER_INTENSITY_COUNT_PREFIX + tagPlusSuffix));
        info.setExperimentId(experiment != null ? experiment.getId() : null);
        return info;
    }
}
