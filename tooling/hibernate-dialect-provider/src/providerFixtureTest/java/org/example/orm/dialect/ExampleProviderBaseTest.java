/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;

import org.hibernate.dialect.AbstractSybaseDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.AbstractSelfRenderingExpression;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Exercises reusable base classes from a standalone Dialect provider.
///
/// @author Steve Ebersole
public class ExampleProviderBaseTest {
	@Test
	void sybaseFixturePreservesVersionAndLockingBehavior() {
		final var dialect = new ExampleSybaseDialect();

		assertSame( AbstractSybaseDialect.class, dialect.getClass().getSuperclass() );
		assertEquals( DatabaseVersion.make( 17 ), dialect.getVersion() );
		assertSame( ExampleLockingSupport.INSTANCE, dialect.getLockingSupport() );
	}

	@Test
	void expressionFixtureRetainsTypeAndRendering() {
		final JdbcMappingContainer expressionType = (JdbcMappingContainer) Proxy.newProxyInstance(
				JdbcMappingContainer.class.getClassLoader(),
				new Class<?>[] { JdbcMappingContainer.class },
				(proxy, method, arguments) -> null
		);
		final var expression = new ExampleSelfRenderingExpression( expressionType );
		final var sql = new StringBuilder();

		expression.renderToSql( new StringBuilderSqlAppender( sql ), null, null );

		assertSame( AbstractSelfRenderingExpression.class, expression.getClass().getSuperclass() );
		assertSame( expressionType, expression.getExpressionType() );
		assertEquals( "fixture_expression", sql.toString() );
		assertNull( new ExampleSelfRenderingExpression( null ).getExpressionType() );
	}
}
