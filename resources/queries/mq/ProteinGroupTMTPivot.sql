
SELECT
  ProteinGroupId,
  ExperimentId,
  TMTChannelId.TagNumber AS TMTChannel,
  MAX(ReporterIntensity) AS ReporterIntensity,
  MAX(ReporterIntensityCorrected) AS ReporterIntensityCorrected,
  MAX(ReporterIntensityCount) AS ReporterIntensityCount
FROM ProteinGroupTMT
GROUP BY ProteinGroupId, ExperimentId, TMTChannelId.TagNumber
PIVOT ReporterIntensity, ReporterIntensityCorrected, ReporterIntensityCount BY TMTChannel
