/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.CustomTrackingRevisionEntity;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.CustomTrackingRevisionListener;
import org.hibernate.envers.test.support.domains.revisionentity.trackmodifiedentitynames.ModifiedEntityTypeEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;


/**
 * Tests proper behavior of entity listener that implements {@link EntityTrackingRevisionListener}
 * interface. {@link CustomTrackingRevisionListener} shall be notified whenever an entity instance has been
 * added, modified or removed, so that changed entity name can be persisted.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomTrackingEntitiesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer steId = null;
	private Integer siteId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ModifiedEntityTypeEntity.class,
				StrTestEntity.class,
				StrIntTestEntity.class,
				CustomTrackingRevisionEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - Adding two entities
				entityManager -> {
					final StrTestEntity ste = new StrTestEntity( "x" );
					final StrIntTestEntity site = new StrIntTestEntity( "y", 1 );
					entityManager.persist( ste );
					entityManager.persist( site );
					steId = ste.getId();
					siteId = site.getId();
				},

				// Revision 2 - Modifying one entity
				entityManager -> {
					final StrIntTestEntity site = entityManager.find( StrIntTestEntity.class, siteId );
					site.setNumber( 2 );
				},

				// Revision 3 - Deleting both entities
				entityManager -> {
					final StrTestEntity ste = entityManager.find( StrTestEntity.class, steId );
					final StrIntTestEntity site = entityManager.find( StrIntTestEntity.class, siteId );
					entityManager.remove( ste );
					entityManager.remove( site );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackAddedEntities() {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 1 );

		assertThat( ctre.getModifiedEntityTypes(), notNullValue() );
		assertThat( ctre.getModifiedEntityTypes(), CollectionMatchers.hasSize( 2 ) );
		assertThat( ctre.getModifiedEntityTypes(), containsInAnyOrder( steDescriptor, siteDescriptor ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackModifiedEntities() {
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 2 );

		assertThat( ctre.getModifiedEntityTypes(), notNullValue() );
		assertThat( ctre.getModifiedEntityTypes(), CollectionMatchers.hasSize( 1 ) );
		assertThat( ctre.getModifiedEntityTypes(), containsInAnyOrder( siteDescriptor ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackDeletedEntities() {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		CustomTrackingRevisionEntity ctre = getAuditReader().findRevision( CustomTrackingRevisionEntity.class, 3 );

		assertThat( ctre.getModifiedEntityTypes(), notNullValue() );
		assertThat( ctre.getModifiedEntityTypes(), CollectionMatchers.hasSize( 2 ) );
		assertThat( ctre.getModifiedEntityTypes(), containsInAnyOrder( steDescriptor, siteDescriptor ) );
	}

	@DynamicTest(expected = AuditException.class)
	public void testFindEntitiesChangedInRevisionException() {
		getAuditReader().getCrossTypeRevisionChangesReader();
	}
}
