package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import java.util.Objects;

/**
 * A named expression. Used when the name of the expression matters
 * e.g. in XML generation.
 *
 * @since 7.0
 */
@Incubating(since = "6.2")
public class SqmNamedExpression<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> expression;
	private final String name;

	public SqmNamedExpression(@Nonnull SqmExpression<T> expression, @Nonnull String name) {
		super( expression.getExpressible(), expression.nodeBuilder() );
		this.expression = expression;
		this.name = name;
	}

	@Nonnull
	@Override
	public SqmNamedExpression<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNamedExpression<T> expression = context.registerCopy(
				this,
				new SqmNamedExpression<>( this.expression.copy( context ), name )
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	public SqmExpression<T> getExpression() {
		return expression;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitNamedExpression( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( name );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmNamedExpression<?> that
			&& Objects.equals( this.name, that.name )
			&& this.expression.equals( that.expression );
	}

	@Override
	public int hashCode() {
		int result = expression.hashCode();
		result = 31 * result + name.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmNamedExpression<?> that
			&& Objects.equals( this.name, that.name )
			&& this.expression.isCompatible( that.expression );
	}

	@Override
	public int cacheHashCode() {
		int result = expression.cacheHashCode();
		result = 31 * result + name.hashCode();
		return result;
	}
}
