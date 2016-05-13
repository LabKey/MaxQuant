package org.labkey.mq.parser;


import org.labkey.mq.model.Experiment;
import org.labkey.mq.model.ExperimentGroup;
import org.labkey.mq.model.RawFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vsharma on 2/3/2016.
 */
public class ExperimentDesignTemplateParser
{
    public static final String FILE = "experimentalDesignTemplate.txt";

    public ExperimentGroup parse(File file) throws MqParserException
    {
        if(file == null)
        {
            throw new MqParserException("file is null.");
        }
        if(!file.exists())
        {
            throw new MqParserException("File does not exist: " + file.getPath());
        }

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine(); // header
            if(line != null)
            {
                String[] parts = getParts(line);
                if(parts.length != 3
                        || !parts[0].equalsIgnoreCase("Name")
                        || !parts[1].equalsIgnoreCase("Fraction")
                        || !parts[2].equalsIgnoreCase("Experiment"))
                {
                    throw new MqParserException("Expected header: \\'Name   Fraction    Experiment\\'; found " + line);
                }
            }

            Map<String, Experiment> experimentMap = new HashMap<>();

            while((line = reader.readLine()) != null)
            {
                String[] parts = getParts(line);
                if(parts.length != 3)
                {
                    throw new MqParserException("Expected 3 tab separated values; found " + parts.length);
                }

                Experiment experiment = experimentMap.get(parts[2]);
                if(experiment == null)
                {
                    experiment = new Experiment();
                    experiment.setExperimentName(parts[2]);
                    experimentMap.put(experiment.getExperimentName(), experiment);
                }

                experiment.addRawfile(new RawFile(parts[0], parts[1]));
            }
            ExperimentGroup expGroup = new ExperimentGroup(file.getParent());
            expGroup.setExperiments(new ArrayList<>(experimentMap.values()));
            return expGroup;
        }
        catch(IOException e)
        {
            throw new MqParserException("Error reading file " + file.getParent(), e);
        }
        finally
        {
            if(reader != null) try {reader.close();} catch(Exception ignored){}
        }
    }

    private String[] getParts(String line) throws MqParserException
    {
        return line.split("\\t");
    }
}
