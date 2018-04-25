
SELECT
  ModifiedPeptideId,
  ExperimentId,
  TMTChannelId.TagNumber AS TMTChannel,
  MAX(ReporterIntensity) AS ReporterIntensity,
  MAX(ReporterIntensityCorrected) AS ReporterIntensityCorrected,
  MAX(ReporterIntensityCount) AS ReporterIntensityCount
FROM ModifiedPeptideTMT
GROUP BY ModifiedPeptideId, ExperimentId, TMTChannelId.TagNumber
PIVOT ReporterIntensity, ReporterIntensityCorrected, ReporterIntensityCount BY TMTChannel
