package org.hibernate.query.sqm.tree.spi.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmAttributeJoin;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedRoot;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom<E,E> implements JpaRoot<E> {

	private final boolean allowJoins;
	private @Nullable List<SqmJoin<?, ?>> orderedJoins;

	public SqmRoot(
			@Nonnull EntityDomainType<E> entityType,
			@Nullable String alias,
			boolean allowJoins,
			@Nonnull NodeBuilder nodeBuilder) {
		super( entityType, alias, nodeBuilder );
		this.allowJoins = allowJoins;
	}

	protected SqmRoot(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<E> referencedNavigable,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, nodeBuilder );
		this.allowJoins = true;
	}

	public SqmRoot(
			@Nonnull NavigablePath navigablePath,
			@Nonnull EntityDomainType<E> entityType,
			@Nullable String alias,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, (SqmEntityDomainType<E>) entityType, alias, nodeBuilder );
		this.allowJoins = true;
	}

	protected SqmRoot(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<E> referencedNavigable,
			@Nullable String alias,
			boolean allowJoins,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, alias, nodeBuilder );
		this.allowJoins = allowJoins;
	}

	@Nonnull
	@Override
	public SqmRoot<E> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmRoot<>(
						getNavigablePath(),
						getReferencedPathSource(),
						getExplicitAlias(),
						allowJoins,
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Internal
	public void copyTo(@Nonnull SqmRoot<E> target, @Nonnull SqmCopyContext context) {
		if ( orderedJoins != null ) {
			// A local join predicate may refer to a join owned by a correlation.
			// Copy its path owner first, as we would for an ordinary attribute join.
			for ( var join : orderedJoins ) {
				if ( join.getLhs() instanceof SqmCorrelation<?, ?> correlation ) {
					correlation.getCorrelatedRoot().copy( context );
				}
			}
		}
		super.copyTo( target, context );
		if ( orderedJoins != null ) {
			target.orderedJoins = new ArrayList<>( orderedJoins.size() );
			for ( var orderedJoin : orderedJoins ) {
				target.orderedJoins.add( orderedJoin.copy( context ) );
			}
		}
	}

	@Nullable
	@Override
	public SqmPath<?> getLhs() {
		// a root has no LHS
		return null;
	}

	public boolean isAllowJoins() {
		return allowJoins;
	}

	public @Nullable List<SqmJoin<?, ?>> getOrderedJoins() {
		return orderedJoins;
	}

	public void addOrderedJoin(@Nonnull SqmJoin<?, ?> join) {
		if ( join instanceof AbstractSqmAttributeJoin<?, ?> attributeJoin && attributeJoin.getDeclaringRoot() != null ) {
			// The attribute belongs to the correlation, but its join is declared in a local FROM item.
			final var declaringRoot = attributeJoin.getDeclaringRoot();
			if ( declaringRoot.orderedJoins == null ) {
				declaringRoot.initializeOrderedJoins();
			}
			castNonNull( declaringRoot.orderedJoins ).add( join );
		}
		else if ( orderedJoins != null ) {
			orderedJoins.add( join );
		}
		else if ( !( join instanceof SqmAttributeJoin<?, ?> ) ) {
			// The join is already in the list visited by initializeOrderedJoins().
			initializeOrderedJoins();
		}
	}

	private void initializeOrderedJoins() {
		orderedJoins = new ArrayList<>();
		getDeclaredJoins().forEach( this::addOrderedJoinTransitive );
	}

	private void addOrderedJoinTransitive(@Nonnull SqmJoin<?, ?> join) {
		// The caller will have already initialized `orderedJoin` when this is called.
		castNonNull( orderedJoins ).add( join );
		join.visitSqmJoins( this::addOrderedJoinTransitive );
	}

	@Override
	public void addSqmJoin(@Nonnull SqmJoin<E, ?> join) {
		if ( !allowJoins ) {
			throw new IllegalArgumentException(
					"The root node [" + this + "] does not allow join/fetch"
			);
		}
		super.addSqmJoin( join );
	}

	@Override
	@Nonnull
	public SqmRoot<?> findRoot() {
		return this;
	}

	@Nonnull
	public String getEntityName() {
		return getModel().getHibernateEntityName();
	}

	@Nonnull
	@Override
	public String toString() {
		final String entityName = getEntityName();
		final String explicitAlias = getExplicitAlias();
		return explicitAlias == null ? entityName : entityName + " as " + explicitAlias;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitRootPath( this );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> other) {
		return super.deepEquals( other )
			&& Objects.equals( getOrderedJoins(), ((SqmRoot<?>) other).getOrderedJoins() );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> other) {
		return super.isDeepCompatible( other )
			&& SqmCacheable.areCompatible( getOrderedJoins(), ((SqmRoot<?>) other).getOrderedJoins() );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmEntityDomainType<E> getModel() {
		return (SqmEntityDomainType<E>) getReferencedPathSource();
	}

	@Nonnull
	@Override
	public EntityDomainType<E> getManagedType() {
		return getModel();
	}

	@Override
	@Nonnull
	public SqmCorrelatedRoot<E> createCorrelation() {
		return new SqmCorrelatedRoot<>( this );
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType, null, false );
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null, false );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ), alias, fetch );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedFrom<E,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		if ( alias != null ) {
			throw new TreatException( "Root path treats can not be aliased - " + getNavigablePath().getFullPath() );
		}
		if ( fetch ) {
			throw new TreatException( "Root path treats can not be fetched - " + getNavigablePath().getFullPath() );
		}
		final var treat = findTreat( treatTarget, null );
		if ( treat == null ) {
			final var treatedRoot = new SqmTreatedRoot<>( this, (SqmEntityDomainType<S>) treatTarget );
			@SuppressWarnings("unchecked")
			final var typedTreat = (SqmTreatedFrom<E, E, S>) treatedRoot;
			return addTreat( typedTreat );
		}
		return treat;
	}

}
