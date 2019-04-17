/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmCoalesceFunction<T> extends AbstractSqmFunction<T> implements SqmFunction<T>, JpaCoalesce<T> {
	public static final String NAME = "coalesce";

	private List<SqmExpression<T>> arguments = new ArrayList<>();

	public SqmCoalesceFunction(NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmCoalesceFunction(AllowableFunctionReturnType<T> type, NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		if ( getExpressableType() == null ) {
			return null;
		}

		return getExpressableType().getJavaTypeDescriptor();
	}

	public List<SqmExpression<T>> getArguments() {
		return arguments;
	}

	public void value(SqmExpression<T> expression) {
		arguments.add( expression );

//		if ( getExpressableType() == null ) {
			setExpressableType( expression.getExpressableType() );
//		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCoalesceFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCoalesceFunction<T> value(T value) {
		value( nodeBuilder().literal( value ) );
		return this;
	}

	@Override
	public SqmCoalesceFunction<T> value(Expression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	public SqmCoalesceFunction<T> value(JpaExpression<? extends T> value) {
		//noinspection unchecked
		value( (SqmExpression) value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmCoalesceFunction<T> values(T... values) {
		for ( T value : values ) {
			value( nodeBuilder().literal( value ) );
		}
		return this;
	}

}
