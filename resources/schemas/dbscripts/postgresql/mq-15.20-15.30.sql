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

/* mq-15.20-15.21.sql */

CREATE SCHEMA mq;

-- ExperimentGroup table
CREATE TABLE mq.ExperimentGroup
(
    _ts TIMESTAMP DEFAULT now(),
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    DataId INT,
    EntityId ENTITYID NOT NULL,
    Filename VARCHAR(300),
    Description VARCHAR(300),
    StatusId INT NOT NULL DEFAULT 0,
    Status VARCHAR(200),
    Deleted Boolean NOT NULL DEFAULT false,
    ExperimentRunLsid lsidtype,

    LocationOnFileSystem VARCHAR(500),

    CONSTRAINT PK_ExperimentGroup PRIMARY KEY (Id)
);

-- Experiment table
CREATE TABLE mq.Experiment
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ExperimentGroupId INT NOT NULL,
    ExperimentName VARCHAR(300),

    CONSTRAINT PK_Experiment PRIMARY KEY (Id),
    CONSTRAINT FK_Experiment_ExperimentGroup FOREIGN KEY (ExperimentGroupId) REFERENCES mq.ExperimentGroup(Id)
);
CREATE INDEX IX_Experiment_ExperimentGroupId ON mq.Experiment (ExperimentGroupId);

-- RawFile table
CREATE TABLE mq.RawFile
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ExperimentId INT NOT NULL,
    Name VARCHAR(300),
    Fraction VARCHAR(100),

    CONSTRAINT PK_RawFile PRIMARY KEY (Id),
    CONSTRAINT FK_RawFile_Experiment FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment(Id)
);
CREATE INDEX IX_RawFile_ExperimentId ON mq.RawFile (ExperimentId);

-- ProteinGroup table
CREATE TABLE mq.ProteinGroup
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ExperimentGroupId INT NOT NULL,

    MaxQuantId INT NOT NULL,
    ProteinIds TEXT NOT NULL,
    MajorityProteinIds TEXT,
    ProteinNames TEXT,
    GeneNames TEXT,
    FastaHeaders TEXT,
    ProteinCount INT NOT NULL,
    PeptideCount INT NOT NULL,
    UniqPeptideCount INT NOT NULL,
    RazorUniqPeptideCount INT NOT NULL,
    SequenceCoverage REAL NOT NULL,
    Score Double Precision NOT NULL,
    Intensity BIGINT NOT NULL,
    MS2Count INT,
    IdentifiedBySite BOOLEAN,
    Decoy BOOLEAN,
    Contaminant BOOLEAN,

    CONSTRAINT PK_ProteinGroup PRIMARY KEY (Id),
    CONSTRAINT FK_ProteinGroup_ExperimentGroup FOREIGN KEY (ExperimentGroupId) REFERENCES mq.ExperimentGroup(Id)
);
CREATE INDEX IX_ProteinGroup_ExperimentGroupId ON mq.ProteinGroup (ExperimentGroupId);

-- ProteinGroupExperimentInfo table
CREATE TABLE mq.ProteinGroupExperimentInfo
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ProteinGroupId INT NOT NULL,
    ExperimentId INT NOT NULL,
    Coverage REAL NOT NULL,
    Intensity BIGINT NOT NULL,
    LfqIntensity BIGINT,

    CONSTRAINT PK_ProteinGroupExperimentInfo PRIMARY KEY (Id),
    CONSTRAINT FK_ProteinGroupExperimentInfo_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES mq.ProteinGroup(Id),
    CONSTRAINT FK_ProteinGroupExperimentInfo_Experiment FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment(Id)
);
CREATE INDEX IX_ProteinGroupExperimentInfo_ExperimentId ON mq.ProteinGroupExperimentInfo (ExperimentId);
CREATE INDEX IX_ProteinGroupExperimentInfo_ProteinGroupId ON mq.ProteinGroupExperimentInfo (ProteinGroupId);

-- ProteinGroupIntensitySilac table
CREATE TABLE mq.ProteinGroupIntensitySilac
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ProteinGroupId INT NOT NULL,
    ExperimentId INT NOT NULL,
    LabelType CHAR(1) NOT NULL,
    Intensity BIGINT NOT NULL,

    CONSTRAINT PK_ProteinGroupIntensitySilac PRIMARY KEY (Id),
    CONSTRAINT FK_ProteinGroupIntensitySilac_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES mq.ProteinGroup(Id),
    CONSTRAINT FK_ProteinGroupIntensitySilac_Experiment FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment(Id)
);
CREATE INDEX IX_ProteinGroupIntensitySilac_ExperimentId ON mq.ProteinGroupIntensitySilac (ExperimentId);
CREATE INDEX IX_ProteinGroupIntensitySilac_ProteinGroupId ON mq.ProteinGroupIntensitySilac (ProteinGroupId);

-- ProteinGroupRatiosSilac table
CREATE TABLE mq.ProteinGroupRatiosSilac
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ProteinGroupId INT NOT NULL,
    ExperimentId INT NOT NULL,
    RatioType CHAR(3) NOT NULL,
    Ratio DOUBLE PRECISION,
    RatioNormalized DOUBLE PRECISION,
    RatioCount INT,

    CONSTRAINT PK_ProteinGroupRatiosSilac PRIMARY KEY (Id),
    CONSTRAINT FK_ProteinGroupRatiosSilac_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES mq.ProteinGroup(Id),
    CONSTRAINT FK_ProteinGroupRatiosSilac_Experiment FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment(Id)
);
CREATE INDEX IX_ProteinGroupRatiosSilac_ExperimentId ON mq.ProteinGroupRatiosSilac (ExperimentId);
CREATE INDEX IX_ProteinGroupRatiosSilac_ProteinGroupId ON mq.ProteinGroupRatiosSilac (ProteinGroupId);

CREATE TABLE mq.Peptide
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    ExperimentGroupId INT NOT NULL,

    MaxQuantId INT NOT NULL,
    Sequence VARCHAR(500) NOT NULL,
    Length INT NOT NULL,
    StartPosition INT,
    EndPosition INT,
    MissedCleavages INT NOT NULL,
    Mass DOUBLE PRECISION NOT NULL,


    CONSTRAINT PK_Peptide PRIMARY KEY (Id),
    CONSTRAINT FK_Peptide_ExperimentGroup FOREIGN KEY (ExperimentGroupId) REFERENCES mq.ExperimentGroup(Id)
);
CREATE INDEX IX_Peptide_ExperimentGroupId ON mq.Peptide (ExperimentGroupId);


CREATE TABLE mq.ProteinGroupPeptide
(
    ProteinGroupId INT NOT NULL,
    PeptideId INT NOT NULL,

    CONSTRAINT PK_ProteinGroupPeptide PRIMARY KEY (ProteinGroupId, PeptideId),
    CONSTRAINT FK_ProteinGroupPeptide_Peptide FOREIGN KEY (PeptideId) REFERENCES mq.Peptide(Id),
    CONSTRAINT FK_ProteinGroupPeptide_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES mq.ProteinGroup(Id)
);
CREATE INDEX IX_ProteinGroupPeptide_ProteinGroupId ON mq.ProteinGroupPeptide (ProteinGroupId);
CREATE INDEX IX_ProteinGroupPeptide_PeptideId ON mq.ProteinGroupPeptide (PeptideId);

CREATE TABLE mq.ModifiedPeptide
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    PeptideId INT NOT NULL,

    MaxQuantId INT NOT NULL,
    Sequence VARCHAR(500),
    Modifications VARCHAR(500) NOT NULL,
    Mass DOUBLE PRECISION NOT NULL,


    CONSTRAINT PK_ModifiedPeptide PRIMARY KEY (Id),
    CONSTRAINT FK_ModifiedPeptide_Peptide FOREIGN KEY (PeptideId) REFERENCES mq.Peptide(Id)
);
CREATE INDEX IX_ModifiedPeptide_PeptideId ON mq.ModifiedPeptide (PeptideId);

CREATE TABLE mq.Evidence
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    PeptideId INT NOT NULL,
    ModifiedPeptideId INT NOT NULL,
    ExperimentId INT NOT NULL,
    RawFileId INT NOT NULL,

    MaxQuantId INT NOT NULL,
    MsmsMz DOUBLE PRECISION,
    Charge INT NOT NULL,
    MassErrorPpm DOUBLE PRECISION NOT NULL,
    UncalibratedMassErrorPpm DOUBLE PRECISION NOT NULL,
    RetentionTime REAL NOT NULL,
    Pep DOUBLE PRECISION,
    MsmsCount INT NOT NULL,
    ScanNumber INT,
    Score DOUBLE PRECISION,
    DeltaScore DOUBLE PRECISION,
    Intensity BIGINT,
    MaxQuantMsmsIds TEXT,
    MaxQuantBestMsmsId INT,



    CONSTRAINT PK_Evidence PRIMARY KEY (Id),
    CONSTRAINT FK_Evidence_Peptide FOREIGN KEY (PeptideId) REFERENCES mq.Peptide(Id),
    CONSTRAINT FK_Evidence_ModifiedPeptide FOREIGN KEY (ModifiedPeptideId) REFERENCES mq.ModifiedPeptide(Id),
    CONSTRAINT FK_Evidence_Experiment FOREIGN KEY (ExperimentId) REFERENCES mq.Experiment(Id),
    CONSTRAINT FK_Evidence_RawFile FOREIGN KEY (RawFileId) REFERENCES mq.RawFile(Id)
);
CREATE INDEX IX_Evidence_PeptideId ON mq.Evidence (PeptideId);
CREATE INDEX IX_Evidence_ModifiedPeptideId ON mq.Evidence (ModifiedPeptideId);
CREATE INDEX IX_Evidence_ExperimentId ON mq.Evidence (ExperimentId);
CREATE INDEX IX_Evidence_RawFileId ON mq.Evidence (RawFileId);


-- EvidenceIntensitySilac table
CREATE TABLE mq.EvidenceIntensitySilac
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    EvidenceId INT NOT NULL,
    Labeltype CHAR(1) NOT NULL,
    Intensity BIGINT NOT NULL,

    CONSTRAINT PK_EvidenceIntensitySilac PRIMARY KEY (Id),
    CONSTRAINT FK_EvidenceIntensitySilac_Evidence FOREIGN KEY (EvidenceId) REFERENCES mq.Evidence(Id)
);
CREATE INDEX IX_EvidenceIntensitySilac_EvidenceId ON mq.EvidenceIntensitySilac (EvidenceId);

-- EvidenceRatioSilac table
CREATE TABLE mq.EvidenceRatioSilac
(
    Id SERIAL NOT NULL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Container ENTITYID NOT NULL,

    EvidenceId INT NOT NULL,
    RatioType CHAR(3) NOT NULL,
    Ratio DOUBLE PRECISION NOT NULL,
    RatioNormalized DOUBLE PRECISION NOT NULL,

    CONSTRAINT PK_EvidenceRatioSilac PRIMARY KEY (Id),
    CONSTRAINT FK_EvidenceRatioSilac_Evidence FOREIGN KEY (EvidenceId) REFERENCES mq.Evidence(Id)
);
CREATE INDEX IX_EvidenceRatioSilac_EvidenceId ON mq.EvidenceRatioSilac (EvidenceId);