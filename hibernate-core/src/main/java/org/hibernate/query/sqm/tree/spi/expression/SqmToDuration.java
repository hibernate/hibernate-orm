package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * @author Gavin King
 */
public class SqmToDuration<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> magnitude;
	private final SqmDurationUnit<?> unit;

	public SqmToDuration(
			@Nonnull SqmExpression<?> magnitude,
			@Nonnull SqmDurationUnit<?> unit,
			@Nonnull ReturnableType<T> type,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder.resolveExpressible( type ), nodeBuilder );
		this.magnitude = magnitude;
		this.unit = unit;
	}

	@Nonnull
	@Override
	public SqmToDuration<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmToDuration<T> expression = context.registerCopy(
				this,
				new SqmToDuration<>(
						magnitude.copy( context ),
						unit.copy( context ),
						(ReturnableType<T>) getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public SqmExpression<?> getMagnitude() {
		return magnitude;
	}

	@Nonnull
	public SqmDurationUnit<?> getUnit() {
		return unit;
	}

	@Override
	public @Nonnull SqmBindableType<T> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Nullable
	@Override
	public <R> R accept(@Nonnull SemanticQueryWalker<R> walker) {
		return walker.visitToDuration( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return magnitude.asLoggableText() + " " + unit.getUnit();
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		magnitude.appendHqlString( hql, context );
		hql.append( ' ' );
		hql.append( unit.getUnit() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmToDuration<?> that
			&& magnitude.equals( that.magnitude )
			&& unit.equals( that.unit );
	}

	@Override
	public int hashCode() {
		int result = magnitude.hashCode();
		result = 31 * result + unit.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmToDuration<?> that
			&& magnitude.isCompatible( that.magnitude )
			&& unit.isCompatible( that.unit );
	}

	@Override
	public int cacheHashCode() {
		int result = magnitude.cacheHashCode();
		result = 31 * result + unit.cacheHashCode();
		return result;
	}
}
