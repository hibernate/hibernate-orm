/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * Represents the {@code SIZE()} function.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class SqmCollectionSize extends AbstractSqmExpression<Integer> {
	private final SqmPath<?> pluralPath;

	public SqmCollectionSize(@Nonnull SqmPath<?> pluralPath, @Nonnull NodeBuilder nodeBuilder) {
		this( pluralPath, nodeBuilder.getIntegerType(), nodeBuilder );
	}

	public SqmCollectionSize(@Nonnull SqmPath<?> pluralPath, @Nonnull SqmBindableType<Integer> sizeType, @Nonnull NodeBuilder nodeBuilder) {
		super( sizeType, nodeBuilder );
		this.pluralPath = pluralPath;
	}

	@Nonnull
	@Override
	public SqmCollectionSize copy(@Nonnull SqmCopyContext context) {
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

	@Nonnull
	public SqmPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public @Nonnull SqmBindableType<Integer> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Nullable
	@Override
	public <T> T accept(@Nonnull SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralPath.asLoggableText() + ")";
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
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
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmCollectionSize that
			&& this.pluralPath.isCompatible( that.pluralPath );
	}

	@Override
	public int cacheHashCode() {
		return pluralPath.cacheHashCode();
	}
}
