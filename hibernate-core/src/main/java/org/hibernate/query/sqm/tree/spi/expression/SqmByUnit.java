package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Gavin King
 */
public class SqmByUnit extends AbstractSqmExpression<Long> {
	private final SqmDurationUnit<?> unit;
	private final SqmExpression<?> duration;

	public SqmByUnit(
			@Nonnull SqmDurationUnit<?> unit,
			@Nonnull SqmExpression<?> duration,
			@Nonnull SqmBindableType<Long> longType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( longType, nodeBuilder );
		this.unit = unit;
		this.duration = duration;
	}

	@Nonnull
	@Override
	public SqmByUnit copy(@Nonnull SqmCopyContext context) {
		final SqmByUnit existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmByUnit expression = context.registerCopy(
				this,
				new SqmByUnit(
						unit.copy( context ),
						duration.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public SqmDurationUnit<?> getUnit() {
		return unit;
	}

	@Nonnull
	public SqmExpression<?> getDuration() {
		return duration;
	}

	@Override
	public @Nonnull SqmBindableType<Long> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitByUnit( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		duration.appendHqlString( hql, context );
		hql.append( " by " );
		hql.append( unit.getUnit() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmByUnit that
			&& this.unit.equals( that.unit )
			&& this.duration.equals( that.duration );
	}

	@Override
	public int hashCode() {
		int result = unit.hashCode();
		result = 31 * result + duration.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmByUnit that
			&& this.unit.isCompatible( that.unit )
			&& this.duration.isCompatible( that.duration );
	}

	@Override
	public int cacheHashCode() {
		int result = unit.cacheHashCode();
		result = 31 * result + duration.cacheHashCode();
		return result;
	}
}
