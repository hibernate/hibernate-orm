package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Map;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), AnnotatedTrackingRevisionEntity.class );
    }

	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put("org.hibernate.envers.track_entities_changed_in_revision", "false");
	}
}
