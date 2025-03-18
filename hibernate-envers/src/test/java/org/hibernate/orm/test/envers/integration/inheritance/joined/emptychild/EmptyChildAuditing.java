/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.emptychild;

import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmptyChildAuditing extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EmptyChildEntity.class, ParentEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		id1 = 1;

		// Rev 1
		em.getTransaction().begin();
		EmptyChildEntity pe = new EmptyChildEntity( id1, "x" );
		em.persist( pe );
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		pe = em.find( EmptyChildEntity.class, id1 );
		pe.setData( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( EmptyChildEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfChildId1() {
		EmptyChildEntity ver1 = new EmptyChildEntity( id1, "x" );
		EmptyChildEntity ver2 = new EmptyChildEntity( id1, "y" );

		assert getAuditReader().find( EmptyChildEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( EmptyChildEntity.class, id1, 2 ).equals( ver2 );

		assert getAuditReader().find( ParentEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ParentEntity.class, id1, 2 ).equals( ver2 );
	}

	@Test
	public void testPolymorphicQuery() {
		EmptyChildEntity childVer1 = new EmptyChildEntity( id1, "x" );

		assert getAuditReader().createQuery().forEntitiesAtRevision( EmptyChildEntity.class, 1 ).getSingleResult()
				.equals( childVer1 );

		assert getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult()
				.equals( childVer1 );
	}
}
