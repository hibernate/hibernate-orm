/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.cid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Donnchadh O Donnabhain
 */
public class EmbeddedAndNaturalIdTest extends BaseCoreFunctionalTestCase {
	@JiraKey(value = "HHH-9333")
	@Test
	public void testSave() {
		A account = new A( new AId( 1 ), "testCode" );
		inTransaction(
				session ->
					session.persist( account )
		);
	}

	@JiraKey(value = "HHH-9333")
	@Test
	public void testNaturalIdCriteria() {
		inTransaction(
				s -> {
					A u = new A( new AId( 1 ), "testCode" );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
					Root<A> root = criteria.from( A.class );
					criteria.where( criteriaBuilder.equal( root.get( "shortCode" ), "testCode" ) );
					A u = s.createQuery( criteria ).uniqueResult();
//        u = ( A ) s.createCriteria( A.class )
//                .add( Restrictions.naturalId().set( "shortCode", "testCode" ) )
//                .uniqueResult();
					assertNotNull( u );
				}
		);
	}

	@JiraKey(value = "HHH-9333")
	@Test
	public void testByNaturalId() {
		inTransaction(
				s -> {
					A u = new A( new AId( 1 ), "testCode" );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					A u = s.byNaturalId( A.class ).using( "shortCode", "testCode" ).load();
					assertNotNull( u );
				}
		);
	}

	@After
	public void tearDown() {
		// clean up
		inTransaction(
				session ->
					session.createQuery( "delete A" ).executeUpdate()

		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AId.class
		};
	}

}
