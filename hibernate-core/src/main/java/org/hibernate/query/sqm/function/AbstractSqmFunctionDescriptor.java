/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;

/**
 * Support for SQM function descriptors which ultimately render themselves
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionDescriptor implements SqmFunctionDescriptor {
	private final ArgumentsValidator argumentsValidator;

	public AbstractSqmFunctionDescriptor() {
		this( null );
	}

	public AbstractSqmFunctionDescriptor(ArgumentsValidator argumentsValidator) {
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
	}

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> sqmArguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		argumentsValidator.validate( sqmArguments );

		// todo (6.0) : work out the specifics of the type resolution

		final List<SqlAstNode> sqlAstArgs;
		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
			sqlAstArgs = Collections.emptyList();
		}
		else if ( sqmArguments.size() == 1 ) {
			sqlAstArgs = Collections.singletonList(
					(SqlAstNode) sqmArguments.get( 0 ).accept( converter )
			);
		}
		else {
			sqlAstArgs = new ArrayList<>( sqmArguments.size() );
			for ( int i = 0; i < sqmArguments.size(); i++ ) {
				final SqmVisitableNode sqmVisitableNode = sqmArguments.get( i );
				sqlAstArgs.add( (SqlAstNode) sqmVisitableNode.accept( converter ) );
			}
		}

		return new SelfRenderingSqlFunctionExpression(
				functionName,
				inferableTypeAccess.get(),
				sqlAstArgs,
				getRenderingSupport()
		);
	}

	protected abstract FunctionRenderingSupport getRenderingSupport();


	private static class SelfRenderingSqlFunctionExpression implements SelfRenderingExpression {
		private final String name;
		private final MappingModelExpressable type;
		private final List<SqlAstNode> arguments;

		private final FunctionRenderingSupport renderingSupport;

		public SelfRenderingSqlFunctionExpression(
				String name,
				MappingModelExpressable type,
				List<SqlAstNode> arguments,
				FunctionRenderingSupport renderingSupport) {
			this.name = name;
			this.type = type;
			this.arguments = arguments;
			this.renderingSupport = renderingSupport;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			renderingSupport.render( sqlAppender, name, arguments, walker, sessionFactory );
		}

		@Override
		public MappingModelExpressable getExpressionType() {
			return type;
		}
	}
}
