/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single;

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
@Jpa(annotatedClasses = {ChildEntity.class, ParentEntity.class})
public class ChildAuditing {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		scope.inTransaction( em -> {
			ChildEntity ce = new ChildEntity( "x", 1l );
			em.persist( ce );
			id1 = ce.getId();
		} );

		// Rev 2
		scope.inTransaction( em -> {
			ChildEntity ce = em.find( ChildEntity.class, id1 );
			ce.setData( "y" );
			ce.setNumVal( 2l );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( ChildEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfChildId1(EntityManagerFactoryScope scope) {
		ChildEntity ver1 = new ChildEntity( id1, "x", 1l );
		ChildEntity ver2 = new ChildEntity( id1, "y", 2l );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( ver1, auditReader.find( ChildEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ChildEntity.class, id1, 2 ) );

			assertEquals( ver1, auditReader.find( ParentEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ParentEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testPolymorphicQuery(EntityManagerFactoryScope scope) {
		ChildEntity childVer1 = new ChildEntity( id1, "x", 1l );

		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( childVer1,
					auditReader.createQuery().forEntitiesAtRevision( ChildEntity.class, 1 ).getSingleResult() );

			assertEquals( childVer1,
					auditReader.createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult() );
		} );
	}
}
