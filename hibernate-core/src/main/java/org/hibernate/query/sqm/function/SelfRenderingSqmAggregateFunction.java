/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Christian Beikov
 */
public class SelfRenderingSqmAggregateFunction<T> extends SelfRenderingSqmFunction<T> {

	private final SqmPredicate filter;

	public SelfRenderingSqmAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			AllowableFunctionReturnType<T> impliedResultType,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( descriptor, renderingSupport, arguments, impliedResultType, returnTypeResolver, nodeBuilder, name );
		this.filter = filter;
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
		final AllowableFunctionReturnType<?> resultType = resolveResultType(
				walker.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		return new SelfRenderingAggregateFunctionSqlAstExpression(
				getFunctionName(),
				getRenderingSupport(),
				resolveSqlAstArguments( getArguments(), walker ),
				filter == null ? null : (Predicate) filter.accept( walker ),
				resultType,
				getMappingModelExpressable( walker, resultType )
		);
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		sb.append( getFunctionName() );
		sb.append( '(' );
		int i = 1;
		if ( arguments.get( 0 ) instanceof SqmDistinct<?> ) {
			( (SqmSelectableNode<?>) arguments.get( 0 ) ).appendHqlString( sb );
			sb.append( ' ' );
			( (SqmSelectableNode<?>) arguments.get( 1 ) ).appendHqlString( sb );
			i = 2;
		}
		for ( ; i < arguments.size(); i++ ) {
			sb.append(", ");
			( (SqmSelectableNode<?>) arguments.get( i ) ).appendHqlString( sb );
		}

		sb.append( ')' );
		if ( filter != null ) {
			sb.append( " filter (where " );
			filter.appendHqlString( sb );
			sb.append( ')' );
		}
	}
}
