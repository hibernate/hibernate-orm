/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.expression.SqmSelfRenderingExpression;
import org.hibernate.sql.ast.spi.creation.SqlTreeCreationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel
@SessionFactory
class SqmSqlAstNullabilityTest {
	@Test
	void missingSqlOperandIsRejected(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = (NodeBuilder) session.getCriteriaBuilder();
			final var expression = new SqmSelfRenderingExpression<Integer>(
					walker -> null, builder.getIntegerType(), builder );
			final var query = builder.createQuery( Integer.class );
			query.select( builder.sum( expression, builder.literal( 1 ) ) );
			final var exception = assertThrows( SqlTreeCreationException.class,
					() -> session.createQuery( query ).getSingleResult() );
			assertEquals( "No SQL AST result for " + SqmSelfRenderingExpression.class.getName(), exception.getMessage() );
		} );
	}

	@Test
	void sqlNullOperandIsAllowed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Integer.class );
			query.select( builder.sum( builder.nullLiteral( Integer.class ), builder.literal( 1 ) ) );
			assertNull( session.createQuery( query ).getSingleResult() );
		} );
	}
}
