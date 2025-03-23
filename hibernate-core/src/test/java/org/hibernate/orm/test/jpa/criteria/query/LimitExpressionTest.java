/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

/**
 * @author Andrea Boriero
 */

public class LimitExpressionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class};
	}

	@Test
	@JiraKey(value = "HHH-11278")
	public void testAnEmptyListIsReturnedWhenSetMaxResultsToZero() {
		TransactionUtil.doInJPA( this::entityManagerFactory, (EntityManager entityManager) -> {
			final CriteriaQuery<Person> query = entityManager.getCriteriaBuilder().createQuery( Person.class );
			query.from( Person.class );
			final List list = entityManager.createQuery( query ).setMaxResults( 0 ).getResultList();
			assertTrue( "The list should be empty with setMaxResults 0", list.isEmpty() );
		} );
	}

	@Before
	public void prepareTest() throws Exception {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Person p = new Person();
			entityManager.persist( p );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;
	}
}
