/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
