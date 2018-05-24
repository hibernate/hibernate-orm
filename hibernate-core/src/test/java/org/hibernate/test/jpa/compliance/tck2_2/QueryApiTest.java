/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import java.util.Date;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Parameter;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class QueryApiTest extends BaseNonConfigCoreFunctionalTestCase {
	@Entity( name = "Person" )
	@Table( name = "person" )
	public static class Person {
		@Id
		public Integer id;
		String name;
		@Temporal( TemporalType.DATE )
		Date dob;
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );

		sources.addAnnotatedClass( Person.class );
	}

	@Test
	public void testGetParameterNotBound() {
		inTransaction(
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
	public void testGetParameterFromAnotherQuery() {
		inTransaction(
				session -> {
					try {
						// Query
						final QueryImplementor query1 = session.createQuery( "select p from Person p where name = :name1" );
						final QueryImplementor query2 = session.createQuery( "select p from Person p where name = :name2" );

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
	public void testGetParameterValueByUnknownName() {
		inTransaction(
				session -> {
					try {
						// Query
						final QueryImplementor query1 = session.createQuery( "select p from Person p where name = :name1" );

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
	public void testGetParameterValueByUnknownPosition() {
		inTransaction(
				session -> {
					try {
						// Query
						final QueryImplementor query1 = session.createQuery( "select p from Person p" );

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
	public void testSetParameterValueByUnknownReference() {
		inTransaction(
				session -> {
					try {
						// Query
						final QueryImplementor query1 = session.createQuery( "select p from Person p where p.dob < :date1" );
						final QueryImplementor query2 = session.createQuery( "select p from Person p where p.dob < :date2" );

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
	public void testSetInvalidFirstResult() {
		inTransaction(
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
	public void testSetInvalidMaxResults() {
		inTransaction(
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
	public void testUpdateRequiresTxn() {
		inSession(
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
	public void testInvalidQueryMarksTxnForRollback() {
		inSession(
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
