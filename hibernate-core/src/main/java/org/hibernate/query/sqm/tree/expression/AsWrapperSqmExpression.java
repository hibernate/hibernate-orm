/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import jakarta.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.type.BasicType;

import java.util.Objects;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


public class AsWrapperSqmExpression<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> expression;

	AsWrapperSqmExpression(@Nullable SqmBindableType<T> type, @Nonnull Class<T> clazz, @Nonnull SqmExpression<?> expression) {
		super( null, expression.nodeBuilder() );
		this.expression = expression;
		setExpressibleType(
				type,
				type == null
						? expression.nodeBuilder().getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( clazz )
						: null
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAsWrapperExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "wrap(" );
		expression.appendHqlString( hql, context );
		hql.append( " as " );
		final BasicType<T> nodeType = getNodeType();
		if ( nodeType == null ) {
			hql.append( castNonNull( getJavaType() ).getCanonicalName() );
		}
		else {
			hql.append( nodeType.getReturnedClassName() );
		}
		hql.append( ")" );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return expression.as( type );
	}

	@Override
	public SqmExpression<T> copy(SqmCopyContext context) {
		return new AsWrapperSqmExpression<>( getNodeType(), (Class<T>) castNonNull( getJavaType() ), expression.copy( context ) );
	}

	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public @Nullable BasicType<T> getNodeType() {
		return (BasicType<T>) super.getNodeType();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AsWrapperSqmExpression<?> that
			&& this.expression.equals( that.expression )
			&& Objects.equals( this.getNodeType(), that.getNodeType() )
			&& Objects.equals( this.getJavaTypeDescriptor(), that.getJavaTypeDescriptor() );
	}

	@Override
	public int hashCode() {
		return expression.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof AsWrapperSqmExpression<?> that
			&& this.expression.isCompatible( that.expression )
			&& Objects.equals( this.getNodeType(), that.getNodeType() )
			&& Objects.equals( this.getJavaTypeDescriptor(), that.getJavaTypeDescriptor() );
	}

	@Override
	public int cacheHashCode() {
		return expression.cacheHashCode();
	}
}
