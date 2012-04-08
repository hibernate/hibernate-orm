package org.hibernate.envers.test.entities.reventity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity
public class OracleRevisionEntity extends AbstractOracleRevisionEntity {
    public String toString() {
        return "OracleRevisionEntity(" + super.toString() + ")";
    }
}
