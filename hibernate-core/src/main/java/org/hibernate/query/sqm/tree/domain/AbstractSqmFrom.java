/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.query.sqm.internal.SqmUtil.findCompatibleFetchJoin;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildRootNavigablePath;

/**
 * Convenience base class for SqmFrom implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom<O,T> extends AbstractSqmPath<T> implements SqmFrom<O,T> {
	private @Nullable String alias;

	private @Nullable List<SqmJoin<T, ?>> joins;
	private @Nullable List<SqmTreatedFrom<?,?,@Nullable ?>> treats;

	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			SqmFrom<?, ?> lhs,
			@Nullable String alias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, nodeBuilder );

		if ( lhs == null ) {
			throw new IllegalArgumentException( "LHS cannot be null" );
		}
		this.alias = alias;
	}

	/**
	 * Intended for use with {@link SqmRoot}
	 */
	protected AbstractSqmFrom(
			EntityDomainType<T> entityType,
			@Nullable String alias,
			NodeBuilder nodeBuilder) {
		super(
				buildRootNavigablePath( entityType.getHibernateEntityName(), alias ),
				(SqmEntityDomainType<T>) entityType,
				null,
				nodeBuilder
		);

		this.alias = alias;
	}

	/**
	 * Intended for use with {@link SqmTreatedRoot} to {@link SqmRoot}
	 */
	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			SqmPathSource<T> entityType,
			@Nullable String alias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, entityType, null, nodeBuilder );
		this.alias = alias;
	}

	/**
	 * Intended for use with {@link SqmCorrelatedRootJoin} through {@link SqmRoot}
	 */
	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, null, nodeBuilder );
	}

	protected void copyTo(AbstractSqmFrom<O, T> target, SqmCopyContext context) {
		super.copyTo( target, context );
		final var joins = this.joins;
		if ( joins != null ) {
			final ArrayList<SqmJoin<T, ?>> newJoins =
					new ArrayList<>( joins.size() );
			for ( var join : joins ) {
				newJoins.add( join.copy( context ) );
			}
			target.joins = newJoins;
		}
		final var treats = this.treats;
		if ( treats != null ) {
			final ArrayList<SqmTreatedFrom<?, ?, @Nullable ?>> newTreats =
					new ArrayList<>( treats.size() );
			for ( var treat : treats ) {
				newTreats.add( treat.copy( context ) );
			}
			target.treats = newTreats;
		}
	}

	@Override
	public @Nullable String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(@Nullable String explicitAlias) {
		this.alias = explicitAlias;
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		// Try to resolve an existing attribute join without ON clause
		SqmPath<?> resolvedPath = null;
		for ( var sqmJoin : getSqmJoins() ) {
			// We can only match singular joins here, as plural path parts are interpreted like sub-queries
			if ( sqmJoin instanceof SqmSingularJoin<?, ?> attributeJoin
					&& name.equals( sqmJoin.getReferencedPathSource().getPathName() ) ) {
				if ( attributeJoin.getJoinPredicate() == null ) {
					// todo (6.0): to match the expectation of the JPA spec I think we also have to check
					//  that the join type is INNER or the default join type for the attribute,
					//  but as far as I understand, in 5.x we expect to ignore this behavior
//							if ( attributeJoin.getSqmJoinType() != SqmJoinType.INNER ) {
//								if ( attributeJoin.getAttribute().isCollection() ) {
//									continue;
//								}
//								if ( modelPartContainer == null ) {
//									modelPartContainer = findModelPartContainer( attributeJoin, creationState );
//								}
//								final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) modelPartContainer.findSubPart(
//										name,
//										null
//								);
//								if ( attributeJoin.getSqmJoinType().getCorrespondingSqlJoinType() != joinProducer.getDefaultSqlAstJoinType( null ) ) {
//									continue;
//								}
//							}
					resolvedPath = sqmJoin;
					if ( attributeJoin.isFetched() ) {
						break;
					}
				}
			}
		}
		if ( resolvedPath != null ) {
			return resolvedPath;
		}
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public boolean hasJoins() {
		return joins != null && !joins.isEmpty();
	}

	@Override
	public List<SqmJoin<T, ?>> getSqmJoins() {
		return joins == null ? emptyList() : unmodifiableList( joins );
	}

	@Override
	public int getNumberOfJoins() {
		return joins == null ? 0 : joins.size();
	}

	@Override
	public void addSqmJoin(SqmJoin<T, ?> join) {
		if ( joins == null ) {
			joins = new ArrayList<>();
		}
		joins.add( join );
		findRoot().addOrderedJoin( join );
	}

	@Internal
	public void removeLeftFetchJoins() {
		final List<SqmJoin<T, ?>> joins = this.joins;
		if ( joins != null ) {
			for ( var join : new ArrayList<>( joins ) ) {
				if ( join instanceof SqmAttributeJoin<T, ?> attributeJoin ) {
					if ( attributeJoin.isFetched() ) {
						if ( join.getSqmJoinType() == SqmJoinType.LEFT ) {
							joins.remove( join );
							final var orderedJoins = findRoot().getOrderedJoins();
							if ( orderedJoins != null ) {
								orderedJoins.remove( join );
							}
						}
						else {
							attributeJoin.clearFetched();
						}
					}
				}
			}
		}
	}

	@Override
	public void visitSqmJoins(Consumer<SqmJoin<T, ?>> consumer) {
		if ( joins != null ) {
			joins.forEach( consumer );
		}
	}

	@Override
	public List<SqmTreatedFrom<?,?,@Nullable ?>> getSqmTreats() {
		return treats == null ? emptyList() : treats;
	}

	protected <S extends T, X extends SqmTreatedFrom<O,T,S>> @Nullable X findTreat(ManagedDomainType<S> targetType, @Nullable String alias) {
		if ( treats != null ) {
			for ( var treat : treats ) {
				if ( treat.getModel() == targetType ) {
					if ( Objects.equals( treat.getExplicitAlias(), alias ) ) {
						//noinspection unchecked
						return (X) treat;
					}
				}
			}
		}
		return null;
	}

	protected <S extends T, X extends SqmTreatedFrom<O,T,S>> X addTreat(X treat) {
		if ( treats == null ) {
			treats = new ArrayList<>();
		}
		// Cast needed for Checker Framework
		treats.add( (SqmTreatedFrom<?, ?, @Nullable ?>) treat );
		return treat;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA


	@Override
	public @Nullable JpaPath<?> getParentPath() {
		return getLhs();
	}

	@Override
	public SqmFrom<O,T> getCorrelationParent() {
		throw new IllegalStateException( "Not correlated" );
	}

	public abstract SqmCorrelation<O, T> createCorrelation();

	@Override
	public boolean isCorrelated() {
		return false;
	}

	@Override
	public Set<Join<T, ?>> getJoins() {
		return getSqmJoins().stream()
				.filter( sqmJoin -> sqmJoin instanceof SqmAttributeJoin<?,?> attributeJoin
						&& !attributeJoin.isFetched() )
				.collect( Collectors.toSet() );
	}

	@Override
	public boolean hasImplicitlySelectableJoin() {
		return getSqmJoins().stream()
				.anyMatch( sqmJoin -> sqmJoin instanceof SqmAttributeJoin<?,?> attributeJoin
						&& attributeJoin.isImplicitlySelectable() );
	}

	@Override
	public <A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	public <A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute, JoinType jt) {
		final var join =
				buildSingularJoin( (SqmSingularPersistentAttribute<? super T, A>) attribute,
						SqmJoinType.from( jt ), false );
		addSqmJoin( join );
		return join;
	}

	@Override
	public <X> SqmEntityJoin<T, X> join(Class<X> targetEntityClass, SqmJoinType joinType) {
		return join( nodeBuilder().getJpaMetamodel().entity( targetEntityClass ), joinType );
	}

	@Override
	public <X> SqmEntityJoin<T, X> join(EntityDomainType<X> targetEntityDescriptor) {
		return join( targetEntityDescriptor, SqmJoinType.INNER );
	}

	@Override
	public <X> SqmEntityJoin<T, X> join(EntityDomainType<X> targetEntityDescriptor, SqmJoinType joinType) {
		//noinspection unchecked
		final var root = (SqmRoot<T>) findRoot();
		final var sqmEntityJoin = new SqmEntityJoin<>(
				targetEntityDescriptor,
				generateAlias(),
				joinType,
				root
		);
		root.addSqmJoin( sqmEntityJoin );
		return sqmEntityJoin;
	}

	@Override
	public <A> SqmBagJoin<T, A> join(CollectionAttribute<? super T, A> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	public <E> SqmBagJoin<T, E> join(CollectionAttribute<? super T, E> attribute, JoinType jt) {
		final var join = buildBagJoin(
				(BagPersistentAttribute<? super T, E>) attribute,
				SqmJoinType.from( jt ),
				false
		);
		addSqmJoin( join );
		return join;
	}

	@Override
	public <E> SqmSetJoin<T, E> join(SetAttribute<? super T, E> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	public <E> SqmSetJoin<T, E> join(SetAttribute<? super T, E> attribute, JoinType jt) {
		final var join = buildSetJoin(
				(SetPersistentAttribute<? super T, E>) attribute,
				SqmJoinType.from( jt ),
				false
		);
		addSqmJoin( join );
		return join;
	}

	@Override
	public <E> SqmListJoin<T, E> join(ListAttribute<? super T, E> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	public <E> SqmListJoin<T, E> join(ListAttribute<? super T, E> attribute, JoinType jt) {
		final var join = buildListJoin(
				(ListPersistentAttribute<? super T, E>) attribute,
				SqmJoinType.from( jt ),
				false
		);
		addSqmJoin( join );
		return join;
	}

	@Override
	public <K, V> SqmMapJoin<T, K, V> join(MapAttribute<? super T, K, V> attribute) {
		return join( attribute, JoinType.INNER );
	}

	@Override
	public <K, V> SqmMapJoin<T, K, V> join(MapAttribute<? super T, K, V> attribute, JoinType jt) {
		final var join = buildMapJoin(
				(MapPersistentAttribute<? super T, K, V>) attribute,
				SqmJoinType.from( jt ),
				false
		);
		addSqmJoin( join );
		return join;
	}

	@Override
	public <X, Y> SqmAttributeJoin<X, Y> join(String attributeName) {
		return join( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt) {
		final var subPathSource = (SqmPathSource<Y>)
				getReferencedPathSource().getSubPathSource( attributeName );
		return (SqmAttributeJoin<X, Y>) buildJoin( subPathSource, SqmJoinType.from( jt ), false );
	}

	@Override
	public <X, Y> SqmBagJoin<X, Y> joinCollection(String attributeName) {
		return joinCollection( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, Y> SqmBagJoin<X, Y> joinCollection(String attributeName, JoinType jt) {
		final var joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );
		if ( joinedPathSource instanceof BagPersistentAttribute ) {
			final var join = buildBagJoin(
					(BagPersistentAttribute<T, Y>) joinedPathSource,
					SqmJoinType.from( jt ),
					false
			);
			addSqmJoin( join );
			return (SqmBagJoin<X, Y>) join;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (bag) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath()
				)
		);
	}

	@Override
	public <X, Y> SqmSetJoin<X, Y> joinSet(String attributeName) {
		return joinSet( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, Y> SqmSetJoin<X, Y> joinSet(String attributeName, JoinType jt) {
		final var joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );
		if ( joinedPathSource instanceof SetPersistentAttribute ) {
			final var join = buildSetJoin(
					(SetPersistentAttribute<T, Y>) joinedPathSource,
					SqmJoinType.from( jt ),
					false
			);
			addSqmJoin( join );
			return (SqmSetJoin<X, Y>) join;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (set) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath()
				)
		);
	}

	@Override
	public <X, Y> SqmListJoin<X, Y> joinList(String attributeName) {
		return joinList( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, Y> SqmListJoin<X, Y> joinList(String attributeName, JoinType jt) {
		final var joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof ListPersistentAttribute ) {
			final var join = buildListJoin(
					(ListPersistentAttribute<T, Y>) joinedPathSource,
					SqmJoinType.from( jt ),
					false
			);
			addSqmJoin( join );
			return (SqmListJoin<X, Y>) join;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (list) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath()
				)
		);
	}

	@Override
	public <X, K, V> SqmMapJoin<X, K, V> joinMap(String attributeName) {
		return joinMap( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, K, V> SqmMapJoin<X, K, V> joinMap(String attributeName, JoinType jt) {
		final var joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof MapPersistentAttribute<?, ?, ?> ) {
			final var join = buildMapJoin(
					(MapPersistentAttribute<T, K, V>) joinedPathSource,
					SqmJoinType.from( jt ),
					false
			);
			addSqmJoin( join );
			return (SqmMapJoin<X, K, V>) join;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Passed attribute name [%s] did not correspond to a collection (map) reference [%s] relative to %s",
						attributeName,
						joinedPathSource,
						getNavigablePath()
				)
		);
	}

	@Override
	public <R> SqmEntityJoin<T, R> join(Class<R> entityJavaType) {
		return join( nodeBuilder().getDomainModel().entity( entityJavaType ) );
	}

	@Override
	public <R> SqmEntityJoin<T, R> join(EntityType<R> entityType) {
		return join( entityType, JoinType.INNER );
	}

	@Override
	public <Y> SqmEntityJoin<T, Y> join(Class<Y> entityJavaType, JoinType joinType) {
		return join( nodeBuilder().getDomainModel().entity( entityJavaType ), joinType );
	}

	@Override
	public <Y> SqmEntityJoin<T, Y> join(EntityType<Y> entity, JoinType joinType) {
		//noinspection unchecked
		final var root = (SqmRoot<T>) findRoot();
		final var join = new SqmEntityJoin<>( entity, generateAlias(), joinType, root );
		addSqmJoin( join );
		return join;
	}

	@Override
	public <X> JpaDerivedJoin<X> join(Subquery<X> subquery) {
		return join( subquery, SqmJoinType.INNER, false, generateAlias() );
	}

	@Override
	public <X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType) {
		return join( subquery, joinType, false, generateAlias() );
	}

	@Override
	public <X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery) {
		return join( subquery, SqmJoinType.INNER, true, generateAlias() );
	}

	@Override
	public <X> JpaDerivedJoin<X> joinLateral(Subquery<X> subquery, SqmJoinType joinType) {
		return join( subquery, joinType, true, generateAlias() );
	}

	@Override
	public <X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType, boolean lateral) {
		return join( subquery, joinType, lateral, generateAlias() );
	}

	public <X> JpaDerivedJoin<X> join(Subquery<X> subquery, SqmJoinType joinType, boolean lateral, String alias) {
		validateComplianceFromSubQuery();
		//noinspection unchecked
		final var derivedJoin =
				new SqmDerivedJoin<>( (SqmSubQuery<X>) subquery, alias, joinType, lateral, (SqmRoot<X>) findRoot() );
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) derivedJoin );
		return derivedJoin;
	}

	@Override
	public <X> SqmJoin<?, X> join(JpaCteCriteria<X> cte) {
		return join( cte, SqmJoinType.INNER, generateAlias() );
	}

	@Override
	public <X> SqmJoin<?, X> join(JpaCteCriteria<X> cte, SqmJoinType joinType) {
		return join( cte, joinType, generateAlias() );
	}

	public <X> SqmJoin<?, X> join(JpaCteCriteria<X> cte, SqmJoinType joinType, String alias) {
		validateComplianceFromSubQuery();
		//noinspection unchecked
		final var cteJoin =
				new SqmCteJoin<>( ( SqmCteStatement<X> ) cte, alias, joinType, (SqmRoot<X>) findRoot() );
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) cteJoin );
		return cteJoin;
	}

	@Override
	public <X> JpaFunctionJoin<X> joinLateral(JpaSetReturningFunction<X> function, SqmJoinType joinType) {
		return join( function, joinType, true );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinLateral(JpaSetReturningFunction<X> function) {
		return join( function, SqmJoinType.INNER, true );
	}

	@Override
	public <X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function, SqmJoinType joinType) {
		return join( function, joinType, false );
	}

	@Override
	public <X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function) {
		return join( function, SqmJoinType.INNER, false );
	}

	@Override
	public <X> JpaFunctionJoin<X> join(JpaSetReturningFunction<X> function, SqmJoinType joinType, boolean lateral) {
		validateComplianceFromFunction();
		//noinspection unchecked
		final var functionJoin =
				new SqmFunctionJoin<>(
						(SqmSetReturningFunction<X>) function,
						generateAlias(),
						joinType, lateral,
						(SqmRoot<Object>) findRoot()
				);
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) functionJoin );
		return functionJoin;
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArray(String arrayAttributeName) {
		return joinArray( arrayAttributeName, SqmJoinType.INNER );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArray(String arrayAttributeName, SqmJoinType joinType) {
		return join( nodeBuilder().unnestArray( get( arrayAttributeName ) ), joinType, true );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArray(SingularAttribute<? super T, X[]> arrayAttribute) {
		return joinArray( arrayAttribute, SqmJoinType.INNER );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArray(SingularAttribute<? super T, X[]> arrayAttribute, SqmJoinType joinType) {
		return join( nodeBuilder().unnestArray( get( arrayAttribute ) ), joinType, true );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArrayCollection(String collectionAttributeName) {
		return joinArrayCollection( collectionAttributeName, SqmJoinType.INNER );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArrayCollection(String collectionAttributeName, SqmJoinType joinType) {
		return join( nodeBuilder().unnestCollection( get( collectionAttributeName ) ), joinType, true );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArrayCollection(SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute) {
		return joinArrayCollection( collectionAttribute, SqmJoinType.INNER );
	}

	@Override
	public <X> JpaFunctionJoin<X> joinArrayCollection(
			SingularAttribute<? super T, ? extends Collection<X>> collectionAttribute,
			SqmJoinType joinType) {
		return join( nodeBuilder().unnestCollection( get( collectionAttribute ) ), joinType, true );
	}

	private void validateComplianceFromSubQuery() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries in the from clause. "
					+ "Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	private void validateComplianceFromFunction() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support functions in the from clause. "
					+ "Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	@Override
	public <X> JpaCrossJoin<X> crossJoin(Class<X> entityJavaType) {
		return crossJoin( nodeBuilder().getDomainModel().entity( entityJavaType ) );
	}

	@Override
	public <X> JpaCrossJoin<X> crossJoin(EntityDomainType<X> entity) {
		final var crossJoin =
				new SqmCrossJoin<>( (SqmEntityDomainType<X>) entity, generateAlias(), findRoot() );
		// noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) crossJoin );
		return crossJoin;
	}

	@Override
	public Set<Fetch<T, ?>> getFetches() {
		//noinspection unchecked
		return (Set<Fetch<T, ?>>) (Set<?>) getSqmJoins().stream()
				.filter( sqmJoin -> sqmJoin instanceof SqmAttributeJoin<?,?> attributeJoin
						&& attributeJoin.isFetched() )
				.collect( Collectors.toSet() );
	}

	@Override
	public <A> SqmSingularJoin<T,A> fetch(SingularAttribute<? super T, A> attribute) {
		return fetch( attribute, JoinType.INNER );
	}

	@Override
	public <A> SqmSingularJoin<T, A> fetch(SingularAttribute<? super T, A> attribute, JoinType jt) {
		final var persistentAttribute = (SqmSingularPersistentAttribute<? super T, A>) attribute;
		final var compatibleFetchJoin =
				findCompatibleFetchJoin( this, persistentAttribute, SqmJoinType.from( jt ) );
		if ( compatibleFetchJoin != null ) {
			return (SqmSingularJoin<T, A>) compatibleFetchJoin;
		}

		final var join = buildSingularJoin( persistentAttribute, SqmJoinType.from( jt ), true );
		addSqmJoin( join );
		return join;
	}

	@Override
	public <A> SqmAttributeJoin<T, A> fetch(PluralAttribute<? super T, ?, A> attribute) {
		return fetch( attribute, JoinType.INNER );
	}

	@Override
	public <A> SqmAttributeJoin<T, A> fetch(PluralAttribute<? super T, ?, A> attribute, JoinType jt) {
		return buildJoin(
				(SqmPluralPersistentAttribute<? super T, ?, A>) attribute,
				SqmJoinType.from( jt ),
				true
		);
	}

	@Override
	public <X,A> SqmAttributeJoin<X,A> fetch(String attributeName) {
		return fetch( attributeName, JoinType.INNER );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X, A> SqmAttributeJoin<X, A> fetch(String attributeName, JoinType jt) {
		return (SqmAttributeJoin<X, A>) buildJoin(
				(SqmPathSource<A>)
						getReferencedPathSource()
								.getSubPathSource( attributeName ),
				SqmJoinType.from( jt ),
				true
		);
	}

	private <A> SqmAttributeJoin<T, A> buildJoin(
			SqmPathSource<A> joinedPathSource,
			SqmJoinType joinType,
			boolean fetched) {
		if ( fetched ) {
			final var compatibleFetchJoin =
					findCompatibleFetchJoin( this, joinedPathSource, joinType );
			if ( compatibleFetchJoin != null ) {
				return compatibleFetchJoin;
			}
		}

		final var sqmJoin = buildAttributeJoin( joinedPathSource, joinType, fetched );
		addSqmJoin( sqmJoin );
		return sqmJoin;
	}

	private <A> SqmAttributeJoin<T, A> buildAttributeJoin(SqmPathSource<A> joinedPathSource, SqmJoinType joinType, boolean fetched) {
		if ( joinedPathSource instanceof SqmSingularPersistentAttribute<?, A> ) {
			return buildSingularJoin( (SqmSingularPersistentAttribute<T, A>) joinedPathSource, joinType, fetched );
		}
		else if ( joinedPathSource instanceof SqmBagPersistentAttribute<?, A> ) {
			return buildBagJoin( (SqmBagPersistentAttribute<T, A>) joinedPathSource, joinType, fetched );
		}
		else if ( joinedPathSource instanceof SqmListPersistentAttribute<?, A> ) {
			return buildListJoin( (SqmListPersistentAttribute<T, A>) joinedPathSource, joinType, fetched );
		}
		else if ( joinedPathSource instanceof SqmMapPersistentAttribute<?, ?, A> ) {
			return buildMapJoin( (SqmMapPersistentAttribute<T, ?, A>) joinedPathSource, joinType, fetched );
		}
		else if ( joinedPathSource instanceof SqmSetPersistentAttribute<?, A> ) {
			return buildSetJoin( (SqmSetPersistentAttribute<T, A>) joinedPathSource, joinType, fetched );
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Passed attribute [%s] did not correspond to a joinable reference [%s] relative to %s",
							joinedPathSource.getPathName(),
							joinedPathSource,
							getNavigablePath()
					)
			);
		}
	}

	private <A> SqmSingularJoin<T, A> buildSingularJoin(
			SqmSingularPersistentAttribute<? super T, A> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		if ( attribute.getPathType() instanceof ManagedDomainType ) {
			return new SqmSingularJoin<>(
					this,
					attribute,
					generateAlias(),
					joinType,
					fetched,
					nodeBuilder()
			);
		}

		throw new SemanticException( "Attribute '" + attribute + "' is not joinable" );
	}

	private <E> SqmBagJoin<T, E> buildBagJoin(
			BagPersistentAttribute<? super T, E> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmBagJoin<>(
				this,
				(SqmBagPersistentAttribute<? super T, E>) attribute,
				generateAlias(),
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	private <E> SqmListJoin<T, E> buildListJoin(
			ListPersistentAttribute<? super T, E> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmListJoin<>(
				this,
				(SqmListPersistentAttribute<? super T, E>) attribute,
				generateAlias(),
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	private <K, V> SqmMapJoin<T, K, V> buildMapJoin(
			MapPersistentAttribute<? super T, K, V> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmMapJoin<>(
				this,
				(SqmMapPersistentAttribute<? super T, K, V>) attribute,
				generateAlias(),
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	private <E> SqmSetJoin<T, E> buildSetJoin(
			SetPersistentAttribute<? super T, E> attribute,
			SqmJoinType joinType,
			boolean fetched) {
		return new SqmSetJoin<>(
				this,
				(SqmSetPersistentAttribute<? super T, E>) attribute,
				generateAlias(),
				joinType,
				fetched,
				nodeBuilder()
		);
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatJavaType );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(EntityDomainType<S> treatTarget) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatTarget );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatJavaType, alias );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatTarget, alias );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( resolveAlias( context ) );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		if ( getExplicitAlias() == null ) {
			setExplicitAlias( name );
		}
		return super.alias( name );
	}

	@Override
	public JpaExpression<?> id() {
		return nodeBuilder().id( this );
	}

	private int aliasCounter = 0;

	private String generateAlias() {
		final String prefix;
		if ( alias == null ) {
			prefix = "var_";
		}
		else if ( alias.startsWith( "var_" ) ) {
			prefix = alias;
		}
		else {
			prefix = "var_" + alias;
		}
		return prefix + "_" + (++aliasCounter);
	}

	// No need for equals/hashCode or isCompatible/cacheHashCode, because the base implementation using NavigablePath
	// is fine for the purpose of matching nodes "syntactically".
	// Since the navigablePath contains the alias, no need to check this separately here

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return equals( object )
			&& SqmFrom.areDeepEqual( getSqmJoins(), object.getSqmJoins() )
			&& SqmFrom.areDeepEqual( getSqmTreats(), object.getSqmTreats() );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return isCompatible( object )
				&& SqmFrom.areDeepCompatible( getSqmJoins(), object.getSqmJoins() )
				&& SqmFrom.areDeepCompatible( getSqmTreats(), object.getSqmTreats() );
	}
}
