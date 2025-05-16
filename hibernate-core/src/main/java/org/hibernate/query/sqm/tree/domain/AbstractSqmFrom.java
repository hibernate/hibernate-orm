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
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
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

/**
 * Convenience base class for SqmFrom implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom<O,T> extends AbstractSqmPath<T> implements SqmFrom<O,T> {
	private String alias;

	private List<SqmJoin<T, ?>> joins;
	private List<SqmTreatedFrom<?,?,?>> treats;

	protected AbstractSqmFrom(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedNavigable,
			SqmFrom<?, ?> lhs,
			String alias,
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
			String alias,
			NodeBuilder nodeBuilder) {
		super(
				SqmCreationHelper.buildRootNavigablePath( entityType.getHibernateEntityName(), alias ),
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
			String alias,
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
		if ( joins != null ) {
			target.joins = new ArrayList<>( joins.size() );
			for ( SqmJoin<T, ?> join : joins ) {
				target.joins.add( join.copy( context ) );
			}
		}
		if ( treats != null ) {
			target.treats = new ArrayList<>( treats.size() );
			for ( SqmTreatedFrom<?,?,?> treat : treats ) {
				target.treats.add( treat.copy( context ) );
			}
		}
	}

	@Override
	public String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.alias = explicitAlias;
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		// Try to resolve an existing attribute join without ON clause
		SqmPath<?> resolvedPath = null;
		for ( SqmJoin<?, ?> sqmJoin : getSqmJoins() ) {
			// We can only match singular joins here, as plural path parts are interpreted like sub-queries
			if ( sqmJoin instanceof SqmSingularJoin<?, ?> attributeJoin
					&& name.equals( sqmJoin.getReferencedPathSource().getPathName() ) ) {
				if ( attributeJoin.getOn() == null ) {
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
	public List<SqmTreatedFrom<?,?,?>> getSqmTreats() {
		return treats == null ? emptyList() : treats;
	}

	protected <S extends T, X extends SqmTreatedFrom<O,T,S>> X findTreat(ManagedDomainType<S> targetType, String alias) {
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

	protected <X extends SqmTreatedFrom<?,?,?>> X addTreat(X treat) {
		if ( treats == null ) {
			treats = new ArrayList<>();
		}
		treats.add( treat );
		return treat;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA


	@Override
	public JpaPath<?> getParentPath() {
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
		final SqmSingularJoin<T, A> join =
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
		final SqmRoot<T> root = (SqmRoot<T>) findRoot();
		final SqmEntityJoin<T, X> sqmEntityJoin = new SqmEntityJoin<>(
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
		final SqmBagJoin<T, E> join = buildBagJoin(
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
		final SqmSetJoin<T, E> join = buildSetJoin(
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
		final SqmListJoin<T, E> join = buildListJoin(
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
		final SqmMapJoin<T, K, V> join = buildMapJoin(
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
		final SqmPathSource<Y> subPathSource = (SqmPathSource<Y>)
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
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof BagPersistentAttribute ) {
			final SqmBagJoin<T, Y> join = buildBagJoin(
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
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof SetPersistentAttribute ) {
			final SqmSetJoin<T, Y> join = buildSetJoin(
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
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof ListPersistentAttribute ) {
			final SqmListJoin<T, Y> join = buildListJoin(
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
		final SqmPathSource<?> joinedPathSource = getReferencedPathSource().getSubPathSource( attributeName );

		if ( joinedPathSource instanceof MapPersistentAttribute<?, ?, ?> ) {
			final SqmMapJoin<T, K, V> join = buildMapJoin(
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
		final SqmEntityJoin<T,Y> join =
				new SqmEntityJoin<>( entity, generateAlias(), joinType, (SqmRoot<T>) findRoot() );
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
		final JpaDerivedJoin<X> join =
				new SqmDerivedJoin<>( (SqmSubQuery<X>) subquery, alias, joinType, lateral, (SqmRoot<X>) findRoot() );
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) join );
		return join;
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
		final SqmJoin<?, X> join =
				new SqmCteJoin<>( ( SqmCteStatement<X> ) cte, alias, joinType, (SqmRoot<X>) findRoot() );
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) join );
		return join;
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
		final SqmFunctionJoin<X> join =
				new SqmFunctionJoin<>(
						(SqmSetReturningFunction<X>) function,
						generateAlias(),
						joinType, lateral,
						(SqmRoot<Object>) findRoot()
				);
		//noinspection unchecked
		addSqmJoin( (SqmJoin<T, ?>) join );
		return join;
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
		final SqmCrossJoin<X> crossJoin =
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
		final SqmAttributeJoin<T, A> compatibleFetchJoin =
				findCompatibleFetchJoin( this, persistentAttribute, SqmJoinType.from( jt ) );
		if ( compatibleFetchJoin != null ) {
			return (SqmSingularJoin<T, A>) compatibleFetchJoin;
		}

		final SqmSingularJoin<T, A> join =
				buildSingularJoin( persistentAttribute, SqmJoinType.from( jt ), true );
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
			final SqmAttributeJoin<T, A> compatibleFetchJoin =
					findCompatibleFetchJoin( this, joinedPathSource, joinType );
			if ( compatibleFetchJoin != null ) {
				return compatibleFetchJoin;
			}
		}

		final SqmAttributeJoin<T, A> sqmJoin =
				buildAttributeJoin( joinedPathSource, joinType, fetched );
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
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType, String alias) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatJavaType, alias );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatTarget, alias );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(Class<S> treatJavaType, String alias, boolean fetch) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatJavaType, alias, fetch );
	}

	@Override
	public <S extends T> SqmTreatedFrom<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch) {
		return (SqmTreatedFrom<O,T,S>) super.treatAs( treatTarget, alias, fetch );
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

	private int aliasCounter = 0;

	private String generateAlias() {
		return alias + "_" + (++aliasCounter);
	}
}
