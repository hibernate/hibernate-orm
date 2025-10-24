/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.entities.components.ComponentTestEntity;
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
@Jpa(annotatedClasses = {ComponentTestEntity.class})
public class Components {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a", "b" ), new Component2( "x", "y" ) );
			ComponentTestEntity cte2 = new ComponentTestEntity(
					new Component1( "a2", "b2" ), new Component2(
					"x2",
					"y2"
			)
			);
			ComponentTestEntity cte3 = new ComponentTestEntity(
					new Component1( "a3", "b3" ), new Component2(
					"x3",
					"y3"
			)
			);
			ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

			em.persist( cte1 );
			em.persist( cte2 );
			em.persist( cte3 );
			em.persist( cte4 );

			id1 = cte1.getId();
			id2 = cte2.getId();
			id3 = cte3.getId();
			id4 = cte4.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = em.find( ComponentTestEntity.class, id1 );
			ComponentTestEntity cte2 = em.find( ComponentTestEntity.class, id2 );
			ComponentTestEntity cte3 = em.find( ComponentTestEntity.class, id3 );
			ComponentTestEntity cte4 = em.find( ComponentTestEntity.class, id4 );

			cte1.setComp1( new Component1( "a'", "b'" ) );
			cte2.getComp1().setStr1( "a2'" );
			cte3.getComp2().setStr6( "y3'" );
			cte4.setComp1( new Component1() );
			cte4.getComp1().setStr1( "n" );
			cte4.setComp2( new Component2() );
			cte4.getComp2().setStr5( "m" );
		} );

		// Revision 3
		scope.inTransaction( em -> {
			ComponentTestEntity cte1 = em.find( ComponentTestEntity.class, id1 );
			ComponentTestEntity cte2 = em.find( ComponentTestEntity.class, id2 );
			ComponentTestEntity cte3 = em.find( ComponentTestEntity.class, id3 );
			ComponentTestEntity cte4 = em.find( ComponentTestEntity.class, id4 );

			cte1.setComp2( new Component2( "x'", "y'" ) );
			cte3.getComp1().setStr2( "b3'" );
			cte4.setComp1( null );
			cte4.setComp2( null );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			ComponentTestEntity cte2 = em.find( ComponentTestEntity.class, id2 );
			em.remove( cte2 );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ComponentTestEntity.class, id1 ) );
			assertEquals( Arrays.asList( 1, 2, 4 ), auditReader.getRevisions( ComponentTestEntity.class, id2 ) );
			assertEquals( Arrays.asList( 1, 3 ), auditReader.getRevisions( ComponentTestEntity.class, id3 ) );
			assertEquals( Arrays.asList( 1, 2, 3 ), auditReader.getRevisions( ComponentTestEntity.class, id4 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		ComponentTestEntity ver1 = new ComponentTestEntity( id1, new Component1( "a", "b" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id1, new Component1( "a'", "b'" ), null );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id1, 2 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id1, 3 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id1, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		ComponentTestEntity ver1 = new ComponentTestEntity( id2, new Component1( "a2", "b2" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id2, new Component1( "a2'", "b2" ), null );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id2, 1 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id2, 2 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id2, 3 ) );
			assertNull( auditReader.find( ComponentTestEntity.class, id2, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		ComponentTestEntity ver1 = new ComponentTestEntity( id3, new Component1( "a3", "b3" ), null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id3, new Component1( "a3", "b3'" ), null );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id3, 1 ) );
			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id3, 2 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id3, 3 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id3, 4 ) );
		} );
	}

	@Test
	public void testHistoryOfId4(EntityManagerFactoryScope scope) {
		ComponentTestEntity ver1 = new ComponentTestEntity( id4, null, null );
		ComponentTestEntity ver2 = new ComponentTestEntity( id4, new Component1( "n", null ), null );
		ComponentTestEntity ver3 = new ComponentTestEntity( id4, null, null );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ComponentTestEntity.class, id4, 1 ) );
			assertEquals( ver2, auditReader.find( ComponentTestEntity.class, id4, 2 ) );
			assertEquals( ver3, auditReader.find( ComponentTestEntity.class, id4, 3 ) );
			assertEquals( ver3, auditReader.find( ComponentTestEntity.class, id4, 4 ) );
		} );
	}
}
