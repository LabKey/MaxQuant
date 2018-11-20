package org.labkey.test.pages.maxquant;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class ProteinGroupDetails extends BaseDetailsPage
{
    public ProteinGroupDetails(WebDriver driver)
    {
        super(driver);
    }

    public int proteinIdLinkCount(String value)
    {
        return Locator.linkContainingText(value).findElements(getDriver()).size();
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