/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

import javax.persistence.criteria.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqmCoalesce<T> extends AbstractSqmExpression<T> implements JpaCoalesce<T> {
	private final SqmFunctionDescriptor functionDescriptor;
	private final List<SqmExpression<? extends T>> arguments;

	public SqmCoalesce(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCoalesce(SqmExpressable<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>();
	}

	public SqmCoalesce(SqmExpressable<T> type, int numberOfArguments, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		functionDescriptor = nodeBuilder.getQueryEngine().getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" );
		this.arguments = new ArrayList<>( numberOfArguments );
	}

	public SqmFunctionDescriptor getFunctionDescriptor() {
		return functionDescriptor;
	}

	public void value(SqmExpression<? extends T> expression) {
		arguments.add( expression );
	}

	public List<SqmExpression<? extends T>> getArguments() {
		return arguments;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCoalesce( this );
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCoalesce<T> value(T value) {
		value( nodeBuilder().literal( value ) );
		return this;
	}

	@Override
	public SqmCoalesce<T> value(Expression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	public SqmCoalesce<T> value(JpaExpression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmCoalesce<T> values(T... values) {
		for ( T value : values ) {
			value( nodeBuilder().literal( value ) );
		}
		return this;
	}

}
