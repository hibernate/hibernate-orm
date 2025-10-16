/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {BasicTestEntity2.class})
public class Delete {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();

			BasicTestEntity2 bte1 = new BasicTestEntity2( "x", "a" );
			BasicTestEntity2 bte2 = new BasicTestEntity2( "y", "b" );
			BasicTestEntity2 bte3 = new BasicTestEntity2( "z", "c" );
			em.persist( bte1 );
			em.persist( bte2 );
			em.persist( bte3 );

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			bte1 = em.find( BasicTestEntity2.class, bte1.getId() );
			bte2 = em.find( BasicTestEntity2.class, bte2.getId() );
			bte3 = em.find( BasicTestEntity2.class, bte3.getId() );
			bte1.setStr1( "x2" );
			bte2.setStr2( "b2" );
			em.remove( bte3 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			bte2 = em.find( BasicTestEntity2.class, bte2.getId() );
			em.remove( bte2 );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			bte1 = em.find( BasicTestEntity2.class, bte1.getId() );
			em.remove( bte1 );

			em.getTransaction().commit();

			id1 = bte1.getId();
			id2 = bte2.getId();
			id3 = bte3.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2, 4 ),
					org.hibernate.envers.AuditReaderFactory.get( em ).getRevisions( BasicTestEntity2.class, id1 ) );
			assertEquals( Arrays.asList( 1, 3 ),
					org.hibernate.envers.AuditReaderFactory.get( em ).getRevisions( BasicTestEntity2.class, id2 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					org.hibernate.envers.AuditReaderFactory.get( em ).getRevisions( BasicTestEntity2.class, id3 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( new BasicTestEntity2( id1, "x", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id1, 1 ) );
			assertEquals( new BasicTestEntity2( id1, "x2", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id1, 2 ) );
			assertEquals( new BasicTestEntity2( id1, "x2", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id1, 3 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id1, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( new BasicTestEntity2( id2, "y", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id2, 1 ) );
			assertEquals( new BasicTestEntity2( id2, "y", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id2, 2 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id2, 3 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id2, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( new BasicTestEntity2( id3, "z", null ),
					org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id3, 1 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id3, 2 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id3, 3 ) );
			assertNull( org.hibernate.envers.AuditReaderFactory.get( em ).find( BasicTestEntity2.class, id3, 4 ) );
		} );
	}
}
