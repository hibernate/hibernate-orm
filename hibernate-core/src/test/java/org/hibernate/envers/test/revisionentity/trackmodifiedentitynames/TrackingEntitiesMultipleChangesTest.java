/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import java.util.Map;

import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class TrackingEntitiesMultipleChangesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer steId1 = null;
	private Integer steId2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - Adding two entities
				entityManager -> {
					final StrTestEntity ste1 = new StrTestEntity( "x" );
					final StrTestEntity ste2 = new StrTestEntity( "y" );
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );
					steId1 = ste1.getId();
					steId2 = ste2.getId();
				},

				// Revision 2 - Adding first and removing second entity
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, steId1 );
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, steId2 );
					ste1.setStr( "z" );
					entityManager.remove( ste2 );
				},

				// Revision 3 - Modifying and removing the same entity.
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, steId1 );
					ste1.setStr( "a" );
					entityManager.merge( ste1 );
					entityManager.remove( ste1 );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackAddedTwoEntities() {
		final StrTestEntity ste1 = new StrTestEntity( steId1, "x" );
		final StrTestEntity ste2 = new StrTestEntity( steId2, "y" );

		assertThat( getCrossTypeRevisionReader().findEntities( 1 ), containsInAnyOrder( ste1, ste2 ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackUpdateAndRemoveDifferentEntities() {
		final StrTestEntity ste1 = new StrTestEntity( steId1, "z" );
		final StrTestEntity ste2 = new StrTestEntity( steId2, null );

		assertThat( getCrossTypeRevisionReader().findEntities( 2 ), containsInAnyOrder( ste1, ste2 ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackUpdateAndRemoveTheSameEntity() {
		final StrTestEntity ste1 = new StrTestEntity( steId1, null );

		assertThat( getCrossTypeRevisionReader().findEntities( 3 ), containsInAnyOrder( ste1 ) );
	}

	private CrossTypeRevisionChangesReader getCrossTypeRevisionReader() {
		return getAuditReader().getCrossTypeRevisionChangesReader();
	}
}