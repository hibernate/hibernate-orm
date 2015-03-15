/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.criteria.enumcollection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

import org.junit.Test;

import org.hibernate.jpa.test.criteria.enumcollection.User_;


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
	@TestForIssue(jiraKey = "HHH-9605")
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
