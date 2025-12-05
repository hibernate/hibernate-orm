/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.cid;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Donnchadh O Donnabhain
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {A.class, AId.class})
@SessionFactory
public class EmbeddedAndNaturalIdTest {
	@JiraKey(value = "HHH-9333")
	@Test
	public void testSave(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(  session -> {
			var account = new A( new AId( 1 ), "testCode" );
			session.persist( account );
		} );
	}

	@JiraKey(value = "HHH-9333")
	@Test
	public void testNaturalIdCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			A u = new A( new AId( 1 ), "testCode" );
			session.persist( u );
		} );

		factoryScope.inTransaction( (session) -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
			Root<A> root = criteria.from( A.class );
			criteria.where( criteriaBuilder.equal( root.get( "shortCode" ), "testCode" ) );
			A u = session.createQuery( criteria ).uniqueResult();
			assertNotNull( u );
		} );
	}

	@JiraKey(value = "HHH-9333")
	@Test
	public void testByNaturalId(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			A u = new A( new AId( 1 ), "testCode" );
			session.persist( u );
		} );

		factoryScope.inTransaction(s -> {
			A u = s.byNaturalId( A.class ).using( "shortCode", "testCode" ).load();
			assertNotNull( u );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

}
