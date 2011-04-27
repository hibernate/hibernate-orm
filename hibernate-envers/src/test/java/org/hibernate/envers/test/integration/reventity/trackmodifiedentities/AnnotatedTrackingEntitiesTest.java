package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
    @Override
    public void configure(Ejb3Configuration cfg) {
        super.configure(cfg);
        cfg.addAnnotatedClass(AnnotatedTrackingRevisionEntity.class);
        cfg.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "false");
    }
}
