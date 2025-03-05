/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.enumcollection;

import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Mote that these are simply performing syntax checking (can the criteria query
 * be properly compiled and executed)
 *
 * @author Steve Ebersole
 */
public class EnumIsMemberTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class};
	}

	@Test
	@JiraKey(value = "HHH-9605")
	public void testQueryEnumCollection() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		User user = new User();
		user.setId( 1l );
		user.getRoles().add( User.Role.Admin );
		em.persist( user );
		em.getTransaction().commit();
		em.getTransaction().begin();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<User> query = builder.createQuery( User.class );
		Root<User> root = query.from( User.class );

		Expression<Set<User.Role>> roles = root.get( User_.roles );

		// Using the correct collection of enums and an enum parameter
		query.where( builder.isMember( User.Role.Admin, roles ) );

		TypedQuery<User> typedQuery = em.createQuery( query );
		List<User> users = typedQuery.getResultList();
		assertEquals( 1, users.size() );

		em.getTransaction().commit();
		em.getTransaction().begin();
		// delete
		em.remove( user );
		em.getTransaction().commit();

	}


}
