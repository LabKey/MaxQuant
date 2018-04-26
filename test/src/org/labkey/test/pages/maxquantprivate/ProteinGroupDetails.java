package org.labkey.test.pages.maxquantprivate;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class ProteinGroupDetails extends BaseDetailsPage
{
    public ProteinGroupDetails(WebDriver driver)
    {
        super(driver);
    }

    public boolean hasProteinIdLink(String value, int count)
    {
        return Locator.linkContainingText(value).findElements(getDriver()).size() == count;
    }

    public DataRegionTable getIntensityAndCoverageGrid()
    {
        return getGrid("IntensityAndCoverage");
    }

    public DataRegionTable getSilacRatiosGrid()
    {
        return getGrid("SilacRatios");
    }

    public DataRegionTable getSilacIntensitiesGrid()
    {
        return getGrid("SilacIntensities");
    }

    public DataRegionTable getProteinGroupTMTPivotGrid()
    {
        return getGrid("ProteinGroupTMTPivot");
    }

    public DataRegionTable getPeptidesGrid()
    {
        return getGrid("Peptides");
    }
}