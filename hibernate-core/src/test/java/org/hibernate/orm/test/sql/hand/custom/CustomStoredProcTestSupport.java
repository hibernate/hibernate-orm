/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.custom;

import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Abstract test case defining tests of stored procedure support.
 *
 * @author Gail Badner
 */
public abstract class CustomStoredProcTestSupport extends CustomSQLTestSupport {

	@Test
	public void testScalarStoredProcedure(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					ProcedureCall namedQuery = session.createNamedStoredProcedureQuery( "simpleScalar" );
					namedQuery.setParameter( "p_number", 43 );
					List list = namedQuery.getResultList();
					Object[] o = (Object[]) list.get( 0 );
					assertThat( o[0] ).isEqualTo( "getAll" );
					assertThat( o[1] ).isEqualTo( 43L );
				}
		);
	}

	@Test
	public void testParameterHandling(SessionFactoryScope scope) {
//		Query namedQuery = s.getNamedQuery( "paramhandling" );
		scope.inSession(
				session -> {
					ProcedureCall namedQuery = session.createNamedStoredProcedureQuery( "paramhandling" );
					namedQuery.setParameter( 1, 10 );
					namedQuery.setParameter( 2, 20 );
					List list = namedQuery.getResultList();
					Object[] o = (Object[]) list.get( 0 );
					assertThat( o[0] ).isEqualTo( 10L );
					assertThat( o[1] ).isEqualTo( 20L );
				}
		);
	}

	@Test
	public void testMixedParameterHandling(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IllegalArgumentException illegalArgumentException =
							assertThrows( IllegalArgumentException.class, () ->
									session.createNamedStoredProcedureQuery( "paramhandling_mixed" ) );
					assertThat( illegalArgumentException.getMessage() )
							.isEqualTo( "Cannot mix named parameter with positional parameter registrations" );
				}
		);
	}

	@Test
	public void testEntityStoredProcedure(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Organization ifa = new Organization( "IFA" );
					Organization jboss = new Organization( "JBoss" );
					Person gavin = new Person( "Gavin" );
					Employment emp = new Employment( gavin, jboss, "AU" );
					session.persist( ifa );
					session.persist( jboss );
					session.persist( gavin );
					session.persist( emp );
					session.flush();

					//		Query namedQuery = s.getNamedQuery( "selectAllEmployments" );
					ProcedureCall namedQuery = session.createNamedStoredProcedureQuery( "selectAllEmployments" );
					List list = namedQuery.getResultList();
					assertThat( list.get( 0 ) ).isInstanceOf( Employment.class );
					session.remove( emp );
					session.remove( ifa );
					session.remove( jboss );
					session.remove( gavin );
				}
		);
	}
}
