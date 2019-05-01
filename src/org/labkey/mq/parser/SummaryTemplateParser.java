package org.labkey.mq.parser;


import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.reader.Readers;
import org.labkey.mq.model.Experiment;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.RawFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vsharma on 2/3/2016.
 */
public class SummaryTemplateParser
{
    public static final String FILE = "summary.txt";

    private static int SUMMARY_TXT_RAW_FILE_INDEX = 0;

    public ExperimentGroup parse(File file) throws MqParserException
    {
        if (file == null)
        {
            throw new MqParserException("file is null.");
        }
        if (!file.exists())
        {
            throw new MqParserException("File does not exist: " + file.getPath());
        }

        BufferedReader reader = null;
        try
        {
            reader = Readers.getReader(file);
            ArrayList<String> fileLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null){
                fileLines.add(line);
            }
            String[] fileLinesArray = fileLines.toArray(new String[0]);
            validateFileHeader(fileLinesArray);
            Map<String, Experiment> experimentMap = getStringExperimentMap(fileLinesArray);
            ExperimentGroup expGroup = new ExperimentGroup(file.getParent());
            expGroup.setExperiments(new ArrayList<>(experimentMap.values()));
            return expGroup;
        }
        catch (IOException e)
        {
            throw new MqParserException("Error reading file " + file.getParent(), e);
        }
        finally
        {
            if (reader != null) try
            {
                reader.close();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    protected static Map<String, Experiment> getStringExperimentMap(String[] lines)
    {
        Map<String, Experiment> experimentMap = new HashMap<>();

        String[] headerParts = getParts(lines[0]);
        int SUMMARY_TXT_EXPERIMENT_INDEX = 1;
        boolean hasExperimentColumn = headerParts[SUMMARY_TXT_EXPERIMENT_INDEX].equalsIgnoreCase("Experiment");
        int SUMMARY_TXT_FRACTION_INDEX = 2;
        boolean hasFractionColumn = headerParts[SUMMARY_TXT_FRACTION_INDEX].equalsIgnoreCase("Fraction");

        Experiment experiment = null;
        for (int i = 1; i < lines.length; i++)
        {
            String[] parts = getParts(lines[i]);
            if (parts.length < 3)
            {
                throw new MqParserException("Expected at least 3 tab separated values; found " + parts.length);
            }

            String rawFileName = parts[SUMMARY_TXT_RAW_FILE_INDEX];

            if ("Total".equals(rawFileName))
            {
                // Skip the "Total" line at the end of the file
                continue;
            }

            String experimentName = null;
            if (hasExperimentColumn)
            {
                experimentName = parts[SUMMARY_TXT_EXPERIMENT_INDEX];

                if (experimentMap.containsKey(rawFileName) && experimentName.trim().isEmpty())
                {
                    // Skip experiment subtotal lines near the end of the file, if present
                    continue;
                }

                experiment = experimentMap.get(experimentName);
            }

            if (experiment == null)
            {
                experiment = new Experiment();
                experiment.setExperimentName(experimentName);
                experimentMap.put(experiment.getExperimentName(), experiment);
            }

            String fraction = "NA";
            if (hasFractionColumn)
            {
                fraction = parts[SUMMARY_TXT_FRACTION_INDEX];
            }

            experiment.addRawfile(new RawFile(rawFileName, fraction));
        }
        return experimentMap;
    }

    protected static void validateFileHeader(String[] fileLines)
    {
        if(fileLines.length == 0){
            throw new MqParserException("The selected file is empty.");
        }
        String line = fileLines[0]; // header
        if (line != null)
        {
            String[] parts = getParts(line);
            if (!parts[SUMMARY_TXT_RAW_FILE_INDEX].equalsIgnoreCase("Raw file"))
            {
                throw new MqParserException("Expected header starting with: \\'Raw File\\'; found " + line);
            }
        }
    }

    private static String[] getParts(String line) throws MqParserException
    {
        return line.split("\\t");
    }

    public static class TestCase extends Assert
    {
        @Test
        public void testValidateFile()
        {
            String fileHeader = "Raw file\tExperiment\tFraction\tEnzyme\tEnzyme mode\tEnzyme first search\tEnzyme " +
                    "mode first search\t" + "Use enzyme first search\tVariable modifications\tMulti modifications\t"
                    + "Variable modifications first search\tUse variable modifications first search\tRequantify\t" +
                    "Multiplicity\tMax. missed cleavages\tLabels0\tLC-MS run type\tTime-dependent recalibration\t" +
                    "MS\tMS/MS\tMS3\tMS/MS Submitted\tMS/MS Submitted (SIL)\tMS/MS Submitted (ISO)\t" + "MS/MS " +
                    "Submitted (PEAK)\tMS/MS Identified\tMS/MS Identified (SIL)\tMS/MS Identified (ISO)\t" + "MS/MS " +
                    "Identified (PEAK)\tMS/MS Identified [%]\tMS/MS Identified (SIL) [%]\tMS/MS Identified (ISO) " +
                    "[%]\tMS/MS Identified (PEAK) [%]\tPeptide Sequences Identified\tPeaks\tPeaks Sequenced\tPeaks " +
                    "Sequenced [%]\tPeaks Repeatedly Sequenced\tPeaks Repeatedly Sequenced [%]\tIsotope " +
                    "Patterns\tIsotope Patterns Sequenced\tIsotope Patterns Sequenced (z>1)\tIsotope Patterns " +
                    "Sequenced [%]\tIsotope Patterns Sequenced (z>1) [%]\tIsotope Patterns Repeatedly " +
                    "Sequenced\tIsotope Patterns Repeatedly Sequenced [%]\tRecalibrated\tAv. Absolute Mass Deviation " +
                    "[ppm]\tMass Standard Deviation [ppm]\tAv. Absolute Mass Deviation [mDa]\tMass Standard Deviation" +
                    " [mDa]\tLabel free norm param\n";

            String experiment = "rawfiledata\texperimentdat\t1\tenzymedata\tSpecific\t\t\tFalse\tfake (F)" +
                    "\t\tTrue\tFalse\t3\t4\t\tfake\tfake " +
                    "fake\t\t1111\t22222\t0\t33333\t44444\t0\t55555\t6666\t7777\t0\t888\t99.99\t10.10\tNaN\t2.22" +
                    "\t1212\t13131\t14141\t5.55\t1515\t7.77\t15151\t16161\t17171\t88.88\t99.99\t1818\t19.19\t" +
                    "-\t0.12345\t0.4587\t20202\t212121\t\n";
            String endLine1 = "experimentdat\t \t \t";
            String endLine2 = "Total\t \t \t";

            String[] fileLines = new String[]{fileHeader,experiment, endLine1, endLine2};

            validateFileHeader(fileLines);

            Map<String, Experiment> experimentMapExpected = new HashMap<>();
            Experiment experiment1 = new Experiment();
            experiment1.setExperimentName("experimentdat");
            experimentMapExpected.put("experimentdat", experiment1);
            experiment1.addRawfile(new RawFile("rawfiledata","1" ));

            Map<String, Experiment> resultMap = getStringExperimentMap(fileLines);
            assertEquals("Map does not match",experimentMapExpected.entrySet(), resultMap.entrySet());
            assertEquals("Map does not match",experimentMapExpected.get("experimentdat"), resultMap.get("experimentdat"));
        }
    }
}
