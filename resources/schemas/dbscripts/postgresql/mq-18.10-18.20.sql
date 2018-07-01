/*
 * Copyright (c) 2015 LabKey Corporation
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
/* mq-18.10-18.11.sql */

-- Do nothing as this is a placeholder to be in sync with sqlserver scripts.
;

/* mq-18.11-18.12.sql */

ALTER TABLE mq.Evidence ALTER COLUMN ModifiedPeptideId DROP NOT NULL;
ALTER TABLE mq.Evidence ALTER COLUMN MassErrorPpm DROP NOT NULL;
ALTER TABLE mq.Evidence ALTER COLUMN UncalibratedMassErrorPpm DROP NOT NULL;

/* mq-18.12-18.13.sql */

CREATE TABLE mq.TMTChannel
(
  Id SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  ExperimentGroupId INT NOT NULL,
  TagNumber INT NOT NULL,

  CONSTRAINT PK_TMTChannel PRIMARY KEY (Id),
  CONSTRAINT FK_TMTChannel_ExperimentGroupId FOREIGN KEY (ExperimentGroupId) REFERENCES mq.ExperimentGroup (Id),
  CONSTRAINT UQ_TMTChannel UNIQUE (ExperimentGroupId, TagNumber)
);
CREATE INDEX IX_TMTChannel_ExperimentGroupId ON mq.TMTChannel (ExperimentGroupId);


CREATE TABLE mq.ProteinGroupTMT
(
  Id SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  ProteinGroupId INT NOT NULL,
  TMTChannelId INT NOT NULL,
  ExperimentId INT, -- allow null
  ReporterIntensity DOUBLE PRECISION NOT NULL,
  ReporterIntensityCorrected DOUBLE PRECISION NOT NULL,
  ReporterIntensityCount INT NOT NULL,

  CONSTRAINT PK_ProteinGroupTMT PRIMARY KEY (Id),
  CONSTRAINT FK_ProteinGroupTMT_ProteinGroupId FOREIGN KEY (ProteinGroupId) REFERENCES mq.ProteinGroup (Id),
  CONSTRAINT FK_ProteinGroupTMT_TMTChannelId FOREIGN KEY (TMTChannelId) REFERENCES mq.TMTChannel (Id),
  CONSTRAINT FK_ProteinGroupTMT_ExperimentId FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment (Id),
  CONSTRAINT UQ_ProteinGroupTMT UNIQUE (ProteinGroupId, TMTChannelId, ExperimentId)
);
CREATE INDEX IX_ProteinGroupTMT_ProteinGroupId ON mq.ProteinGroupTMT (ProteinGroupId);
CREATE INDEX IX_ProteinGroupTMT_TMTChannelId ON mq.ProteinGroupTMT (TMTChannelId);
CREATE INDEX IX_ProteinGroupTMT_ExperimentId ON mq.ProteinGroupTMT (ExperimentId);


CREATE TABLE mq.PeptideTMT
(
  Id SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  PeptideId INT NOT NULL,
  TMTChannelId INT NOT NULL,
  ExperimentId INT, -- allow null
  ReporterIntensity DOUBLE PRECISION NOT NULL,
  ReporterIntensityCorrected DOUBLE PRECISION NOT NULL,
  ReporterIntensityCount INT NOT NULL,

  CONSTRAINT PK_PeptideTMT PRIMARY KEY (Id),
  CONSTRAINT FK_PeptideTMT_PeptideId FOREIGN KEY (PeptideId) REFERENCES mq.Peptide (Id),
  CONSTRAINT FK_PeptideTMT_TMTChannelId FOREIGN KEY (TMTChannelId) REFERENCES mq.TMTChannel (Id),
  CONSTRAINT FK_PeptideTMT_ExperimentId FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment (Id),
  CONSTRAINT UQ_PeptideTMT UNIQUE (PeptideId, TMTChannelId, ExperimentId)
);
CREATE INDEX IX_PeptideTMT_PeptideId ON mq.PeptideTMT (PeptideId);
CREATE INDEX IX_PeptideTMT_TMTChannelId ON mq.PeptideTMT (TMTChannelId);
CREATE INDEX IX_PeptideTMT_ExperimentId ON mq.PeptideTMT (ExperimentId);


CREATE TABLE mq.EvidenceTMT
(
  Id SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  EvidenceId INT NOT NULL,
  TMTChannelId INT NOT NULL,
  ExperimentId INT, -- allow null
  ReporterIntensity DOUBLE PRECISION NOT NULL,
  ReporterIntensityCorrected DOUBLE PRECISION NOT NULL,
  ReporterIntensityCount INT NOT NULL,

  CONSTRAINT PK_EvidenceTMT PRIMARY KEY (Id),
  CONSTRAINT FK_EvidenceTMT_EvidenceId FOREIGN KEY (EvidenceId) REFERENCES mq.Evidence (Id),
  CONSTRAINT FK_EvidenceTMT_TMTChannelId FOREIGN KEY (TMTChannelId) REFERENCES mq.TMTChannel (Id),
  CONSTRAINT FK_EvidenceTMT_ExperimentId FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment (Id),
  CONSTRAINT UQ_EvidenceTMT UNIQUE (EvidenceId, TMTChannelId, ExperimentId)
);
CREATE INDEX IX_EvidenceTMT_EvidenceId ON mq.EvidenceTMT (EvidenceId);
CREATE INDEX IX_EvidenceTMT_TMTChannelId ON mq.EvidenceTMT (TMTChannelId);
CREATE INDEX IX_EvidenceTMT_ExperimentId ON mq.EvidenceTMT (ExperimentId);


CREATE TABLE mq.ModifiedPeptideTMT
(
  Id SERIAL NOT NULL,
  Container ENTITYID NOT NULL,
  ModifiedPeptideId INT NOT NULL,
  TMTChannelId INT NOT NULL,
  ExperimentId INT, -- allow null
  ReporterIntensity DOUBLE PRECISION NOT NULL,
  ReporterIntensityCorrected DOUBLE PRECISION NOT NULL,
  ReporterIntensityCount INT NOT NULL,

  CONSTRAINT PK_ModifiedPeptideTMT PRIMARY KEY (Id),
  CONSTRAINT FK_ModifiedPeptideTMT_ModifiedPeptideId FOREIGN KEY (ModifiedPeptideId) REFERENCES mq.ModifiedPeptide (Id),
  CONSTRAINT FK_ModifiedPeptideTMT_TMTChannelId FOREIGN KEY (TMTChannelId) REFERENCES mq.TMTChannel (Id),
  CONSTRAINT FK_ModifiedPeptideTMT_ExperimentId FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment (Id),
  CONSTRAINT UQ_ModifiedPeptideTMT UNIQUE (ModifiedPeptideId, TMTChannelId, ExperimentId)
);
CREATE INDEX IX_ModifiedPeptideTMT_ModifiedPeptideId ON mq.ModifiedPeptideTMT (ModifiedPeptideId);
CREATE INDEX IX_ModifiedPeptideTMT_TMTChannelId ON mq.ModifiedPeptideTMT (TMTChannelId);
CREATE INDEX IX_ModifiedPeptideTMT_ExperimentId ON mq.ModifiedPeptideTMT (ExperimentId);

/* mq-18.13-18.14.sql */

DROP INDEX mq.IX_TMTChannel_ExperimentGroupId;
DROP INDEX mq.IX_ProteinGroupTMT_ProteinGroupId;
DROP INDEX mq.IX_PeptideTMT_PeptideId;
DROP INDEX mq.IX_EvidenceTMT_EvidenceId;
DROP INDEX mq.IX_ModifiedPeptideTMT_ModifiedPeptideId;