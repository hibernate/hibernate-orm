package org.hibernate.query.sqm.tree.spi.domain;

import java.time.temporal.Temporal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Expression;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmTemporalExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmTemporalExpressionImplementor;
import org.hibernate.query.sqm.tree.spi.expression.SqmTemporalExpressionWrapper;
import org.hibernate.spi.NavigablePath;

/**
 * A path to an attribute of temporal type which is mapped as a
 * composite value, for example, an {@code OffsetDateTime} with
 * {@link org.hibernate.annotations.TimeZoneStorageType#COLUMN}.
 *
 * @see org.hibernate.query.sqm.tree.spi.expression.SqmExpressionHelper#isCompositeTemporal
 */
public class SqmEmbeddedTemporalValuedSimplePath<T extends Temporal & Comparable<? super T>>
		extends SqmEmbeddedValuedSimplePath<T>
		implements SqmTemporalPath<T>, SqmTemporalExpressionImplementor<T> {

	public SqmEmbeddedTemporalValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	public SqmEmbeddedTemporalValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Nonnull
	@Override
	public SqmEmbeddedTemporalValuedSimplePath<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmEmbeddedTemporalValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> coalesce(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> coalesce(@Nonnull T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().coalesce( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> nullif(@Nonnull Expression<? extends T> y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}

	@Nonnull
	@Override
	public SqmTemporalExpression<T> nullif(@Nonnull T y) {
		return new SqmTemporalExpressionWrapper<>( nodeBuilder().nullif( this, y ) );
	}
}
