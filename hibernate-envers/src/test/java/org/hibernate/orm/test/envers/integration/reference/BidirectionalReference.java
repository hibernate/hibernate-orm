/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reference;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {GreetingPO.class, GreetingSetPO.class})
public class BidirectionalReference {
	private Long set1_id;
	private Long set2_id;

	private Long g1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			GreetingSetPO set1 = new GreetingSetPO();
			set1.setName( "a1" );

			GreetingSetPO set2 = new GreetingSetPO();
			set2.setName( "a2" );

			em.persist( set1 );
			em.persist( set2 );

			set1_id = set1.getId();
			set2_id = set2.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			GreetingPO g1 = new GreetingPO();
			g1.setGreeting( "g1" );
			g1.setGreetingSet( em.getReference( GreetingSetPO.class, set1_id ) );

			em.persist( g1 );
			g1_id = g1.getId();
		} );

		// Revision 3
		scope.inTransaction( em -> {
			GreetingPO g1 = em.find( GreetingPO.class, g1_id );
			g1.setGreetingSet( em.getReference( GreetingSetPO.class, set2_id ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 2, 3 ), auditReader.getRevisions( GreetingPO.class, g1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( GreetingSetPO.class, set1_id ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( GreetingSetPO.class, set2_id ) );
		} );
	}

	@Test
	public void testHistoryOfG1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			GreetingPO rev1 = auditReader.find( GreetingPO.class, g1_id, 1 );
			GreetingPO rev2 = auditReader.find( GreetingPO.class, g1_id, 2 );
			GreetingPO rev3 = auditReader.find( GreetingPO.class, g1_id, 3 );

			assertNull( rev1 );
			assertEquals( "a1", rev2.getGreetingSet().getName() );
			assertEquals( "a2", rev3.getGreetingSet().getName() );
		} );
	}

	@Test
	public void testHistoryOfSet1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			GreetingSetPO rev1 = auditReader.find( GreetingSetPO.class, set1_id, 1 );
			GreetingSetPO rev2 = auditReader.find( GreetingSetPO.class, set1_id, 2 );
			GreetingSetPO rev3 = auditReader.find( GreetingSetPO.class, set1_id, 3 );

			assertEquals( "a1", rev1.getName() );
			assertEquals( "a1", rev2.getName() );
			assertEquals( "a1", rev3.getName() );

			GreetingPO g1 = new GreetingPO();
			g1.setId( g1_id );
			g1.setGreeting( "g1" );

			assertEquals( 0, rev1.getGreetings().size() );
			assertEquals( TestTools.makeSet( g1 ), rev2.getGreetings() );
			assertEquals( 0, rev3.getGreetings().size() );
		} );
	}

	@Test
	public void testHistoryOfSet2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			GreetingSetPO rev1 = auditReader.find( GreetingSetPO.class, set2_id, 1 );
			GreetingSetPO rev2 = auditReader.find( GreetingSetPO.class, set2_id, 2 );
			GreetingSetPO rev3 = auditReader.find( GreetingSetPO.class, set2_id, 3 );

			assertEquals( "a2", rev1.getName() );
			assertEquals( "a2", rev2.getName() );
			assertEquals( "a2", rev3.getName() );

			GreetingPO g1 = new GreetingPO();
			g1.setId( g1_id );
			g1.setGreeting( "g1" );

			assertEquals( 0, rev1.getGreetings().size() );
			assertEquals( 0, rev2.getGreetings().size() );
			assertEquals( TestTools.makeSet( g1 ), rev3.getGreetings() );
		} );
	}
}
