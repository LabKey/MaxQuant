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
package org.labkey.test.tests.maxquantprivate;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.util.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Git;
import org.labkey.test.pages.maxquantprivate.ExperimentGroupDetails;
import org.labkey.test.pages.maxquantprivate.PeptideDetails;
import org.labkey.test.pages.maxquantprivate.ProteinGroupDetails;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.SummaryStatisticsHelper;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        log("Experiment Runs (Imported MaxQuant Results) ");
        DataRegionTable experimentGroupTable = new DataRegionTable("ExperimentGroups", this);
        assertEquals("Unexpected number of experiment group rows", 1, experimentGroupTable.getDataRowCount());
        //assertEquals("Unexpected experiment run folder name", "txt", experimentGroupTable.getDataAsText(0, "ExperimentGroup/FolderName"));
        assertEquals("Unexpected experiment run file name", "summary.txt", experimentGroupTable.getDataAsText(0, "FileName"));
        assertEquals("Unexpected experiment run protein groups count", ""+PROTEIN_GROUPS_COUNT, experimentGroupTable.getDataAsText(0, "ProteinGroups"));
        assertEquals("Unexpected experiment run peptides count", ""+PEPTIDES_COUNT, experimentGroupTable.getDataAsText(0, "Peptides"));

        log("Protein Groups for Experiment Group");
        clickAndWait(experimentGroupTable.link(0, "Id"));
        ExperimentGroupDetails experimentGroupDetails = new ExperimentGroupDetails(getDriver());
        assertEquals("Unexpected protein groups link", PROTEIN_GROUPS_COUNT, experimentGroupDetails.getProteinGroupsCount());
        assertEquals("Unexpected peptides link", PEPTIDES_COUNT, experimentGroupDetails.getPeptidesCount());
        assertEquals("Unexpected files links", Arrays.asList("summary.txt", "proteinGroups.txt", "peptides.txt", "modificationSpecificPeptides.txt", "evidence.txt"), getTexts(experimentGroupDetails.getExperimentFileDownloadLinks()));
        DataRegionTable proteinGroupsTable = experimentGroupDetails.getProteinGroupsGrid();
        assertEquals("Unexpected number of protein group rows", 51, proteinGroupsTable.getDataRowCount());
        proteinGroupsTable.setFilter("MaxQuantId", "Equals", "2900");
        assertEquals("Unexpected number of protein group rows", 1, proteinGroupsTable.getDataRowCount());
        assertEquals("Unexpected protein name for protein group", "Fanconi anemia-associated protein of 100 kDa", proteinGroupsTable.getDataAsText(0, "ProteinNames"));
        assertEquals("Unexpected gene name for protein group", "FAAP100", proteinGroupsTable.getDataAsText(0, "GeneNames"));

        log("Experiment Details for Protein Group");
        pushLocation();
        proteinGroupsTable.clickRowDetails(0);
        ProteinGroupDetails proteinGroupDetails = validateProteinGroupDetails(3);
        assertTrue("Missing download link for: proteinGroups.txt", proteinGroupDetails.hasFilesLinks(Arrays.asList("proteinGroups.txt")));
        assertEquals("Unexpected grid row count: IntensityAndCoverage", 3, proteinGroupDetails.getIntensityAndCoverageGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: SilacRatios", 0, proteinGroupDetails.getSilacRatiosGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: SilacIntensities", 0, proteinGroupDetails.getSilacIntensitiesGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: TMT", 4, proteinGroupDetails.getProteinGroupTMTPivotGrid().getDataRowCount());

        log("Peptides for Protein Group");
        popLocation();
        experimentGroupDetails = new ExperimentGroupDetails(getDriver());
        proteinGroupsTable = experimentGroupDetails.getProteinGroupsGrid();
        clickAndWait(proteinGroupsTable.link(0, "PeptideCount"));
        proteinGroupDetails = validateProteinGroupDetails(0);
        assertTrue("Missing download link for: peptides.txt", proteinGroupDetails.hasFilesLinks(Arrays.asList("peptides.txt")));
        DataRegionTable peptidesTable = proteinGroupDetails.getPeptidesGrid();
        assertEquals("Unexpected grid row count: Peptides", 3, peptidesTable.getDataRowCount());
        peptidesTable.setFilter("PeptideId", "Equals", "APSPLGPTRDPVATFLETCREPGSQPAGPASLR");
        assertEquals("Unexpected grid row count: Peptides", 1, peptidesTable.getDataRowCount());

        log("Evidence for Peptide");
        clickAndWait(peptidesTable.link(0, "EvidenceCount"));
        PeptideDetails peptideDetails = new PeptideDetails(getDriver());
        assertEquals("Unexpected peptide details value: Sequence", "APSPLGPTRDPVATFLETCREPGSQPAGPASLR", peptideDetails.getSequence());
        assertEquals("Unexpected peptide details value: Length", "33", peptideDetails.getLength());
        assertEquals("Unexpected peptide details value: Mass", "3,431.7256", peptideDetails.getMass());
        assertEquals("Unexpected peptide details value: Start Position", "514", peptideDetails.getStartPosition());
        assertEquals("Unexpected peptide details value: End Position", "546", peptideDetails.getEndPosition());
        assertEquals("Unexpected peptide details value: Missed Cleavages", "2", peptideDetails.getMissedCleavages());
        assertEquals("Unexpected grid row count: Evidence", 4, peptideDetails.getEvidenceGrid().getDataRowCount());
        assertEquals("Unexpected grid row count: Evidence", 4, peptideDetails.getPeptideTMTPivotGrid().getDataRowCount());
    }

    private ProteinGroupDetails validateProteinGroupDetails(int countFromPivotGrid)
    {
        ProteinGroupDetails proteinGroupDetails = new ProteinGroupDetails(getDriver());
        assertElementPresent(Locator.tagWithText("td", "Q0VG06-3, Q0VG06, Q0VG06-2"), 1);
        assertElementPresent(Locator.tagWithText("td", "Q0VG06-3, Q0VG06"), 1);
        assertTrue("Unexpected number of protein id links", proteinGroupDetails.hasProteinIdLink("Q0VG06-3", 1 + countFromPivotGrid));
        assertTrue("Unexpected number of protein id links", proteinGroupDetails.hasProteinIdLink("Q0VG06", 3 + countFromPivotGrid));
        return proteinGroupDetails;
    }

    @Test
    public void testProteinSearch()
    {
        log("Search for Protein Ids");
        proteinSearch("Q58", true, 0);
        proteinSearch("Q58", false, 3);
        proteinSearch("Q9ULS5", false, 1);

        log("Search for Protein Names");
        proteinSearch("Heat shock protein HSP 90", true, 0);
        proteinSearch("Heat shock protein HSP 90", false, 3);
        proteinSearch("Heat shock protein HSP 90-beta", true, 1);
        proteinSearch("Heat shock protein HSP 91", false, 0);

        log("Search for Gene Names");
        proteinSearch("DNA", true, 0);
        proteinSearch("DNA", false, 2);
        proteinSearch("DNA repair protein XRCC1", true, 1);
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

    @Test
    public void testMQSchemaTableSummaryStats()
    {
        log("ProteinGroup: row count and numeric column stats validation");
        Map<String, Pair<String, String>> columnStats = new HashMap<>();
        columnStats.put("ProteinCount", new Pair<>("125", "2.451"));
        columnStats.put("PeptideCount", new Pair<>("366", "7.176"));
        columnStats.put("UniqPeptideCount", new Pair<>("299", "5.863"));
        columnStats.put("RazorUniqPeptideCount", new Pair<>("336", "6.588"));
        columnStats.put("SequenceCoverage", new Pair<>("751.70", "14.74"));
        columnStats.put("Score", new Pair<>("4,472.1424", "87.6891"));
        columnStats.put("Intensity", new Pair<>("107,040,227,800", "2,098,827,996.078"));
        columnStats.put("MS2Count", new Pair<>("811", "15.902"));
        verifyQueryRowCountAndColumnStats("ProteinGroup", 51, columnStats);

        log("Peptide: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("Length", new Pair<>("5,444", "16.154"));
        columnStats.put("Mass", new Pair<>("607,162.8796", "1,801.6703"));
        columnStats.put("StartPosition", new Pair<>("133,637", "400.111"));
        columnStats.put("EndPosition", new Pair<>("138,717", "415.32"));
        columnStats.put("MissedCleavages", new Pair<>("158", "0.469"));
        verifyQueryRowCountAndColumnStats("Peptide", 337, columnStats);

        log("ModifiedPeptide: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("Mass", new Pair<>("759,046.5560", "1,916.7842"));
        verifyQueryRowCountAndColumnStats("ModifiedPeptide", 396, columnStats);

        log("Evidence: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("MsmsMz", new Pair<>("1,315,255.1001", "770.5068"));
        columnStats.put("Charge", new Pair<>("5,676", "3.325"));
        columnStats.put("MassErrorPpm", new Pair<>("437,531,218.1343", "263,097.5455"));
        columnStats.put("UncalibratedMassErrorPpm", new Pair<>("437,534,439.0769", "263,099.4823"));
        columnStats.put("RetentionTime", new Pair<>(null, "90.5381")); // this column is of type REAL which makes the SUM inconsistent
        columnStats.put("Pep", new Pair<>("5.943", "0.003"));
        columnStats.put("MsmsCount", new Pair<>("2,074", "1.215"));
        columnStats.put("ScanNumber", new Pair<>("41,451,924", "24,283.494"));
        columnStats.put("Score", new Pair<>("179,817.83", "105.34"));
        columnStats.put("DeltaScore", new Pair<>("139,934.7538", "81.9770"));
        columnStats.put("Intensity", new Pair<>("493,456,488,930", "296,726,692.081"));
        verifyQueryRowCountAndColumnStats("Evidence", 1707, columnStats);

        log("ProteinGroupExperimentInfo: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("Coverage", new Pair<>(null, "8.49")); // this column is of type REAL which makes the SUM inconsistent
        columnStats.put("Intensity", new Pair<>("107,040,317,490", "699,609,918.235"));
        columnStats.put("LfqIntensity", new Pair<>("n/a", "n/a"));
        verifyQueryRowCountAndColumnStats("ProteinGroupExperimentInfo", 153, columnStats);

        log("ProteinGroupTMTPivot: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("0::ReporterIntensity", new Pair<>("3,117,272.85", "20,374.33"));
        columnStats.put("0::ReporterIntensityCorrected", new Pair<>("3,294,180.71", "21,530.59"));
        columnStats.put("0::ReporterIntensityCount", new Pair<>("782", "5.111"));
        verifyQueryRowCountAndColumnStats("ProteinGroupTMTPivot", 153, columnStats);

        log("PeptideTMTPivot: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("3::ReporterIntensity", new Pair<>("4,501,291.92", "4,452.32"));
        columnStats.put("3::ReporterIntensityCorrected", new Pair<>("4,402,597.13", "4,354.70"));
        columnStats.put("3::ReporterIntensityCount", new Pair<>("934", "0.924"));
        verifyQueryRowCountAndColumnStats("PeptideTMTPivot", 1011, columnStats);

        log("ModifiedPeptideTMTPivot: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("6::ReporterIntensity", new Pair<>("10,384,734.36", "8,741.36"));
        columnStats.put("6::ReporterIntensityCorrected", new Pair<>("10,197,450.74", "8,583.71"));
        columnStats.put("6::ReporterIntensityCount", new Pair<>("2,009", "1.691"));
        verifyQueryRowCountAndColumnStats("ModifiedPeptideTMTPivot", 1188, columnStats);

        log("EvidenceTMTPivot: row count and numeric column stats validation");
        columnStats = new HashMap<>();
        columnStats.put("9::ReporterIntensity", new Pair<>("1,972,389.32", "1,155.47"));
        columnStats.put("9::ReporterIntensityCorrected", new Pair<>("1,822,296.77", "1,067.54"));
        columnStats.put("9::ReporterIntensityCount", new Pair<>("1,836", "1.076"));
        verifyQueryRowCountAndColumnStats("EvidenceTMTPivot", 1707, columnStats);

        log("Other tables: row count validation");
        verifyQueryRowCountAndColumnStats("RawFile", 42, null);
        verifyQueryRowCountAndColumnStats("Experiment", 3, null);
        verifyQueryRowCountAndColumnStats("EvidenceIntensitySilac", 0, null);
        verifyQueryRowCountAndColumnStats("EvidenceRatioSilac", 0, null);
        verifyQueryRowCountAndColumnStats("ProteinGroupIntensitySilac", 0, null);
        verifyQueryRowCountAndColumnStats("ProteinGroupRatiosSilac", 0, null);
    }

    private void verifyQueryRowCountAndColumnStats(String tableName, int rowCount, Map<String, Pair<String, String>> columnStats)
    {
        goToMQTable(tableName);
        DataRegionTable drt = new DataRegionTable("query", getDriver());
        if (rowCount < 100)
            assertEquals("Unexpected grid row count: " + tableName, rowCount, drt.getDataRowCount());
        else if (rowCount < 1000)
            drt.assertPaginationText(1, 100, rowCount);
        // TODO issue with assertPaginationText for > 1,000

        if (columnStats != null)
        {
            for (Map.Entry<String, Pair<String, String>> entry : columnStats.entrySet())
            {
                if (entry.getValue().first != null)
                    drt.setSummaryStatistic(entry.getKey(), SummaryStatisticsHelper.BASE_STAT_SUM, entry.getValue().first);

                if (entry.getValue().second != null)
                    drt.setSummaryStatistic(entry.getKey(), SummaryStatisticsHelper.BASE_STAT_MEAN, entry.getValue().second);
            }
        }
    }

    private void goToMQTable(String tableName)
    {
        Map<String, String> params = new HashMap<>();
        params.put("schemaName", "mq");
        params.put("query.queryName", tableName);
        beginAt(WebTestHelper.buildURL("query", getCurrentContainerPath(), "executeQuery", params));
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