/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for registering and binding parameters
 * for a {@link jakarta.persistence.StoredProcedureQuery}
 *
 * Uses H2's `locate` function.  Uses the function's default
 * parameter to test that not registering the parameter forces
 * the parameter's default value to be used
 *
 * @author Steve Ebersole
 */
@Jpa
@RequiresDialect( H2Dialect.class )
@JiraKey("https://hibernate.atlassian.net/browse/HHH-11447")
public class ProcedureParameterTests {

	@Test
	public void testRegisteredParameter(EntityManagerFactoryScope scope) {
		// locate takes 2 parameters with an optional 3rd.  Here, we will call it
		// registering and binding all 3 parameters
		scope.inTransaction( (em) -> {
			final StoredProcedureQuery query = em.createStoredProcedureQuery("locate" );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );
			// search-tring
			query.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
			// source-string
			query.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
			// start-position
			query.registerStoredProcedureParameter( 3, Integer.class, ParameterMode.IN );

			query.setParameter( 1, "." );
			query.setParameter( 2, "org.hibernate.query" );
			query.setParameter( 3, 5 );

			final Object singleResult = query.getSingleResult();
			assertThat( singleResult ).isInstanceOf( Integer.class );
			assertThat( singleResult ).isEqualTo( 14 );
		} );

		// explicit start-position baseline for no-arg

		scope.inTransaction( (em) -> {
			final StoredProcedureQuery query = em.createStoredProcedureQuery("locate" );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );
			// search-string
			query.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
			// source-string
			query.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );
			// start-position
			query.registerStoredProcedureParameter( 3, Integer.class, ParameterMode.IN );

			query.setParameter( 1, "." );
			query.setParameter( 2, "org.hibernate.query" );
			query.setParameter( 3, 0 );

			final Object singleResult = query.getSingleResult();
			assertThat( singleResult ).isInstanceOf( Integer.class );
			assertThat( singleResult ).isEqualTo( 4 );
		} );
	}

	@Test
	public void testUnRegisteredParameter(EntityManagerFactoryScope scope) {
		// next, skip start-position registration which should trigger the
		// function's default value defined on the database to be applied
		scope.inTransaction( (em) -> {
			final StoredProcedureQuery query = em.createStoredProcedureQuery("locate" );
			query.setHint( HibernateHints.HINT_CALLABLE_FUNCTION, "true" );
			// search-string
			query.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
			// source-string
			query.registerStoredProcedureParameter( 2, String.class, ParameterMode.IN );

			query.setParameter( 1, "." );
			query.setParameter( 2, "org.hibernate.query" );

			final Object singleResult = query.getSingleResult();
			assertThat( singleResult ).isInstanceOf( Integer.class );
			assertThat( singleResult ).isEqualTo( 4 );
		} );
	}
}
