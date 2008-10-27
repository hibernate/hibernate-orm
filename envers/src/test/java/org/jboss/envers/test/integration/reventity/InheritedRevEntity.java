package org.jboss.envers.test.integration.reventity;

import org.jboss.envers.RevisionEntity;
import org.jboss.envers.DefaultRevisionEntity;

import javax.persistence.Entity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity
public class InheritedRevEntity extends DefaultRevisionEntity {
    
}
