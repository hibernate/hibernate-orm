/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.derived.AnonymousTupleType;
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
public abstract class SqmSetReturningFunction<T> extends AbstractSqmNode implements SqmVisitableNode,
		JpaSetReturningFunction<T> {
	// this function-name is the one used to resolve the descriptor from
	// the function registry (which may or may not be a db function name)
	private final String functionName;
	private final SqmSetReturningFunctionDescriptor functionDescriptor;

	private final AnonymousTupleType<T> type;
	private final List<? extends SqmTypedNode<?>> arguments;

	public SqmSetReturningFunction(
			String functionName,
			SqmSetReturningFunctionDescriptor functionDescriptor,
			AnonymousTupleType<T> type,
			List<? extends SqmTypedNode<?>> arguments,
			NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.functionName = functionName;
		this.functionDescriptor = functionDescriptor;
		this.type = type;
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

	public AnonymousTupleType<T> getType() {
		return type;
	}

	public List<? extends SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	public abstract TableGroup convertToSqlAst(
			NavigablePath navigablePath,
			String identifierVariable,
			boolean lateral,
			boolean canUseInnerJoins,
			SqmToSqlAstConverter walker);

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSetReturningFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( functionName );
		if ( arguments.isEmpty() ) {
			sb.append( "()" );
			return;
		}
		sb.append( '(' );
		arguments.get( 0 ).appendHqlString( sb );
		for ( int i = 1; i < arguments.size(); i++ ) {
			sb.append( ", " );
			arguments.get( i ).appendHqlString( sb );
		}

		sb.append( ')' );
	}
}
