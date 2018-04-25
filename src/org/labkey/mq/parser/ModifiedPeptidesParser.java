package org.labkey.mq.parser;

import org.labkey.mq.model.Experiment;

import java.io.File;
import java.util.List;

/**
 * Created by vsharma on 3/17/2016.
 */
public class ModifiedPeptidesParser extends MaxQuantTsvParser
{
    public static final String FILE = "modificationSpecificPeptides.txt";

    private static final String Modifications = "Modifications";
    private static final String Mass = "Mass";
    private static final String MaxQuantPeptideId = "Peptide ID";

    public ModifiedPeptidesParser(File file) throws MqParserException
    {
        super(file);
    }

    public ModifiedPeptideRow nextModifiedPeptide(List<Experiment> experiments)
    {
        TsvRow row = super.nextRow();
        if(row == null)
        {
            return null;
        }
        ModifiedPeptideRow pepRow = new ModifiedPeptideRow();
        pepRow.setModifications(getValue(row, Modifications));
        pepRow.setMass(getDoubleValue(row, Mass));
        pepRow.setMaxQuantId(getIntValue(row, MaxQuantId));
        pepRow.setMaxQuantPeptideId(getIntValue(row, MaxQuantPeptideId));
        pepRow.setTMTInfos(getTMTInfosFromRow(row, experiments));
        return pepRow;
    }

    public static final class ModifiedPeptideRow extends MaxQuantTsvRow
    {
        private String _modifications;
        private double _mass;
        private int _maxQuantPeptideId;

        public String getModifications()
        {
            return _modifications;
        }

        public void setModifications(String modifications)
        {
            _modifications = modifications;
        }

        public double getMass()
        {
            return _mass;
        }

        public void setMass(double mass)
        {
            _mass = mass;
        }

        public int getMaxQuantPeptideId()
        {
            return _maxQuantPeptideId;
        }

        public void setMaxQuantPeptideId(int maxQuantPeptideId)
        {
            _maxQuantPeptideId = maxQuantPeptideId;
        }
    }
}
