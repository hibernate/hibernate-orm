/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcOperations;
import org.hibernate.sql.exec.spi.JdbcPaginationApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests the provider-facing JDBC operation builders without depending on their
/// Hibernate-owned implementations.
///
/// @author Steve Ebersole
public class JdbcOperationsTests {
	@Test
	void buildsSelectUsingTheRequestMappingAndDefensiveMetadataCopies() {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final SelectStatement statement = mock( SelectStatement.class );
		final JdbcValuesMappingProducerProvider mappingProvider = mock( JdbcValuesMappingProducerProvider.class );
		final JdbcValuesMappingProducer mappingProducer = mock( JdbcValuesMappingProducer.class );
		final JdbcParameterBinder binder = mock( JdbcParameterBinder.class );
		final JdbcParameter limitParameter = mock( JdbcParameter.class );
		final Set<String> querySpaces = new HashSet<>( Set.of( "books" ) );

		when( sessionFactory.getJdbcValuesMappingProducerProvider() ).thenReturn( mappingProvider );
		when( mappingProvider.buildMappingProducer( statement, sessionFactory ) ).thenReturn( mappingProducer );

		final JdbcSelect operation = JdbcOperations.select(
				new SqlAstTranslationRequest.Select( sessionFactory, statement )
		)
				.command( "{ \"find\": \"books\" }" )
				.parameterBinders( List.of( binder ) )
				.affectedQuerySpaces( querySpaces )
				.rowsToSkip( 4 )
				.maxRows( 12 )
				.lockingApplication( JdbcLockingApplication.RENDERED )
				.paginationApplication( JdbcPaginationApplication.JDBC )
				.limitParameter( limitParameter )
				.scrollExecution( true )
				.build();

		querySpaces.add( "authors" );

		assertEquals( "{ \"find\": \"books\" }", operation.getSqlString() );
		assertEquals( List.of( binder ), operation.getParameterBinders() );
		assertEquals( Set.of( "books" ), operation.getAffectedTableNames() );
		assertSame( mappingProducer, operation.getJdbcValuesMappingProducer() );
		assertEquals( 4, operation.getRowsToSkip() );
		assertEquals( 12, operation.getMaxRows() );
		assertEquals( JdbcLockingApplication.RENDERED, operation.getLockingApplication() );
		assertEquals( JdbcPaginationApplication.JDBC, operation.getPaginationApplication() );
		assertSame( limitParameter, operation.getLimitParameter() );
		assertTrue( operation.usesLimitParameters() );
		verify( mappingProvider ).buildMappingProducer( statement, sessionFactory );
	}

	@Test
	void usesAnExplicitSelectMappingWithoutConsultingTheRequestFactory() {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final SelectStatement statement = mock( SelectStatement.class );
		final JdbcValuesMappingProducer mappingProducer = mock( JdbcValuesMappingProducer.class );

		final JdbcSelect operation = JdbcOperations.select(
				new SqlAstTranslationRequest.Select( sessionFactory, statement )
		)
				.command( "select-command" )
				.jdbcValuesMappingProducer( mappingProducer )
				.build();

		assertSame( mappingProducer, operation.getJdbcValuesMappingProducer() );
	}

	@Test
	void buildsTypedInsertAndUpdateOperations() {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final JdbcParameter parameter = mock( JdbcParameter.class );
		final JdbcParameterBinding binding = mock( JdbcParameterBinding.class );
		final Map<JdbcParameter, JdbcParameterBinding> appliedBindings = new HashMap<>();
		appliedBindings.put( parameter, binding );

		final JdbcOperationQueryMutation insert = JdbcOperations.queryMutation(
				new SqlAstTranslationRequest.QueryMutation(
						sessionFactory,
						mock( InsertSelectStatement.class )
				)
		)
				.command( "insert-command" )
				.appliedParameterBindings( appliedBindings )
				.uniqueConstraintNameThatMayFail( "UK_books" )
				.build();

		appliedBindings.clear();

		assertTrue( insert instanceof JdbcOperationQueryInsert );
		assertEquals( "UK_books", ( (JdbcOperationQueryInsert) insert ).getUniqueConstraintNameThatMayFail() );
		assertEquals( binding, insert.getAppliedParameters().get( parameter ) );

		final JdbcOperationQueryMutation update = JdbcOperations.queryMutation(
				new SqlAstTranslationRequest.QueryMutation( sessionFactory, mock( UpdateStatement.class ) )
		)
				.command( "update-command" )
				.build();

		assertFalse( update instanceof JdbcOperationQueryInsert );
		assertEquals( "update-command", update.getSqlString() );
	}

	@Test
	void rejectsInsertOnlyMetadataForOtherMutationKinds() {
		final SqlAstTranslationRequest.QueryMutation request = new SqlAstTranslationRequest.QueryMutation(
				mock( SessionFactoryImplementor.class ),
				mock( UpdateStatement.class )
		);

		assertThrows(
				IllegalStateException.class,
				() -> JdbcOperations.queryMutation( request )
						.command( "update-command" )
						.uniqueConstraintNameThatMayFail( "UK_books" )
						.build()
		);
	}

	@Test
	void requiresACommandAndValidRowBounds() {
		final SqlAstTranslationRequest.Select request = new SqlAstTranslationRequest.Select(
				mock( SessionFactoryImplementor.class ),
				mock( SelectStatement.class )
		);

		assertThrows( IllegalStateException.class, () -> JdbcOperations.select( request ).build() );
		assertThrows( IllegalArgumentException.class, () -> JdbcOperations.select( request ).rowsToSkip( -1 ) );
		assertThrows( IllegalArgumentException.class, () -> JdbcOperations.select( request ).maxRows( -1 ) );
	}
}
