/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.AbstractSelfRenderingExpression;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/// Tests community Dialect behavior implemented using provider base classes.
///
/// @author Steve Ebersole
public class ProviderBaseBehaviorTest {
	@Test
	void sqlAnywhereConstructionPathsPreserveVersionAndLockingBehavior() {
		final var versionDialect = new SybaseAnywhereDialect( DatabaseVersion.make( 10 ) );
		final var resolutionDialect = new SybaseAnywhereDialect(
				TestingDialectResolutionInfo.forDatabaseInfo( "Adaptive Server Anywhere", 10, 0 )
		);
		assertThat( versionDialect.getVersion() ).isEqualTo( DatabaseVersion.make( 10 ) );
		assertThat( resolutionDialect.getVersion() ).isEqualTo( DatabaseVersion.make( 10 ) );
		final var versionMetadata = versionDialect.getLockingSupport().getMetadata();
		final var resolutionMetadata = resolutionDialect.getLockingSupport().getMetadata();
		assertThat( versionMetadata.getPessimisticLockStyle() )
				.isEqualTo( resolutionMetadata.getPessimisticLockStyle() );
		assertThat( versionMetadata.getReadRowLockStrategy() )
				.isEqualTo( resolutionMetadata.getReadRowLockStrategy() );
		assertThat( versionMetadata.getWriteRowLockStrategy() )
				.isEqualTo( resolutionMetadata.getWriteRowLockStrategy() );
		assertThat( versionMetadata.getOuterJoinLockingType() )
				.isEqualTo( resolutionMetadata.getOuterJoinLockingType() );
	}

	@Test
	void gaussWrapperRetainsTypeAndRendering() {
		final JdbcMappingContainer expressionType = mock( JdbcMappingContainer.class );
		final var nested = new AbstractSelfRenderingExpression( expressionType ) {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> translator,
					SessionFactoryImplementor sessionFactory) {
				sqlAppender.appendSql( "nested_value" );
			}
		};
		final var wrapped = GaussDBCastingIntervalSecondJdbcType.INSTANCE
				.wrapTopLevelSelectionExpression( nested );
		final var sql = new StringBuilder();
		final var appender = new StringBuilderSqlAppender( sql );
		final SqlAstTranslator<?> translator = mock( SqlAstTranslator.class );
		doAnswer( invocation -> {
			((SelfRenderingExpression) invocation.getArgument( 0 )).renderToSql( appender, translator, null );
			return null;
		} ).when( translator ).visitSelfRenderingExpression( nested );

		((SelfRenderingExpression) wrapped).renderToSql( appender, translator, null );

		assertThat( wrapped ).isInstanceOf( AbstractSelfRenderingExpression.class );
		assertThat( wrapped.getExpressionType() ).isSameAs( expressionType );
		assertThat( sql ).hasToString( "extract(epoch from nested_value)" );
	}

	@Test
	void gaussWrapperPreservesNullType() {
		final var nested = new AbstractSelfRenderingExpression( null ) {
			@Override
			public void renderToSql(
					SqlAppender sqlAppender,
					SqlAstTranslator<?> translator,
					SessionFactoryImplementor sessionFactory) {
			}
		};

		assertThat( GaussDBCastingIntervalSecondJdbcType.INSTANCE
				.wrapTopLevelSelectionExpression( nested )
				.getExpressionType() ).isNull();
	}
}
