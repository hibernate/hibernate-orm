package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;

import java.util.Objects;


/**
 * @author Gavin King
 */
public class SqmCastTarget<T> extends AbstractSqmNode implements SqmTypedNode<T>, JpaCastTarget<T> {
	private final ReturnableType<T> type;
	private final @Nullable Long length;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	public SqmCastTarget(
			@Nonnull ReturnableType<T> type,
			@Nonnull NodeBuilder nodeBuilder) {
		this( type, null, nodeBuilder );
	}

	public SqmCastTarget(
			@Nonnull ReturnableType<T> type,
			@Nullable Long length,
			@Nonnull NodeBuilder nodeBuilder) {
		this( type, length, null, null, nodeBuilder );
	}

	public SqmCastTarget(
			@Nonnull ReturnableType<T> type,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nonnull NodeBuilder nodeBuilder) {
		this( type, null, precision, scale, nodeBuilder );
	}

	public SqmCastTarget(
			@Nonnull ReturnableType<T> type,
			@Nullable Long length,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.type = type;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Nonnull
	@Override
	public SqmCastTarget<T> copy(@Nonnull SqmCopyContext context) {
		return this;
	}

	@Nonnull
	public ReturnableType<T> getType() {
		return type;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitCastTarget(this);
	}

	@Override
	public @Nullable SqmBindableType<T> getNodeType() {
		return nodeBuilder().resolveExpressible( type );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( type.getTypeName() );
		if ( precision != null ) {
			hql.append( '(' );
			hql.append( precision );
			if ( scale != null ) {
				hql.append( ", " );
				hql.append( scale );
			}
			hql.append( ')' );
		}
		else if ( length != null ) {
			hql.append( '(' );
			hql.append( length );
			hql.append( ')' );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmCastTarget<?> that
			&& Objects.equals( type, that.type )
			&& Objects.equals( length, that.length )
			&& Objects.equals( precision, that.precision )
			&& Objects.equals( scale, that.scale );
	}

	@Override
	public int hashCode() {
		return Objects.hash( type, length, precision, scale );
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
