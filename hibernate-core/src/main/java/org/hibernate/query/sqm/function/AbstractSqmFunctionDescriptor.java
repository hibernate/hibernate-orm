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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Support for SQM function descriptors which ultimately render themselves
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionDescriptor implements SqmSelfRenderingFunctionDescriptor {
	private final ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;

	public AbstractSqmFunctionDescriptor(ArgumentsValidator argumentsValidator, FunctionReturnTypeResolver returnTypeResolver) {
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
		this.returnTypeResolver = returnTypeResolver == null
				? (impliedTypeAccess, arguments) -> impliedTypeAccess.get()
				: returnTypeResolver;
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

		final BasicValuedMapping returnType = returnTypeResolver.resolveFunctionReturnType(
				() -> (BasicValuedMapping) inferableTypeAccess.get(),
				sqlAstArgs
		);


		return new SelfRenderingSqlFunctionExpression(
				resolveFunctionName( functionName ),
				returnType,
				sqlAstArgs,
				getRenderingSupport()
		);
	}

	protected String resolveFunctionName(String functionName) {
		return functionName;
	}

	public String getSignature(String name) {
		return getReturnSignature() + name + getArgumentListSignature();
	}

	public String getReturnSignature() {
		String result = returnTypeResolver.getResult();
		return result.isEmpty() ? "" : result + " ";
	}

	public String getArgumentListSignature() {
		String args = argumentsValidator.getSignature();
		return alwaysIncludesParentheses() ? args : "()".equals(args) ? "" : "[" + args + "]";
	}

	private static class SelfRenderingSqlFunctionExpression implements SelfRenderingExpression, DomainResultProducer {
		private final String name;
		private final BasicValuedMapping type;
		private final List<SqlAstNode> arguments;

		private final FunctionRenderingSupport renderingSupport;

		public SelfRenderingSqlFunctionExpression(
				String name,
				BasicValuedMapping type,
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
		public BasicValuedMapping getExpressionType() {
			return type;
		}

		@Override
		public void visitJdbcTypes(
				Consumer action,
				TypeConfiguration typeConfiguration) {
			getExpressionType().visitJdbcTypes( action, Clause.IRRELEVANT, typeConfiguration );
		}

		@Override
		public DomainResult createDomainResult(
				String resultVariable,
				DomainResultCreationState creationState) {
			final SqlSelection sqlSelection = resolveSqlSelection( creationState );
			return new BasicResult(
					sqlSelection.getValuesArrayPosition(),
					resultVariable,
					type.getMappedTypeDescriptor().getMappedJavaTypeDescriptor()
			);
		}

		private SqlSelection resolveSqlSelection(DomainResultCreationState creationState) {
			return creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
					this,
					type.getJdbcMapping().getJavaTypeDescriptor(),
					creationState.getSqlAstCreationState()
							.getCreationContext()
							.getSessionFactory()
							.getTypeConfiguration()
			);
		}

		@Override
		public void applySqlSelections(DomainResultCreationState creationState) {
			resolveSqlSelection( creationState );
		}

		@Override
		public SqlSelection createSqlSelection(
				int jdbcPosition,
				int valuesArrayPosition,
				JavaTypeDescriptor javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return new SqlSelectionImpl(
					jdbcPosition,
					valuesArrayPosition,
					this,
					getExpressionType().getJdbcMapping()
			);
		}
	}
}
