package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.OracleExtendedRevisionEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.OracleExtendedRevisionListener;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
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
    protected void revisionEntityForDialect(Ejb3Configuration cfg, Dialect dialect, Properties configurationProperties) {
        if (dialect instanceof Oracle8iDialect) {
            cfg.addAnnotatedClass(OracleExtendedRevisionEntity.class);
        } else {
            cfg.addAnnotatedClass(ExtendedRevisionEntity.class);
        }
    }

	@Override
	public void addConfigurationProperties(Properties configuration) {
		super.addConfigurationProperties(configuration);
		configuration.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "false");
	}

    @Test
    @SkipForDialect(Oracle8iDialect.class)
    public void testCommentPropertyValue() {
        ExtendedRevisionEntity ere = getAuditReader().findRevision(ExtendedRevisionEntity.class, 1);

        Assert.assertEquals(ExtendedRevisionListener.COMMENT_VALUE, ere.getComment());
    }

    @Test
    @RequiresDialect(Oracle8iDialect.class)
    public void testCommentPropertyValueOracle() {
        OracleExtendedRevisionEntity ere = getAuditReader().findRevision(OracleExtendedRevisionEntity.class, 1);

        Assert.assertEquals(OracleExtendedRevisionListener.COMMENT_VALUE, ere.getUserComment());
    }
}
