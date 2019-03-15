/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import java.util.Map;

import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.ExtendedRevisionEntity;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.ExtendedRevisionListener;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests proper behavior of revision entity that extends {@link DefaultTrackingModifiedEntitiesRevisionEntity}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Disabled("NYI - Inheritance support")
public class ExtendedRevisionEntityTest extends DefaultTrackingEntitiesTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), ExtendedRevisionEntity.class );
	}

	@Override
	public void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "false" );
	}

	@DynamicTest
	public void testCommentPropertyValue() {
		final ExtendedRevisionEntity ere = getAuditReader().findRevision( ExtendedRevisionEntity.class, 1 );
		assertThat( ere.getComment(), equalTo( ExtendedRevisionListener.COMMENT_VALUE ) );
	}
}
