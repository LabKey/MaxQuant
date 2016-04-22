
SELECT
e.EvidenceId,
e.RatioType AS RatioType,
MAX(e.Ratio) AS Ratio,
MAX(e.RatioNormalized) AS RatioNormalized
FROM EvidenceRatioSilac AS e
GROUP BY e.EvidenceId, e.RatioType
PIVOT Ratio, RatioNormalized  BY RatioType