package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.test.entities.reventity.AbstractOracleRevisionEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "TrackModifiedEntitiesRevInfo")
@RevisionEntity
public class OracleTrackingModifiedEntitiesRevisionEntity extends AbstractOracleTrackingModifiedEntitiesRevisionEntity {
    public String toString() {
        return "OracleTrackingModifiedEntitiesRevisionEntity(" + super.toString() + ")";
    }
}