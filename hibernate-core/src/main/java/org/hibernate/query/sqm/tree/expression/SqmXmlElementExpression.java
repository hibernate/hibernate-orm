/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.criteria.JpaXmlElementExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the xmlelement function that also captures special syntax elements like xmlattributes.
 *
 * @since 7.0
 */
@Incubating
public class SqmXmlElementExpression extends SelfRenderingSqmFunction<String> implements JpaXmlElementExpression {

	public SqmXmlElementExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<String> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
	}

	@Override
	public SqmXmlElementExpression attribute(String attributeName, Expression<?> expression) {
		//noinspection unchecked
		final List<SqmTypedNode<?>> arguments = (List<SqmTypedNode<?>>) getArguments();
		if ( arguments.size() > 1 && arguments.get( 1 ) instanceof SqmXmlAttributesExpression attributesExpression ) {
			attributesExpression.attribute( attributeName, expression );
		}
		else {
			arguments.add( 1, new SqmXmlAttributesExpression( attributeName, expression ) );
		}
		return this;
	}

	@Override
	public SqmXmlElementExpression content(Expression<?>... expressions) {
		return content( Arrays.asList(expressions) );
	}

	@Override
	public SqmXmlElementExpression content(List<? extends Expression<?>> expressions) {
		//noinspection unchecked
		final List<SqmTypedNode<?>> arguments = (List<SqmTypedNode<?>>) getArguments();
		int contentIndex = 1;
		if ( arguments.size() > contentIndex ) {
			if ( arguments.get( contentIndex ) instanceof SqmXmlAttributesExpression ) {
				contentIndex++;
			}
			while ( contentIndex < arguments.size() ) {
				arguments.remove( arguments.size() - 1 );
			}
		}
		for ( Expression<?> expression : expressions ) {
			arguments.add( (SqmTypedNode<?>) expression );
		}
		return this;
	}

	@Override
	public SqmXmlElementExpression copy(SqmCopyContext context) {
		final SqmXmlElementExpression existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmXmlElementExpression(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getImpliedResultType(),
						getArgumentsValidator(),
						getReturnTypeResolver(),
						nodeBuilder(),
						getFunctionName()
				)
		);
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		final List<? extends SqmTypedNode<?>> arguments = getArguments();
		hql.append( "xmlelement(name " );
		arguments.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < arguments.size(); i++ ) {
			hql.append( ',' );
			arguments.get( i ).appendHqlString( hql, context );
		}
		hql.append( ')' );
	}
}
