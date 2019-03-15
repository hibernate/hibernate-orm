/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import java.util.Map;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.AnnotatedTrackingRevisionEntity;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), AnnotatedTrackingRevisionEntity.class );
	}

	@Override
	public void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "false" );
	}
}
