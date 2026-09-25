package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.from.SqmAttributeJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * @author Steve Ebersole
 */
public class SqmIndexedCollectionAccessPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmExpression<?> selectorExpression;

	public SqmIndexedCollectionAccessPath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmAttributeJoin<?, ?> pluralDomainPath,
			@Nonnull SqmExpression<?> selectorExpression) {
		//noinspection unchecked
		super(
				navigablePath,
				( (SqmPluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource() )
						.getElementPathSource(),
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.selectorExpression = selectorExpression;
	}

	@Override
	public @Nonnull SqmAttributeJoin<?, ?> getLhs() {
		return (SqmAttributeJoin<?, ?>) castNonNull( super.getLhs() );
	}

	@Nonnull
	@Override
	public SqmIndexedCollectionAccessPath<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmAttributeJoin<?, ?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmIndexedCollectionAccessPath<T>(
						getNavigablePathCopy( lhsCopy ),
						lhsCopy,
						selectorExpression.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	public SqmExpression<?> getSelectorExpression() {
		return selectorExpression;
	}

	@Nonnull
	public PluralPersistentAttribute<?, ?, T> getPluralAttribute() {
		//noinspection unchecked
		return (PluralPersistentAttribute<?, ?, T>) getLhs().getReferencedPathSource();
	}

	@Nonnull
	@Override
	public SqmPath<?> resolvePathPart(
			@Nonnull String name,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitIndexedPluralAccessPath( this );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		if ( getReferencedPathSource().getPathType() instanceof EntityDomainType ) {
			return getTreatedPath( treatTarget );
		}

		throw new UnsupportedOperationException(  );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		getLhs().getLhs().appendHqlString( hql, context );
		hql.append( '.' );
		hql.append( getLhs().getReferencedPathSource().getPathName() );
		hql.append( '[' );
		selectorExpression.appendHqlString( hql, context );
		hql.append( ']' );
	}

	// No need for a custom equals/hashCode or isCompatible/cacheHashCode, because the LHS is a SqmJoin
	// which is checked for deep equality/compatibility through SqmFromClause. The NavigablePath equality check is
	// enough to determine "syntactic" equality for expressions and predicates
}
