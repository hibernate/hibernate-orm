/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;

/**
 * Acts as a wrapper to another SqmFunctionTemplate - upon rendering uses the
 * standard JDBC escape sequence (i.e. `{fn blah}`) when rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class JdbcEscapeFunctionDescriptor implements SqmFunctionDescriptor {
	private final SqmFunctionDescriptor wrapped;

	@SuppressWarnings("WeakerAccess")
	public JdbcEscapeFunctionDescriptor(SqmFunctionDescriptor wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> sqmArguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		final Expression wrappedExpression = wrapped.generateSqlExpression(
				functionName,
				sqmArguments,
				inferableTypeAccess,
				converter,
				creationState
		);

		return new EscapeExpression( wrappedExpression );
	}

	private static class EscapeExpression implements SelfRenderingExpression {
		private final Expression wrappedExpression;

		EscapeExpression(Expression wrappedExpression) {
			this.wrappedExpression = wrappedExpression;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( "{fn " );
			wrappedExpression.accept( walker );
			sqlAppender.appendSql( "}" );
		}

		@Override
		public MappingModelExpressable getExpressionType() {
			return wrappedExpression.getExpressionType();
		}
	}
}
