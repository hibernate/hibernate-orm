/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.List;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * A SQM set-returning function
 *
 * @since 7.0
 */
@Incubating
public abstract class SqmSetReturningFunction<T> extends AbstractSqmNode
		implements SqmVisitableNode, JpaSetReturningFunction<T> {
	// this function-name is the one used to resolve the descriptor from
	// the function registry (which may or may not be a db function name)
	private final String functionName;
	private final SqmSetReturningFunctionDescriptor functionDescriptor;

	private final List<? extends SqmTypedNode<?>> arguments;

	public SqmSetReturningFunction(
			String functionName,
			SqmSetReturningFunctionDescriptor functionDescriptor,
			List<? extends SqmTypedNode<?>> arguments,
			NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.functionName = functionName;
		this.functionDescriptor = functionDescriptor;
		this.arguments = arguments;
	}

	@Override
	public abstract SqmSetReturningFunction<T> copy(SqmCopyContext context);

	public SqmSetReturningFunctionDescriptor getFunctionDescriptor() {
		return functionDescriptor;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	public abstract AnonymousTupleType<T> getType();

	public List<? extends SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	public abstract TableGroup convertToSqlAst(
			NavigablePath navigablePath,
			String identifierVariable,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean withOrdinality,
			SqmToSqlAstConverter walker);

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSetReturningFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( functionName );
		if ( arguments.isEmpty() ) {
			hql.append( "()" );
		}
		else {
			hql.append( '(' );
			arguments.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < arguments.size(); i++ ) {
				hql.append( ", " );
				arguments.get( i ).appendHqlString( hql, context );
			}
			hql.append( ')' );
		}
	}

	@Override
	// TODO: override on all subtypes
	public boolean equals(Object other) {
		return other instanceof SqmSetReturningFunction<?> that
			&& Objects.equals( this.functionName, that.functionName )
			&& Objects.equals( this.arguments, that.arguments )
			&& this.getClass() == that.getClass()
			&& Objects.equals( this.toHqlString(), that.toHqlString() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( functionName, arguments, getClass() );
	}
}
