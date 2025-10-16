/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.emptychild;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {EmptyChildEntity.class, ParentEntity.class})
public class EmptyChildAuditing {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = 1;

		// Rev 1
		scope.inTransaction( em -> {
			EmptyChildEntity pe = new EmptyChildEntity( id1, "x" );
			em.persist( pe );
		} );

		// Rev 2
		scope.inTransaction( em -> {
			EmptyChildEntity pe = em.find( EmptyChildEntity.class, id1 );
			pe.setData( "y" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( em ).getRevisions( EmptyChildEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfChildId1(EntityManagerFactoryScope scope) {
		EmptyChildEntity ver1 = new EmptyChildEntity( id1, "x" );
		EmptyChildEntity ver2 = new EmptyChildEntity( id1, "y" );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( EmptyChildEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( EmptyChildEntity.class, id1, 2 ) );

			assertEquals( ver1, auditReader.find( ParentEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ParentEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testPolymorphicQuery(EntityManagerFactoryScope scope) {
		EmptyChildEntity childVer1 = new EmptyChildEntity( id1, "x" );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( childVer1, auditReader.createQuery().forEntitiesAtRevision( EmptyChildEntity.class, 1 ).getSingleResult() );

			assertEquals( childVer1, auditReader.createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult() );
		} );
	}
}
