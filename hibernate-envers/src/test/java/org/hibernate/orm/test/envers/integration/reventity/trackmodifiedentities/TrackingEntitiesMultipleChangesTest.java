/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import java.util.HashSet;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class},
		integrationSettings = @Setting(name = EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, value = "true"))
public class TrackingEntitiesMultipleChangesTest {
	private Integer steId1 = null;
	private Integer steId2 = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - Adding two entities
		scope.inTransaction( em -> {
			StrTestEntity ste1 = new StrTestEntity( "x" );
			StrTestEntity ste2 = new StrTestEntity( "y" );
			em.persist( ste1 );
			em.persist( ste2 );
			steId1 = ste1.getId();
			steId2 = ste2.getId();
		} );

		// Revision 2 - Adding first and removing second entity
		scope.inTransaction( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, steId1 );
			StrTestEntity ste2 = em.find( StrTestEntity.class, steId2 );
			ste1.setStr( "z" );
			em.remove( ste2 );
		} );

		// Revision 3 - Modifying and removing the same entity.
		scope.inTransaction( em -> {
			StrTestEntity ste1 = em.find( StrTestEntity.class, steId1 );
			ste1.setStr( "a" );
			em.merge( ste1 );
			em.remove( ste1 );
		} );
	}

	@Test
	public void testTrackAddedTwoEntities(EntityManagerFactoryScope scope) {
		StrTestEntity ste1 = new StrTestEntity( "x", steId1 );
		StrTestEntity ste2 = new StrTestEntity( "y", steId2 );

		scope.inEntityManager( em -> {
			assertEquals(
					TestTools.makeSet( ste1, ste2 ),
					new HashSet<Object>( getCrossTypeRevisionChangesReader( em ).findEntities( 1 ) )
			);
		} );
	}

	@Test
	public void testTrackUpdateAndRemoveDifferentEntities(EntityManagerFactoryScope scope) {
		StrTestEntity ste1 = new StrTestEntity( "z", steId1 );
		StrTestEntity ste2 = new StrTestEntity( null, steId2 );

		scope.inEntityManager( em -> {
			assertEquals(
					TestTools.makeSet( ste1, ste2 ),
					new HashSet<Object>( getCrossTypeRevisionChangesReader( em ).findEntities( 2 ) )
			);
		} );
	}

	@Test
	public void testTrackUpdateAndRemoveTheSameEntity(EntityManagerFactoryScope scope) {
		StrTestEntity ste1 = new StrTestEntity( null, steId1 );

		scope.inEntityManager( em -> {
			assertEquals(
					TestTools.makeSet( ste1 ),
					new HashSet<Object>( getCrossTypeRevisionChangesReader( em ).findEntities( 3 ) )
			);
		} );
	}

	private CrossTypeRevisionChangesReader getCrossTypeRevisionChangesReader(jakarta.persistence.EntityManager em) {
		return AuditReaderFactory.get( em ).getCrossTypeRevisionChangesReader();
	}
}
