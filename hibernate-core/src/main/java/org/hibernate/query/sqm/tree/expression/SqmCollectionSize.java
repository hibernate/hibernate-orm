/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * Represents the {@code SIZE()} function.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class SqmCollectionSize extends AbstractSqmExpression<Integer> {
	private final SqmPath<?> pluralPath;

	public SqmCollectionSize(SqmPath<?> pluralPath, NodeBuilder nodeBuilder) {
		this( pluralPath, nodeBuilder.getIntegerType(), nodeBuilder );
	}

	public SqmCollectionSize(SqmPath<?> pluralPath, SqmBindableType<Integer> sizeType, NodeBuilder nodeBuilder) {
		super( sizeType, nodeBuilder );
		this.pluralPath = pluralPath;
	}

	@Override
	public SqmCollectionSize copy(SqmCopyContext context) {
		final SqmCollectionSize existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCollectionSize expression = context.registerCopy(
				this,
				new SqmCollectionSize(
						pluralPath.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public @NonNull SqmBindableType<Integer> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralPath.asLoggableText() + ")";
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "size(" );
		pluralPath.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmCollectionSize that
			&& this.pluralPath.equals( that.pluralPath );
	}

	@Override
	public int hashCode() {
		return pluralPath.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmCollectionSize that
			&& this.pluralPath.isCompatible( that.pluralPath );
	}

	@Override
	public int cacheHashCode() {
		return pluralPath.cacheHashCode();
	}
}
