/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Gavin King
 */
public class TimestampaddFunction implements SqmFunctionDescriptor {
	private Dialect dialect;

	public TimestampaddFunction(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> arguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		StandardArgumentsValidators.exactly( 3 ).validate( arguments );

		final ExtractUnit field = (ExtractUnit) arguments.get( 0 );
		final Expression magnitude = (Expression) arguments.get( 1 );
		final Expression to = (Expression) arguments.get( 2 );
		final TemporalUnit unit = field.getUnit();

		final AbstractSqmFunctionDescriptor functionDescriptor = new AbstractSqmFunctionDescriptor(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 ) ) {
			@Override
			public FunctionRenderingSupport getRenderingSupport() {
				return (sqlAppender, fn, sqlAstArguments, walker, sessionFactory) -> dialect.timestampadd(
						unit,
						() -> magnitude.accept( walker ),
						() -> to.accept( walker ),
						sqlAppender::appendSql,
						TypeConfiguration.isSqlTimestampType( to.getExpressionType() )
				);
			}
		};

		return functionDescriptor.generateSqlExpression( functionName, arguments, inferableTypeAccess, converter, creationState );
	}
}
