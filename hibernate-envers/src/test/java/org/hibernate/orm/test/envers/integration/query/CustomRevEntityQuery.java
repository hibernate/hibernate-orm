/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomRevEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {
		StrIntTestEntity.class,
		CustomRevEntity.class
})
@EnversTest
public class CustomRevEntityQuery {
	private Integer id1;
	private Integer id2;
	private Long timestamp;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws InterruptedException {
		// Revision 1
		scope.inTransaction( em -> {
			StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
			StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

			em.persist( site1 );
			em.persist( site2 );

			id1 = site1.getId();
			id2 = site2.getId();
		} );

		Thread.sleep( 100 );

		timestamp = System.currentTimeMillis();

		Thread.sleep( 100 );

		// Revision 2
		scope.inTransaction( em -> {
			StrIntTestEntity site1 = em.find( StrIntTestEntity.class, id1 );
			site1.setStr1( "c" );
		} );
	}

	@Test
	public void testRevisionsOfId1Query(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Object[]> result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id1 ) )
					.getResultList();

			assertEquals( new StrIntTestEntity( "a", 10, id1 ), result.get( 0 )[0] );
			assertInstanceOf( CustomRevEntity.class, result.get( 0 )[1] );
			assertEquals( 1, ((CustomRevEntity) result.get( 0 )[1]).getCustomId() );

			assertEquals( new StrIntTestEntity( "c", 10, id1 ), result.get( 1 )[0] );
			assertInstanceOf( CustomRevEntity.class, result.get( 1 )[1] );
			assertEquals( 2, ((CustomRevEntity) result.get( 1 )[1]).getCustomId() );
		} );
	}

	@Test
	public void testRevisionsOfId2Query(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Object[]> result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id2 ) )
					.getResultList();

			assertEquals( new StrIntTestEntity( "b", 15, id2 ), result.get( 0 )[0] );
			assertInstanceOf( CustomRevEntity.class, result.get( 0 )[1] );
			assertEquals( 1, ((CustomRevEntity) result.get( 0 )[1]).getCustomId() );
		} );
	}

	@Test
	public void testRevisionPropertyRestriction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Object[]> result = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
					.add( AuditEntity.id().eq( id1 ) )
					.add( AuditEntity.revisionProperty( "customTimestamp" ).ge( timestamp ) )
					.getResultList();

			assertEquals( new StrIntTestEntity( "c", 10, id1 ), result.get( 0 )[0] );
			assertInstanceOf( CustomRevEntity.class, result.get( 0 )[1] );
			assertEquals( 2, ((CustomRevEntity) result.get( 0 )[1]).getCustomId() );
			assertTrue( ((CustomRevEntity) result.get( 0 )[1]).getCustomTimestamp() >= timestamp );
		} );
	}
}
