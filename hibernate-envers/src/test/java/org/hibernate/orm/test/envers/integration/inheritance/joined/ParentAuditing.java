/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@Jpa(annotatedClasses = {ChildEntity.class, ParentEntity.class})
public class ParentAuditing {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = 1;

		// Rev 1
		scope.inTransaction( em -> {
			ParentEntity pe = new ParentEntity( id1, "x" );
			em.persist( pe );
		} );

		// Rev 2
		scope.inTransaction( em -> {
			ParentEntity pe = em.find( ParentEntity.class, id1 );
			pe.setData( "y" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( ParentEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfChildId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( ChildEntity.class, id1, 1 ) );
			assertNull( auditReader.find( ChildEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testHistoryOfParentId1(EntityManagerFactoryScope scope) {
		ParentEntity ver1 = new ParentEntity( id1, "x" );
		ParentEntity ver2 = new ParentEntity( id1, "y" );

		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ParentEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ParentEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testPolymorphicQuery(EntityManagerFactoryScope scope) {
		ParentEntity parentVer1 = new ParentEntity( id1, "x" );

		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( parentVer1, auditReader.createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult() );
			assertEquals( 0, auditReader.createQuery().forEntitiesAtRevision( ChildEntity.class, 1 ).getResultList().size() );
		} );
	}
}
