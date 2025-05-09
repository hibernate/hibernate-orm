/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

import static org.hibernate.internal.util.QuotingHelper.appendSingleQuoteEscapedString;

/**
 * Represents a literal value in the sqm, e.g.<ul>
 *     <li>1</li>
 *     <li>'some string'</li>
 *     <li>some.JavaClass.CONSTANT</li>
 *     <li>some.JavaEnum.VALUE</li>
 *     <li>etc</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class SqmLiteral<T> extends AbstractSqmExpression<T> {
	private final T value;

	public SqmLiteral(T value, SqmExpressible<? super T> inherentType, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		assert value != null;
		assert inherentType == null
			|| inherentType.getExpressibleJavaType().isInstance( value );
		this.value = value;
	}

	protected SqmLiteral(SqmExpressible<T> inherentType, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.value = null;
	}

	@Override
	public SqmLiteral<T> copy(SqmCopyContext context) {
		final SqmLiteral<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		else {
			final SqmLiteral<T> expression =
					context.registerCopy( this,
							new SqmLiteral<>( getLiteralValue(), getNodeType(), nodeBuilder() ) );
			copyTo( expression, context );
			return expression;
		}
	}

	public T getLiteralValue() {
		return value;
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitLiteral( this );
	}

	@Override
	public String asLoggableText() {
		return "Literal( " + getLiteralValue() + ")";
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		appendHqlString( hql, getJavaTypeDescriptor(), getLiteralValue() );
	}

	public static <T> void appendHqlString(StringBuilder sb, @Nullable JavaType<T> javaType, @Nullable T value) {
		if ( value == null ) {
			sb.append( "null" );
		}
		else {
			if ( javaType == null ) {
				throw new IllegalArgumentException( "Can't render value because java type is null" );
			}
			if ( value instanceof Number || value instanceof Boolean ) {
				// We know these are safe to render
				sb.append( value );
			}
			else if ( value instanceof Enum<?> enumValue ) {
				sb.append( enumValue.getDeclaringClass().getTypeName() ).append( "." ).append( enumValue.name() );
			}
			else {
				// Even if this is not 100% correct, our goal with this implementation is to provide
				// a cache key or insight into the query structure, not necessarily produce an executable query
				appendSingleQuoteEscapedString( sb, javaType.toString( value ) );
			}
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmLiteral<?> that
			&& Objects.equals( value, that.value );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( value );
	}
}
