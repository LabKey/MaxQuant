
SELECT
  PeptideId,
  ExperimentId,
  TMTChannelId.TagNumber AS TMTChannel,
  MAX(ReporterIntensity) AS ReporterIntensity,
  MAX(ReporterIntensityCorrected) AS ReporterIntensityCorrected,
  MAX(ReporterIntensityCount) AS ReporterIntensityCount
FROM PeptideTMT
GROUP BY PeptideId, ExperimentId, TMTChannelId.TagNumber
PIVOT ReporterIntensity, ReporterIntensityCorrected, ReporterIntensityCount BY TMTChannel
