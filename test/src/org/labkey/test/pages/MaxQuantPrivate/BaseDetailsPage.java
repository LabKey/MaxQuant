package org.labkey.test.pages.MaxQuantPrivate;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

abstract class BaseDetailsPage<EC extends BaseDetailsPage.ElementCache> extends LabKeyPage<EC>
{
    public BaseDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public List<WebElement> getExperimentFileDownloadLinks()
    {
        return Locator.tagWithName("ul", "downloadLinks").append(Locator.tag("a")).findElements(getDriver());
    }

    public boolean hasFilesLinks(List<String> txtFileNames)
    {
        boolean valid = true;
        for (String txtFileName : txtFileNames)
        {
            if (Locator.linkWithText(txtFileName).findElements(getDriver()).size() != 1)
            {
                valid = false;
                break;
            }
        }
        return valid;
    }

    public DataRegionTable getGrid(String regionName)
    {
        return new DataRegionTable(regionName, getDriver());
    }
}