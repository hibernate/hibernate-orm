/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.mutation.multitable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.orm.test.mapping.SecondaryTableTests;
import org.hibernate.orm.test.mapping.inheritance.joined.JoinedInheritanceTest;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.exec.spi.Callback;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Tests for selecting matching ids related to SQM update/select statements.
 *
 * Matching-id-selection is used in CTE- and inline-based strategies.
 *
 * A "functional correctness" test for {@link MatchingIdSelectionHelper#selectMatchingIds}
 *
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT,
		annotatedClasses = {
				SecondaryTableTests.SimpleEntityWithSecondaryTables.class,
				JoinedInheritanceTest.Customer.class,
				JoinedInheritanceTest.DomesticCustomer.class,
				JoinedInheritanceTest.ForeignCustomer.class
		}
)
@ServiceRegistry
@SessionFactory( exportSchema = true )
public class IdSelectionTests {

	@Test
	public void testSecondaryTableRestrictedOnRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement<?> sqm = (SqmDeleteStatement<?>) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete SimpleEntityWithSecondaryTables where name = :n", null );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		((QueryParameterBinding) domainParamBindings.getBinding( "n" ))
				.setBindValue( "abc" );

		scope.inTransaction(
				session -> {
					final DomainQueryExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testSecondaryTableRestrictedOnNonRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement<?> sqm = (SqmDeleteStatement<?>) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete SimpleEntityWithSecondaryTables where data = :d", null );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		((QueryParameterBinding) domainParamBindings.getBinding( "d" ))
				.setBindValue( "123" );

		scope.inTransaction(
				session -> {
					final DomainQueryExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement<?> sqm = (SqmDeleteStatement<?>) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete Customer where name = :n", null );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		((QueryParameterBinding) domainParamBindings.getBinding( "n" ))
				.setBindValue( "Acme" );

		scope.inTransaction(
				session -> {
					final DomainQueryExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnNonPrimaryRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement<?> sqm = (SqmDeleteStatement<?>) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete ForeignCustomer where name = :n", null );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		((QueryParameterBinding) domainParamBindings.getBinding( "n" ))
				.setBindValue( "Acme" );

		scope.inTransaction(
				session -> {
					final DomainQueryExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	@Test
	public void testJoinedSubclassRestrictedOnPrimaryNonRootTable(SessionFactoryScope scope) {
		final SqmDeleteStatement<?> sqm = (SqmDeleteStatement<?>) scope.getSessionFactory()
				.getQueryEngine()
				.getHqlTranslator()
				.translate( "delete ForeignCustomer where vat = :v", null );

		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqm );
		final ParameterMetadataImpl parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );

		final QueryParameterBindingsImpl domainParamBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				scope.getSessionFactory()
		);
		((QueryParameterBinding) domainParamBindings.getBinding( "v" ))
				.setBindValue( "123" );

		scope.inTransaction(
				session -> {
					final DomainQueryExecutionContext executionContext = new TestExecutionContext( session, domainParamBindings );

					MatchingIdSelectionHelper.selectMatchingIds( sqm, domainParameterXref, executionContext );
				}
		);
	}

	private static class TestExecutionContext implements DomainQueryExecutionContext {
		private final SessionImplementor session;
		private final QueryParameterBindingsImpl domainParamBindings;

		public TestExecutionContext(SessionImplementor session, QueryParameterBindingsImpl domainParamBindings) {
			this.session = session;
			this.domainParamBindings = domainParamBindings;
		}

		@Override
		public SharedSessionContractImplementor getSession() {
			return session;
		}

		@Override
		public QueryOptions getQueryOptions() {
			return QueryOptions.NONE;
		}

		@Override
		public QueryParameterBindings getQueryParameterBindings() {
			return domainParamBindings;
		}

		@Override
		public Callback getCallback() {
			return null;
		}
	}
}
