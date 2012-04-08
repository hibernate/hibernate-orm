package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
    @Override
    protected void revisionEntityForDialect(Ejb3Configuration cfg, Dialect dialect, Properties configurationProperties) {
        cfg.addAnnotatedClass(AnnotatedTrackingRevisionEntity.class);
    }

	@Override
	public void addConfigurationProperties(Properties configuration) {
		super.addConfigurationProperties( configuration );
		configuration.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "false");
	}
}
