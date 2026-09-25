package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.spi.NavigablePath;

import java.util.Collection;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SqmBagJoin<O, E> extends AbstractSqmPluralJoin<O,Collection<E>, E> implements JpaCollectionJoin<O, E> {
	public SqmBagJoin(
			@Nonnull SqmFrom<?,O> lhs,
			@Nonnull SqmBagPersistentAttribute<? super O,E> attribute,
			@Nullable String alias,
			@Nonnull SqmJoinType sqmJoinType,
			boolean fetched,
			@Nonnull NodeBuilder nodeBuilder) {
		super( lhs, attribute, alias, sqmJoinType, fetched, nodeBuilder );
	}

	protected SqmBagJoin(
			@Nonnull SqmFrom<?, O> lhs,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmBagPersistentAttribute<O,E> attribute,
			@Nullable String alias,
			@Nonnull SqmJoinType joinType,
			boolean fetched,
			@Nonnull NodeBuilder nodeBuilder) {
		super( lhs, navigablePath, attribute, alias, joinType, fetched, nodeBuilder );
	}

	@Nonnull
	@Override
	public SqmBagJoin<O, E> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmBagJoin<>(
						lhsCopy,
						getNavigablePathCopy( lhsCopy ),
						getAttribute(),
						getExplicitAlias(),
						getSqmJoinType(),
						context.copyFetchedFlag() && isFetched(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmBagPersistentAttribute<O,E> getModel() {
		return (SqmBagPersistentAttribute<O, E>) super.getModel();
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitBagJoin( this );
	}

	@Override
	@Nonnull
	public SqmBagPersistentAttribute<O,E> getAttribute() {
		return getModel();
	}

	@Override
	@Nonnull
	public SqmBagJoin<O, E> on(@Nullable JpaExpression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Nonnull
	@Override
	public SqmBagJoin<O, E> on(@Nonnull BooleanExpression... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmBagJoin<O, E> on(@Nonnull Expression<Boolean> restriction) {
		return (SqmBagJoin<O, E>) super.on( restriction );
	}

	@Override
	@Nonnull
	public SqmBagJoin<O, E> on(@Nullable JpaPredicate... restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	@Nonnull
	@Override
	public SqmBagJoin<O, E> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return (SqmBagJoin<O, E>) super.on( restrictions );
	}

	// todo (6.0) : need to resolve these fetches against the element/index descriptors

	@Override
	@Nonnull
	public SqmCorrelatedBagJoin<O, E> createCorrelation() {
		return new SqmCorrelatedBagJoin<>( this );
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType, null );
	}

	@Nonnull
	@Override
	public <S extends E> SqmBagJoin<O, S> treat(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType );
	}

	@Nonnull
	@Override
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		return treatAs( treatTarget, null );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		return treatAs( treatJavaType, alias, false );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		return treatAs( treatTarget, alias, false );
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedBagJoin<O, E, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		final var treatTarget = nodeBuilder().getDomainModel().managedType( treatJavaType );
		final var treat = (SqmTreatedBagJoin<O, E, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			if ( treatTarget instanceof TreatableDomainType<?> ) {
				return addTreat( new SqmTreatedBagJoin<>( this, (SqmTreatableDomainType<S>) treatTarget, alias, fetch ) );
			}
			else {
				throw new IllegalArgumentException( "Not a treatable type: " + treatJavaType.getName() );
			}
		}
		return treat;
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedBagJoin<O,E,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		final var treat = (SqmTreatedBagJoin<O, E, S>) findTreat( treatTarget, alias );
		if ( treat == null ) {
			return addTreat( new SqmTreatedBagJoin<>( this, (SqmEntityDomainType<S>) treatTarget, alias, fetch ) );
		}
		else {
			return treat;
		}
	}

}
