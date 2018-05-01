
SELECT
  EvidenceId,
  ExperimentId,
  TMTChannelId.TagNumber AS TMTChannel,
  MAX(ReporterIntensity) AS ReporterIntensity,
  MAX(ReporterIntensityCorrected) AS ReporterIntensityCorrected,
  MAX(ReporterIntensityCount) AS ReporterIntensityCount
FROM EvidenceTMT
GROUP BY EvidenceId, ExperimentId, TMTChannelId.TagNumber
PIVOT ReporterIntensity, ReporterIntensityCorrected, ReporterIntensityCount BY TMTChannel
