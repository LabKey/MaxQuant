package org.labkey.test.pages.maxquantprivate;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ExperimentGroupDetails extends BaseDetailsPage<ExperimentGroupDetails.ElementCache>
{
    public ExperimentGroupDetails(WebDriver driver)
    {
        super(driver);
    }

    public int getProteinGroupsCount()
    {
        return Integer.parseInt(getProteinGroupsLink().getText());
    }
    
    public WebElement getProteinGroupsLink()
    {
        return elementCache().getProteinGroupsLinkLoc().waitForElement(getDriver(), 1000);
    }

    public int getPeptidesCount()
    {
        return Integer.parseInt(getPeptidesLink().getText());
    }

    public WebElement getPeptidesLink()
    {
        return elementCache().getPeptidesLinkLoc().waitForElement(getDriver(), 1000);
    }

    public DataRegionTable getProteinGroupsGrid()
    {
        return getGrid("ProteinGroups");
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        public Locator getProteinGroupsLinkLoc()
        {
            return Locator.id("lk-mq-proteingroups").childTag("a");
        }

        public Locator getPeptidesLinkLoc()
        {
            return Locator.id("lk-mq-peptides").childTag("a");
        }
    }
}