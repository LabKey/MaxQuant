
SELECT c.ProteinGroupId, c.ExperimentId, i.Intensity, c.Coverage
FROM ProteinGroupSequenceCoverage AS c
FULL OUTER JOIN ProteinGroupIntensity AS i ON (i.ExperimentId = c.ExperimentId AND i.ProteinGroupId = c.ProteinGroupId)

