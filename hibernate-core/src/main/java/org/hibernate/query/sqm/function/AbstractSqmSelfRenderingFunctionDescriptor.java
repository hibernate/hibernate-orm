/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
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

import java.util.List;
import java.util.function.Consumer;

/**
 * A simplified {@code SqmFunctionDescriptor} which delegates SQL rendering to
 * an instance of {@link FunctionRenderingSupport}.
 *
 * @author Gavin King
 */
public abstract class AbstractSqmSelfRenderingFunctionDescriptor
		extends AbstractSqmFunctionDescriptor {

	public AbstractSqmSelfRenderingFunctionDescriptor(
			String functionName,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( functionName, argumentsValidator, returnTypeResolver );
	}

	/**
	 * Create an {@link Expression} that calls our {@link FunctionRenderingSupport}.
	 */
	protected Expression generateFunctionExpression(List<SqlAstNode> sqlAstArgs, BasicValuedMapping returnType) {
		return new SelfRenderingSqlFunctionExpression(
				getFunctionName(),
				returnType,
				sqlAstArgs,
				getRenderingSupport()
		);
	}

	/***
	 * The {@link FunctionRenderingSupport} which renders this function.
	 */
	protected abstract FunctionRenderingSupport getRenderingSupport();

	/**
	 * An SQM {@link SelfRenderingExpression} that delegates to an instancee of
	 * {@link FunctionRenderingSupport} to actually render the SQL representation
	 * of the function invocation.
	 */
	static class SelfRenderingSqlFunctionExpression implements SelfRenderingExpression, DomainResultProducer {
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
