/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.removal;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialectFeature(DialectChecks.SupportsCascadeDeleteCheck.class)
@Disabled("NYI - Native query support")
public class RemoveTrackingRevisionEntityTest extends AbstractRevisionEntityRemovalTest {
	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "true" );
	}

	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
	}
}
