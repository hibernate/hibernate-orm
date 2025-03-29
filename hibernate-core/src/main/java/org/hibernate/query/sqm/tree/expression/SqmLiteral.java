/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.internal.util.QuotingHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

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
		assert value != null && ( inherentType == null || inherentType.getExpressibleJavaType().isInstance( value ) );
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
		final SqmLiteral<T> expression = context.registerCopy(
				this,
				new SqmLiteral<>(
						getLiteralValue(),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
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
	public void appendHqlString(StringBuilder hql) {
		appendHqlString( hql, getJavaTypeDescriptor(), getLiteralValue() );
	}

	public static <T> void appendHqlString(StringBuilder sb, JavaType<T> javaType, @Nullable T value) {
		if ( value == null ) {
			sb.append( "null" );
		}
		else {
			final String string = javaType.toString( value );
			if ( javaType.getJavaTypeClass() == String.class ) {
				QuotingHelper.appendSingleQuoteEscapedString( sb, string );
			}
			else {
				sb.append( string );
			}
		}
	}

}
