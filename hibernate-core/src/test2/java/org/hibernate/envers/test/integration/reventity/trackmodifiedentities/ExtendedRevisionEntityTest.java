/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Map;

import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.ExtendedRevisionEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.ExtendedRevisionListener;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests proper behavior of revision entity that extends {@link DefaultTrackingModifiedEntitiesRevisionEntity}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ExtendedRevisionEntityTest extends DefaultTrackingEntitiesTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return ArrayHelper.join( super.getAnnotatedClasses(), ExtendedRevisionEntity.class );
	}

	@Override
	public void addConfigOptions(Map configuration) {
		super.addConfigOptions( configuration );
		configuration.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "false" );
	}

	@Test
	public void testCommentPropertyValue() {
		ExtendedRevisionEntity ere = getAuditReader().findRevision( ExtendedRevisionEntity.class, 1 );

		Assert.assertEquals( ExtendedRevisionListener.COMMENT_VALUE, ere.getComment() );
	}
}
