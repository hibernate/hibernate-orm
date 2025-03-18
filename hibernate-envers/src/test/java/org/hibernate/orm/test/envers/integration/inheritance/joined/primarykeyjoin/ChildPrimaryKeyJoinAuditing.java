/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.primarykeyjoin;

import java.util.Arrays;

import jakarta.persistence.EntityManager;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ChildPrimaryKeyJoinAuditing extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ChildPrimaryKeyJoinEntity.class, ParentEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		id1 = 1;

		// Rev 1
		em.getTransaction().begin();
		ChildPrimaryKeyJoinEntity ce = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		em.persist( ce );
		em.getTransaction().commit();

		// Rev 2
		em.getTransaction().begin();
		ce = em.find( ChildPrimaryKeyJoinEntity.class, id1 );
		ce.setData( "y" );
		ce.setNumVal( 2l );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ChildPrimaryKeyJoinEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfChildId1() {
		ChildPrimaryKeyJoinEntity ver1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		ChildPrimaryKeyJoinEntity ver2 = new ChildPrimaryKeyJoinEntity( id1, "y", 2l );

		assert getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 2 ).equals( ver2 );

		assert getAuditReader().find( ParentEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( ParentEntity.class, id1, 2 ).equals( ver2 );
	}

	@Test
	public void testPolymorphicQuery() {
		ChildPrimaryKeyJoinEntity childVer1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );

		assert getAuditReader().createQuery()
				.forEntitiesAtRevision( ChildPrimaryKeyJoinEntity.class, 1 )
				.getSingleResult()
				.equals( childVer1 );

		assert getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult()
				.equals( childVer1 );
	}

	@Test
	public void testChildIdColumnName() {
		// Hibernate now sorts columns that are part of the key and therefore this test needs to test
		// for the existence of the specific key column rather than the expectation that is exists at
		// a specific order in the iterator.
		final PersistentClass persistentClass = metadata().getEntityBinding( ChildPrimaryKeyJoinEntity.class.getName() + "_AUD" );
		Assert.assertNotNull( getColumnFromIteratorByName( persistentClass.getKey().getSelectables(), "other_id" ) );
	}
}
