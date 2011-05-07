package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
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
