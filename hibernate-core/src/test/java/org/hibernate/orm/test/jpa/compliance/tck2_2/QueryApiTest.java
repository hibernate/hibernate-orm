/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Parameter;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TransactionRequiredException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {QueryApiTest.Person.class})
@SessionFactory
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")})
public class QueryApiTest {
	@Entity( name = "Person" )
	@Table( name = "person" )
	public static class Person {
		@Id
		public Integer id;
		String name;
		@Temporal( TemporalType.DATE )
		Date dob;
	}

	@Test
	public void testGetParameterNotBound(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						session.createQuery( "select p from Person p where name = ?1" )
								.getParameterValue( 1 );
						fail( "expecting failure" );
					}
					catch (IllegalStateException expected) {
						// expected condition
					}

					// TypedQuery
					try {
						session.createQuery( "select p from Person p where name = ?1", Person.class )
								.getParameterValue( 1 );
						fail( "expecting failure" );
					}
					catch (IllegalStateException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testGetParameterFromAnotherQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						final Query query1 = session.createQuery( "select p from Person p where name = :name1" );
						final Query query2 = session.createQuery( "select p from Person p where name = :name2" );

						final Parameter<?> name1 = query1.getParameter( "name1" );
						query2.getParameterValue( name1 );
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}

					// TypedQuery
					try {
						session.createQuery( "select p from Person p where name = ?1", Person.class )
								.getParameterValue( 1 );
						fail( "expecting failure" );
					}
					catch (IllegalStateException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testGetParameterValueByUnknownName(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						final Query query1 = session.createQuery( "select p from Person p where name = :name1" );

						query1.getParameterValue( "name2" );
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testGetParameterValueByUnknownPosition(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						final Query query1 = session.createQuery( "select p from Person p" );

						query1.getParameterValue( 2 );
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testSetParameterValueByUnknownReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						final Query query1 = session.createQuery( "select p from Person p where p.dob < :date1" );
						final Query query2 = session.createQuery( "select p from Person p where p.dob < :date2" );

						final Parameter<?> date2 = query2.getParameter( "date2" );
						query1.setParameter( date2, new Date() );

						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testSetInvalidFirstResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						session.createQuery( "select p from Person p" ).setFirstResult( -3 );
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testSetInvalidMaxResults(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						// Query
						session.createQuery( "select p from Person p where name = ?1" ).setMaxResults( -3 );
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testUpdateRequiresTxn(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						assertFalse( session.getTransaction().isActive() );
						// Query
						session.createQuery( "update Person set name = 'steve'" ).executeUpdate();
						fail( "expecting failure" );
					}
					catch (TransactionRequiredException expected) {
						// expected condition
					}
				}
		);
	}

	@Test
	public void testInvalidQueryMarksTxnForRollback(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						assertFalse( session.getTransaction().isActive() );
						session.getTransaction().begin();

						// Query
						session.createQuery( "invalid" ).list();
						fail( "expecting failure" );
					}
					catch (IllegalArgumentException expected) {
						assertTrue( session.getTransaction().isActive() );
						assertTrue( session.getTransaction().getRollbackOnly() );
					}
				}
		);
	}
}
