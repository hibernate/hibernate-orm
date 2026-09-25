package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;


/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier applied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class SqmModifiedSubQueryExpression<T> extends AbstractSqmExpression<T> {
	public enum Modifier {
		ALL,
		ANY,
		SOME,
	}

	private final SqmSubQuery<T> subQuery;
	private final Modifier modifier;

	public SqmModifiedSubQueryExpression(
			@Nonnull SqmSubQuery<T> subquery,
			@Nonnull Modifier modifier,
			@Nonnull NodeBuilder builder) {
		this (
				subquery,
				modifier,
				subquery.getNodeType(),
				builder
		);
	}

	public SqmModifiedSubQueryExpression(
			@Nonnull SqmSubQuery<T> subQuery,
			@Nonnull Modifier modifier,
			@Nullable SqmBindableType<T> resultType,
			@Nonnull NodeBuilder builder) {
		super( resultType, builder );
		this.subQuery = subQuery;
		this.modifier = modifier;
	}

	@Nonnull
	@Override
	public SqmModifiedSubQueryExpression<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmModifiedSubQueryExpression<T> expression = context.registerCopy(
				this,
				new SqmModifiedSubQueryExpression<>(
						subQuery.copy( context ),
						modifier,
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public Modifier getModifier() {
		return modifier;
	}

	@Nonnull
	public SqmSubQuery<T> getSubQuery() {
		return subQuery;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitModifiedSubQueryExpression( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( modifier );
		hql.append( " (" );
		subQuery.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmModifiedSubQueryExpression<?> that
			&& modifier == that.modifier
			&& subQuery.equals( that.subQuery );
	}

	@Override
	public int hashCode() {
		int result = subQuery.hashCode();
		result = 31 * result + modifier.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmModifiedSubQueryExpression<?> that
			&& modifier == that.modifier
			&& subQuery.isCompatible( that.subQuery );
	}

	@Override
	public int cacheHashCode() {
		int result = subQuery.cacheHashCode();
		result = 31 * result + modifier.hashCode();
		return result;
	}
}
