CREATE TABLE property_repository (
    id BIGINT NOT NULL,
    PRIMARY KEY ( id )
)

CREATE TABLE repository_properties (
    repository_id BIGINT NOT NULL,
    property_type VARCHAR(255),
    property_id BIGINT NOT NULL
)