package org.labkey.test.pages.MaxQuantPrivate;

import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.openqa.selenium.WebDriver;

public class PeptideDetails extends BaseDetailsPage
{
    public PeptideDetails(WebDriver driver)
    {
        super(driver);
    }

    public String getSequence()
    {
        return getTableCellText(1);
    }

    public String getLength()
    {
        return getTableCellText(2);
    }

    public String getMass()
    {
        return getTableCellText(3);
    }

    public String getStartPosition()
    {
        return getTableCellText(4);
    }

    public String getEndPosition()
    {
        return getTableCellText(5);
    }

    public String getMissedCleavages()
    {
        return getTableCellText(6);
    }

    private String getTableCellText(int rowIndex)
    {
        return Locator.xpath("//form/table/tbody/tr[" + rowIndex + "]/td[2]").findElement(getDriver()).getText();
    }

    public DataRegionTable getEvidenceGrid()
    {
        return getGrid("Evidence");
    }
}