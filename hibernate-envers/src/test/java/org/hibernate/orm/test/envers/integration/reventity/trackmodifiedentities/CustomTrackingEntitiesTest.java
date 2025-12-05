/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.CustomTrackingRevisionEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.CustomTrackingRevisionListener;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.ModifiedEntityTypeEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests proper behavior of entity listener that implements {@link EntityTrackingRevisionListener}
 * interface. {@link CustomTrackingRevisionListener} shall be notified whenever an entity instance has been
 * added, modified or removed, so that changed entity name can be persisted.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@Jpa(annotatedClasses = {
		ModifiedEntityTypeEntity.class,
		StrTestEntity.class,
		StrIntTestEntity.class,
		CustomTrackingRevisionEntity.class
})
public class CustomTrackingEntitiesTest {
	private Integer steId = null;
	private Integer siteId = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - Adding two entities
		scope.inTransaction( em -> {
			StrTestEntity ste = new StrTestEntity( "x" );
			StrIntTestEntity site = new StrIntTestEntity( "y", 1 );
			em.persist( ste );
			em.persist( site );
			steId = ste.getId();
			siteId = site.getId();
		} );

		// Revision 2 - Modifying one entity
		scope.inTransaction( em -> {
			StrIntTestEntity site = em.find( StrIntTestEntity.class, siteId );
			site.setNumber( 2 );
		} );

		// Revision 3 - Deleting both entities
		scope.inTransaction( em -> {
			StrTestEntity ste = em.find( StrTestEntity.class, steId );
			StrIntTestEntity site = em.find( StrIntTestEntity.class, siteId );
			em.remove( ste );
			em.remove( site );
		} );
	}

	@Test
	public void testTrackAddedEntities(EntityManagerFactoryScope scope) {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		scope.inEntityManager( em -> {
			CustomTrackingRevisionEntity ctre = AuditReaderFactory.get( em )
					.findRevision( CustomTrackingRevisionEntity.class, 1 );

			assertNotNull( ctre.getModifiedEntityTypes() );
			assertEquals( 2, ctre.getModifiedEntityTypes().size() );
			assertEquals( TestTools.makeSet( steDescriptor, siteDescriptor ), ctre.getModifiedEntityTypes() );
		} );
	}

	@Test
	public void testTrackModifiedEntities(EntityManagerFactoryScope scope) {
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		scope.inEntityManager( em -> {
			CustomTrackingRevisionEntity ctre = AuditReaderFactory.get( em )
					.findRevision( CustomTrackingRevisionEntity.class, 2 );

			assertNotNull( ctre.getModifiedEntityTypes() );
			assertEquals( 1, ctre.getModifiedEntityTypes().size() );
			assertEquals( TestTools.makeSet( siteDescriptor ), ctre.getModifiedEntityTypes() );
		} );
	}

	@Test
	public void testTrackDeletedEntities(EntityManagerFactoryScope scope) {
		ModifiedEntityTypeEntity steDescriptor = new ModifiedEntityTypeEntity( StrTestEntity.class.getName() );
		ModifiedEntityTypeEntity siteDescriptor = new ModifiedEntityTypeEntity( StrIntTestEntity.class.getName() );

		scope.inEntityManager( em -> {
			CustomTrackingRevisionEntity ctre = AuditReaderFactory.get( em )
					.findRevision( CustomTrackingRevisionEntity.class, 3 );

			assertNotNull( ctre.getModifiedEntityTypes() );
			assertEquals( 2, ctre.getModifiedEntityTypes().size() );
			assertEquals( TestTools.makeSet( steDescriptor, siteDescriptor ), ctre.getModifiedEntityTypes() );
		} );
	}

	@Test
	public void testFindEntitiesChangedInRevisionException(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertThrows( AuditException.class, () -> {
				AuditReaderFactory.get( em ).getCrossTypeRevisionChangesReader();
			} );
		} );
	}
}
