package org.hibernate.envers.test.integration.reventity.removal;

import java.util.Map;

import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RemoveTrackingRevisionEntity extends AbstractRevisionEntityRemovalTest {
	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put("org.hibernate.envers.track_entities_changed_in_revision", "true");
	}

	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
	}
}
