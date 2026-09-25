package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddableDomainType;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;

import java.util.Objects;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorType;

/**
 * Represents a reference to an embeddable type as a literal.
 *
 * @author Marco Belladelli
 */
public class SqmLiteralEmbeddableType<T>
		extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {
	final SqmEmbeddableDomainType<T> embeddableDomainType;

	public SqmLiteralEmbeddableType(
			@Nonnull SqmEmbeddableDomainType<T> embeddableDomainType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( getDiscriminatorType( embeddableDomainType, nodeBuilder), nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
	}

	@Nonnull
	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	@Nonnull
	@Override
	public SqmLiteralEmbeddableType<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralEmbeddableType<T> expression =
				context.registerCopy( this,
						new SqmLiteralEmbeddableType<>( embeddableDomainType, nodeBuilder() ) );
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(@Nullable SqmBindableType<?> type) {
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableTypeLiteralExpression( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return "TYPE(" + embeddableDomainType + ")";
	}

	@Nonnull
	@Override
	public SemanticPathPart resolvePathPart(
			@Nonnull String name,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Nonnull
	@Override
	public SqmPath<?> resolveIndexedAccess(
			@Nonnull SqmExpression<?> selector,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( embeddableDomainType.getTypeName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmLiteralEmbeddableType<?> that
			&& Objects.equals( embeddableDomainType.getTypeName(), that.embeddableDomainType.getTypeName() );
	}

	@Override
	public int hashCode() {
		return embeddableDomainType.getTypeName().hashCode();
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
