package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.select.SqmAliasedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;

import java.util.Objects;

/**
 * Models a reference to a {@link SqmAliasedNode}
 * used in the order-by or group-by clause by either position or alias,
 * though the reference is normalized here to a positional ref
 */
public class SqmAliasedNodeRef extends AbstractSqmExpression<Integer> {

	private final int position;
	// The navigable path is optionally set in case this is a reference to an attribute of a selection
	private final @Nullable NavigablePath navigablePath;

	public SqmAliasedNodeRef(int position, @Nonnull SqmBindableType<Integer> intType, @Nonnull NodeBuilder criteriaBuilder) {
		super( intType, criteriaBuilder );
		this.position = position;
		this.navigablePath = null;
	}

	public SqmAliasedNodeRef(
			int position,
			@Nullable NavigablePath navigablePath,
			@Nonnull SqmBindableType<Integer> type,
			@Nonnull NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.position = position;
		this.navigablePath = navigablePath;
	}

	private SqmAliasedNodeRef(@Nonnull SqmAliasedNodeRef original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.position = original.position;
		this.navigablePath = original.navigablePath;
	}

	@Nonnull
	@Override
	public SqmAliasedNodeRef copy(@Nonnull SqmCopyContext context) {
		final SqmAliasedNodeRef existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAliasedNodeRef expression = context.registerCopy( this, new SqmAliasedNodeRef( this ) );
		copyTo( expression, context );
		return expression;
	}

	public int getPosition() {
		return position;
	}

	public @Nullable NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		// we expect this to be handled specially in
		// `BaseSqmToSqlAstConverter#resolveGroupOrOrderByExpression`
		throw new UnsupportedOperationException();
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		if ( navigablePath == null ) {
			hql.append( position );
		}
		else {
			hql.append( navigablePath.getLocalName() );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmAliasedNodeRef that
			&& position == that.position
			&& Objects.equals( navigablePath == null ? null : navigablePath.getLocalName(),
				that.navigablePath == null ? null : that.navigablePath.getLocalName() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( position, navigablePath == null ? null : navigablePath.getLocalName() );
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
