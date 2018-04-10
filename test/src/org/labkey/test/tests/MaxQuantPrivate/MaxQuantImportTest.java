/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests.MaxQuantPrivate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.MaxQuantPrivate.PeptideDetails;
import org.labkey.test.pages.MaxQuantPrivate.ProteinGroupDetails;
import org.labkey.test.pages.MaxQuantPrivate.ExperimentGroupDetails;
import org.labkey.test.util.DataRegionTable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({Git.class})
public class MaxQuantImportTest extends BaseWebDriverTest
{
    private String SAMPLE_DATA_DIR = "/MaxQuant_TMT_public_w_mods_subset";
    private int PROTEIN_GROUPS_COUNT = 51;
    private int PEPTIDES_COUNT = 337;

    @BeforeClass
    public static void setupProject()
    {
        MaxQuantImportTest init = (MaxQuantImportTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(getProjectName(), "MaxQuant");
        File sampleDataDir = TestFileUtils.getSampleData(SAMPLE_DATA_DIR);
        setPipelineRoot(sampleDataDir.getPath());
        loadInitialDataSubset();
    }

    private void loadInitialDataSubset()
    {
        goToProjectHome();
        clickButton("Process and Import Data");
        _fileBrowserHelper.selectFileBrowserItem("/txt/summary.txt");
        _fileBrowserHelper.selectImportDataAction("Import MaxQuant Results");
        waitForPipelineJobsToComplete(1, "MaxQuant import - summary.txt", false);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
    }

    @Test
    public void testDetailsPages()
    {
        DataRegionTable experimentGroupTable = new DataRegionTable("ExperimentGroup", this);
        assertEquals("Unexpected number of experiment group rows", 1, experimentGroupTable.getDataRowCount());
        assertEquals("Unexpected experiment run folder name", "txt", experimentGroupTable.getDataAsText(0, "ExperimentGroup/FolderName"));
        assertEquals("Unexpected experiment run file name", "summary.txt", experimentGroupTable.getDataAsText(0, "ExperimentGroup/Filename"));
        assertEquals("Unexpected experiment run protein groups count", ""+PROTEIN_GROUPS_COUNT, experimentGroupTable.getDataAsText(0, "ExperimentGroup/ProteinGroups"));
        assertEquals("Unexpected experiment run peptides count", ""+PEPTIDES_COUNT, experimentGroupTable.getDataAsText(0, "ExperimentGroup/Peptides"));

        log("Protein Groups for Experiment Group");
        clickAndWait(experimentGroupTable.link(0, "ExperimentGroup"));
        ExperimentGroupDetails experimentGroupDetails = new ExperimentGroupDetails(getDriver());
        assertTrue("Unexpected protein groups link", experimentGroupDetails.hasProteinGroupsLink(PROTEIN_GROUPS_COUNT));
        assertTrue("Unexpected peptides link", experimentGroupDetails.hasPeptidesLink(PEPTIDES_COUNT));
        assertTrue("Unexpected files links", experimentGroupDetails.hasFilesLinks(Arrays.asList("summary.txt", "proteinGroups.txt", "peptides.txt", "modificationSpecificPeptides.txt", "evidence.txt")));
        DataRegionTable proteinGroupsTable = experimentGroupDetails.getProteinGroupsGrid();
        assertEquals("Unexpected number of protein group rows", 51, proteinGroupsTable.getDataRowCount());
        proteinGroupsTable.setFilter("MaxQuantId", "Equals", "2900");
        assertEquals("Unexpected number of protein group rows", 1, proteinGroupsTable.getDataRowCount());
        assertEquals("Unexpected protein name for protein group", "Fanconi anemia-associated protein of 100 kDa", proteinGroupsTable.getDataAsText(0, "ProteinNames"));
        assertEquals("Unexpected gene name for protein group", "FAAP100", proteinGroupsTable.getDataAsText(0, "GeneNames"));

        log("Experiment Details for Protein Group");
        pushLocation();
        proteinGroupsTable.clickRowDetails(0);
        ProteinGroupDetails proteinGroupDetails = validateProteinGroupDetails();
        assertTrue("Unexpected files links", proteinGroupDetails.hasFilesLinks(Arrays.asList("proteinGroups.txt")));
        assertEquals("Unexpected grid row count: IntensityAndCoverage", 3, proteinGroupDetails.getIntensityAndCoverageGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: SilacRatios", 0, proteinGroupDetails.getSilacRatiosGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: SilacIntensities", 0, proteinGroupDetails.getSilacIntensitiesGrid().getDataRowCount());

        log("Peptides for Protein Group");
        popLocation();
        experimentGroupDetails = new ExperimentGroupDetails(getDriver());
        proteinGroupsTable = experimentGroupDetails.getProteinGroupsGrid();
        clickAndWait(proteinGroupsTable.link(0, "PeptideCount"));
        proteinGroupDetails = validateProteinGroupDetails();
        assertTrue("Unexpected files links", proteinGroupDetails.hasFilesLinks(Arrays.asList("peptides.txt")));
        DataRegionTable peptidesTable = proteinGroupDetails.getPeptidesGrid();
        assertEquals("Unexpected grid row count: Peptides", 3, peptidesTable.getDataRowCount());
        peptidesTable.setFilter("PeptideId", "Equals", "APSPLGPTRDPVATFLETCREPGSQPAGPASLR");
        assertEquals("Unexpected grid row count: Peptides", 1, peptidesTable.getDataRowCount());

        log("Evidence for Peptide");
        clickAndWait(peptidesTable.link(0, "EvidenceCount"));
        PeptideDetails peptideDetails = new PeptideDetails(getDriver());
        assertEquals("Unexpected peptide details value: Sequence", "APSPLGPTRDPVATFLETCREPGSQPAGPASLR", peptideDetails.getSequence());
        assertEquals("Unexpected peptide details value: Length", "33", peptideDetails.getLength());
        assertEquals("Unexpected peptide details value: Mass", "3,431.72560", peptideDetails.getMass());
        assertEquals("Unexpected peptide details value: Start Position", "514", peptideDetails.getStartPosition());
        assertEquals("Unexpected peptide details value: End Position", "546", peptideDetails.getEndPosition());
        assertEquals("Unexpected peptide details value: Missed Cleavages", "2", peptideDetails.getMissedCleavages());
        assertEquals("Unexpected grid row count: Evidence", 4, peptideDetails.getEvidenceGrid().getDataRowCount());
    }

    private ProteinGroupDetails validateProteinGroupDetails()
    {
        ProteinGroupDetails proteinGroupDetails = new ProteinGroupDetails(getDriver());
        assertElementPresent(Locator.tagWithText("td", "Q0VG06-3, Q0VG06, Q0VG06-2"), 1);
        assertElementPresent(Locator.tagWithText("td", "Q0VG06-3, Q0VG06"), 1);
        assertTrue("Unexpected number of protein id links", proteinGroupDetails.hasProteinIdLink("Q0VG06-3", 1));
        assertTrue("Unexpected number of protein id links", proteinGroupDetails.hasProteinIdLink("Q0VG06", 3));
        return proteinGroupDetails;
    }

    @Test
    public void testProteinSearch()
    {
        // Search for Protein Ids
        proteinSearch("Q58", true, 0);
        proteinSearch("Q58", false, 3);
        proteinSearch("Q9ULS5", false, 1);

        // Search for Protein Names
        proteinSearch("Heat shock protein HSP 90", true, 0);
        proteinSearch("Heat shock protein HSP 90", false, 3);
        proteinSearch("Heat shock protein HSP 90-beta", true, 1);
        proteinSearch("Heat shock protein HSP 91", false, 0);

        // Search for Gene Names
        proteinSearch("DNA", true, 0);
        proteinSearch("DNA", false, 2);
        proteinSearch("DNA repair protein XRCC1", true, 1);
    }

    @Test
    public void testMQSchemaTableSummaryStats()
    {
        // TODO check row counts and summary stats for all mq schema tables
    }

    private void proteinSearch(String value, boolean exactMatch, int expectedCount)
    {
        goToProjectHome();
        setFormElement(Locator.id("identifierInput"), value);
        if (!exactMatch)
            uncheckCheckbox(Locator.checkboxByName("exactMatch"));
        clickButton("Search");

        DataRegionTable mqMatches = new DataRegionTable("MqMatches", this);
        assertEquals("Unexpected protein search results", expectedCount, mqMatches.getDataRowCount());
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "MaxQuantImportTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("MaxQuantPrivate");
    }
}