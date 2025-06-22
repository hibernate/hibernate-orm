/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Parameter;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.Query;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ExpectedExceptionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @author Chris Cranford
 */
@ExtendWith( ExpectedExceptionExtension.class )
public class ParameterTests extends BaseSqmUnitTest {
	@Test
	@ExpectedException( SemanticException.class )
	public void testInvalidLegacyPositionalParam() {
		// todo (6.0) : should we define the rule with the integer as optional and then give a better exception?
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?" );
	}

	@Test
	@ExpectedException( SemanticException.class )
	public void testZeroBasedPositionalParam() {
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?0" );
	}

	@Test
	@ExpectedException( SemanticException.class )
	public void testNonContiguousPositionalParams() {
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1 or a.numberOfToes = ?3" );

	}

	@Test
	public void testParameterCollection() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1" );
		assertThat( sqm.getSqmParameters(), hasSize( 1 ) );
	}

//	@Test
//	public void testAnticipatedTypeHandling() {
//		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1" );
//		final SqmParameter parameter = sqm.getSqmParameters().iterator().next();
//		assertThat( parameter.getAnticipatedType(), is( instanceOf( BasicSqmPathSource.class ) ) );
//		assertThat( parameter.allowMultiValuedBinding(), is( false ) );
//	}

	@Test
	public void testAllowMultiValuedBinding() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes in (?1)" );
		final SqmParameter<?> parameter = sqm.getSqmParameters().iterator().next();

		assertThat( parameter.allowMultiValuedBinding(), is(true) );
	}

	@Test
	public void testWideningTemporalPrecision() {
		try (Session session = sessionFactory().openSession()) {
			final Query query = session.createQuery( "select p.id from Person p where p.anniversary between :start and :end" );

			query.setParameter( "start", Date.from( Instant.now().minus( 7, ChronoUnit.DAYS ) ), TemporalType.TIMESTAMP );
			query.setParameter( "end", Date.from( Instant.now().plus( 7, ChronoUnit.DAYS ) ), TemporalType.TIMESTAMP );

			final QueryParameterBindings bindings = ( (DomainQueryExecutionContext) query ).getQueryParameterBindings();

			final QueryParameterBinding<?> startBinding = bindings.getBinding( "start" );
			assertThat( startBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.TIMESTAMP ) );

			final QueryParameterBinding<?> endBinding = bindings.getBinding( "end" );
			assertThat( endBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.TIMESTAMP ) );
		}
	}

	@Test
	public void testNarrowingTemporalPrecision() {
		try (Session session = sessionFactory().openSession()) {
			final Query query = session.createQuery( "select p.id from Person p where p.dob between :start and :end" );

			query.setParameter( "start", Instant.now().minus( 7, ChronoUnit.DAYS ), TemporalType.DATE );
			query.setParameter( "end", Instant.now().plus( 7, ChronoUnit.DAYS ), TemporalType.DATE );

			final QueryParameterBindings bindings = ( (DomainQueryExecutionContext) query ).getQueryParameterBindings();

			final QueryParameterBinding<?> startBinding = bindings.getBinding( "start" );
			assertThat( startBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.DATE ) );

			final QueryParameterBinding<?> endBinding = bindings.getBinding( "end" );
			assertThat( endBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.DATE ) );
		}
	}

	@Test
	public void testEmbeddableUseInPredicates() {
		{
			final SqmSelectStatement<?> sqm = interpretSelect( "select p.id from Person p where p.name.firstName = :fname" );
			assertThat( sqm.getSqmParameters().size(), equalTo( 1 ) );
			final SqmParameter<?> parameter = sqm.getSqmParameters().iterator().next();
//			assertThat( parameter.getAnticipatedType(), instanceOf( BasicSqmPathSource.class ) );
		}

		{
			final SqmSelectStatement<?> sqm = interpretSelect( "select p.id from Person p where p.name = :name" );
			assertThat( sqm.getSqmParameters().size(), equalTo( 1 ) );
			final SqmParameter<?> parameter = sqm.getSqmParameters().iterator().next();
//			assertThat( parameter.getAnticipatedType(), instanceOf( EmbeddedSqmPathSource.class ) );
		}

	}

	@Test
	public void testNullParamValues() {
		inTransaction(
				session -> {
					session.createQuery( "from Person p where p.name.firstName = :p" ).setParameter( "p", null ).list();
					session.createQuery( "from Person p where p.name = :p" ).setParameter( "p", null ).list();
					session.createQuery( "from Person p where p.pk = :p" ).setParameter( "p", null ).list();
				}
		);
	}

	@Test
	public void testParamTypes() {
		inTransaction(
				session -> {
					final Set<Parameter<?>> parameters = session.createQuery(
							"from Person p where p.pk = :pk and p.name.firstName like :firstName",
							Person.class
					).getParameters();
					assertThat( parameters.size(), equalTo( 2 ) );
					for ( Parameter<?> parameter : parameters ) {
						switch ( parameter.getName() ) {
							case "pk":
								assertThat( parameter.getParameterType(), equalTo( Integer.class ) );
								break;
							case "firstName":
								assertThat( parameter.getParameterType(), equalTo( String.class ) );
								break;
						}
					}
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15341")
	public void testTransientParamValue() {
		inTransaction(
				session -> {
					try {
						session.createQuery( "from Person p where p.mate = :p" )
								.setParameter( "p", new Person())
								.list();
						Assertions.fail( "Expected TransientObjectException" );
					}
					catch (IllegalStateException ex) {
						assertThat( ex.getCause(), instanceOf( TransientObjectException.class ) );
					}
				}
		);
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Entity( name = "Person" )
	public static class Person {
		@Embeddable
		public static class Name {
			public String firstName;
			public String lastName;
		}

		@Id
		public Integer pk;

		@Embedded
		public Person.Name name;

		public String nickName;

		@Temporal( TemporalType.TIMESTAMP )
		public Instant dob;

		@ManyToOne
		public Person mate;

		@Temporal( TemporalType.DATE )
		public Date anniversary;

		public int numberOfToes;
	}

}
