package org.labkey.test.pages.MaxQuantPrivate;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class ExperimentGroupDetails extends BaseDetailsPage<ExperimentGroupDetails.ElementCache>
{
    public ExperimentGroupDetails(WebDriver driver)
    {
        super(driver);
    }

    public boolean hasProteinGroupsLink(int value)
    {
        return elementCache().getProteinGroupsLinkLoc(value).findElements(getDriver()).size() == 1;
    }

    public boolean hasPeptidesLink(int value)
    {
        return elementCache().getPeptidesLinkLoc(value).findElements(getDriver()).size() == 1;
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
        public Locator getProteinGroupsLinkLoc(int value)
        {
            return Locator.id("lk-mq-proteingroups").append(Locator.linkWithText(""+value));
        }

        public Locator getPeptidesLinkLoc(int value)
        {
            return Locator.id("lk-mq-peptides").append(Locator.linkWithText(""+value));
        }
    }
}