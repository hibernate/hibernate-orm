package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.ExtendedRevisionEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.ExtendedRevisionListener;

/**
 * Tests proper behavior of revision entity that extends {@link DefaultTrackingModifiedEntitiesRevisionEntity}.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ExtendedRevisionEntityTest extends DefaultTrackingEntitiesTest {
    @Override
    public void configure(Ejb3Configuration cfg) {
        super.configure(cfg);
        cfg.addAnnotatedClass(ExtendedRevisionEntity.class);
    }

    @Override
    public void addConfigOptions(Map configuration) {
        super.addConfigOptions(configuration);
        configuration.put("org.hibernate.envers.track_entities_changed_in_revision", "false");
    }

    @Test
    public void testCommentPropertyValue() {
        ExtendedRevisionEntity ere = getAuditReader().findRevision(ExtendedRevisionEntity.class, 1);

        Assert.assertEquals(ExtendedRevisionListener.COMMENT_VALUE, ere.getComment());
    }
}
