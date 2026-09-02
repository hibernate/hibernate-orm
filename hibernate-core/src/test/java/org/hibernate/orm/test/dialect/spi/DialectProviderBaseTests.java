/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.spi;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.AbstractSybaseDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.sql.ast.spi.AbstractSelfRenderingExpression;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/// Tests reusable base classes supplied for Dialect providers.
///
/// @author Steve Ebersole
public class DialectProviderBaseTests {
	@Test
	void sybaseBasePreservesVersionAndLockingBehavior() {
		final var dialect = new TestSybaseDialect( DatabaseVersion.make( 17, 1 ) );

		assertThat( dialect.getVersion() ).isEqualTo( DatabaseVersion.make( 17, 1 ) );
		assertThat( dialect.getLockingSupport() ).isSameAs( new SybaseDialect().getLockingSupport() );
	}

	@Test
	void selfRenderingExpressionBaseOwnsTypeAndVisitorDispatch() {
		final JdbcMappingContainer expressionType = mock( JdbcMappingContainer.class );
		final var typedExpression = new TestExpression( expressionType );
		final var untypedExpression = new TestExpression( null );

		assertThat( typedExpression.getExpressionType() ).isSameAs( expressionType );
		assertThat( untypedExpression.getExpressionType() ).isNull();

		final SqlAstWalker walker = mock( SqlAstWalker.class );
		typedExpression.accept( walker );
		verify( walker ).visitSelfRenderingExpression( typedExpression );
		verifyNoMoreInteractions( walker );
	}

	private static final class TestSybaseDialect extends AbstractSybaseDialect {
		private TestSybaseDialect(DatabaseVersion version) {
			super( version );
		}
	}

	private static final class TestExpression extends AbstractSelfRenderingExpression {
		private TestExpression(@Nullable JdbcMappingContainer expressionType) {
			super( expressionType );
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				SessionFactoryImplementor sessionFactory) {
		}
	}
}
