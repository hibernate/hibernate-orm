/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.mutation;

import java.util.ArrayList;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;
import org.hibernate.sql.spi.mutation.jdbc.UpsertOperation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests the immutable request and operation-state guarantees exposed by the
/// post-translation mutation SPI.
///
/// @author Steve Ebersole
public class MutationOperationContractTests {
	@Test
	void translationRequestRejectsNullsAndDerivesItsTarget() {
		final OptionalTableUpdate update = mock( OptionalTableUpdate.class );
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final MutationTarget mutationTarget = mock( MutationTarget.class );
		when( update.getMutationTarget() ).thenReturn( mutationTarget );

		final var request = new OptionalTableUpdateOperationRequest( update, sessionFactory, true );
		assertThat( request.mutationTarget() ).isSameAs( mutationTarget );
		assertThat( request.versionedTarget() ).isTrue();

		assertThatNullPointerException()
				.isThrownBy( () -> new OptionalTableUpdateOperationRequest( null, sessionFactory, false ) );
		assertThatNullPointerException()
				.isThrownBy( () -> new OptionalTableUpdateOperationRequest( update, null, false ) );
	}

	@Test
	void jdbcMutationSnapshotsItsParameterBinders() {
		final ColumnValueParameter parameter = mock( ColumnValueParameter.class );
		final ColumnReference columnReference = mock( ColumnReference.class );
		when( parameter.getColumnReference() ).thenReturn( columnReference );
		when( columnReference.getColumnExpression() ).thenReturn( "id" );
		when( parameter.getUsage() ).thenReturn( ParameterUsage.SET );

		final var binders = new ArrayList<JdbcParameterBinder>();
		binders.add( parameter );
		final var operation = new JdbcInsertMutation(
				mock( TableMapping.class ),
				mock( MutationTarget.class ),
				"insert into example values (?)",
				false,
				new Expectation.RowCount(),
				binders
		);

		binders.clear();
		assertThat( operation.getParameterBinders() ).hasSize( 1 );
		assertThat( operation.getParameterBinders().get( 0 ) ).isSameAs( parameter );
	}

	@Test
	void columnValueParameterDeclaresItsProviderFacingMethods() throws NoSuchMethodException {
		assertThat( ColumnValueParameter.class.getDeclaredMethod( "accept", SqlAstWalker.class ).getDeclaringClass() )
				.isEqualTo( ColumnValueParameter.class );
		assertThat( ColumnValueParameter.class.getDeclaredMethod( "getParameterBinder" ).getDeclaringClass() )
				.isEqualTo( ColumnValueParameter.class );
		assertThat( ColumnValueParameter.class.getDeclaredMethod( "getParameterId" ).getDeclaringClass() )
				.isEqualTo( ColumnValueParameter.class );
		assertThat( ColumnValueParameter.class.getDeclaredMethod( "getJdbcMapping" ).getDeclaringClass() )
				.isEqualTo( ColumnValueParameter.class );

		final JdbcMapping jdbcMapping = mock( JdbcMapping.class );
		final ColumnReference columnReference = mock( ColumnReference.class );
		when( columnReference.getJdbcMapping() ).thenReturn( jdbcMapping );
		final ColumnValueParameter parameter = new ColumnValueParameter( columnReference );
		final SqlAstWalker sqlAstWalker = mock( SqlAstWalker.class );

		assertThat( parameter.getParameterBinder() ).isSameAs( parameter );
		assertThat( parameter.getParameterId() ).isNull();
		assertThat( parameter.getJdbcMapping() ).isSameAs( jdbcMapping );
		parameter.accept( sqlAstWalker );
		verify( sqlAstWalker ).visitParameter( parameter );
	}

	@Test
	void deleteOrUpsertDerivesStateAndExposesItsConstituents() {
		final MutationTarget mutationTarget = mock( MutationTarget.class );
		final TableMapping tableMapping = mock( TableMapping.class );
		final UpsertOperation upsertOperation = mock( UpsertOperation.class );
		final OptionalTableUpdate optionalTableUpdate = mock( OptionalTableUpdate.class );
		when( upsertOperation.getMutationTarget() ).thenReturn( mutationTarget );
		when( upsertOperation.getTableDetails() ).thenReturn( tableMapping );

		final var operation = new DeleteOrUpsertOperation( upsertOperation, optionalTableUpdate );
		assertThat( operation.getMutationTarget() ).isSameAs( mutationTarget );
		assertThat( operation.getTableDetails() ).isSameAs( tableMapping );
		assertThat( operation.getUpsertOperation() ).isSameAs( upsertOperation );
		assertThat( operation.getOptionalTableUpdate() ).isSameAs( optionalTableUpdate );
	}
}
