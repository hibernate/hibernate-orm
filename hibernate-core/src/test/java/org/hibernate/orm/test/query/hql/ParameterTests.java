/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.Query;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;

import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ExpectedExceptionExtension;
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
@SuppressWarnings("WeakerAccess")
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

	@Test
	public void testAnticipatedTypeHandling() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1" );
		final SqmParameter parameter = sqm.getSqmParameters().iterator().next();
		assertThat( parameter.getAnticipatedType(), is( instanceOf( BasicSqmPathSource.class ) ) );
		assertThat( parameter.allowMultiValuedBinding(), is( false ) );
	}

	@Test
	public void testAllowMultiValuedBinding() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes in (?1)" );
		final SqmParameter parameter = sqm.getSqmParameters().iterator().next();

		assertThat( parameter.allowMultiValuedBinding(), is(true) );
	}

	@Test
	public void testWideningTemporalPrecision() {
		try (Session session = sessionFactory().openSession()) {
			final Query query = session.createQuery( "select p.id from Person p where p.anniversary between :start and :end" );

			query.setParameter( "start", Instant.now().minus( 7, ChronoUnit.DAYS ), TemporalType.TIMESTAMP );
			query.setParameter( "end", Instant.now().plus( 7, ChronoUnit.DAYS ), TemporalType.TIMESTAMP );

			final QueryParameterBindings bindings = ( (DomainParameterBindingContext) query ).getQueryParameterBindings();

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

			final QueryParameterBindings bindings = ( (DomainParameterBindingContext) query ).getQueryParameterBindings();

			final QueryParameterBinding<?> startBinding = bindings.getBinding( "start" );
			assertThat( startBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.DATE ) );

			final QueryParameterBinding<?> endBinding = bindings.getBinding( "end" );
			assertThat( endBinding.getExplicitTemporalPrecision(), equalTo( TemporalType.DATE ) );
		}
	}

	@Test
	public void testEmbeddableUseInPredicates() {
		{
			final SqmSelectStatement<?> sqm = interpretSelect( "select p.id from Person p where p.name.first = :fname" );
			assertThat( sqm.getSqmParameters().size(), equalTo( 1 ) );
			final SqmParameter<?> parameter = sqm.getSqmParameters().iterator().next();
			assertThat( parameter.getAnticipatedType(), instanceOf( BasicSqmPathSource.class ) );
		}

		{
			final SqmSelectStatement<?> sqm = interpretSelect( "select p.id from Person p where p.name = :name" );
			assertThat( sqm.getSqmParameters().size(), equalTo( 1 ) );
			final SqmParameter<?> parameter = sqm.getSqmParameters().iterator().next();
			assertThat( parameter.getAnticipatedType(), instanceOf( EmbeddedSqmPathSource.class ) );
		}

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
			public String first;
			public String last;
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
