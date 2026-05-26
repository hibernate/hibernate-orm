/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelectCriteria;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static java.lang.Character.isAlphabetic;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.acquireUniqueAlias;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSelectQuery<T>
		extends AbstractSqmNode
		implements SqmSelectQuery<T> {
	private final Map<String, SqmCteStatement<?>> cteStatements;
	private SqmQueryPart<T> sqmQueryPart;
	private final Class<T> resultType;

	public AbstractSqmSelectQuery(Class<T> resultType, NodeBuilder builder) {
		this( new SqmQuerySpec<>( builder ), resultType, builder );
	}

	public AbstractSqmSelectQuery(SqmQueryPart<T> queryPart, Class<T> resultType, NodeBuilder builder) {
		super( builder );
		this.cteStatements = new LinkedHashMap<>();
		this.resultType = resultType;
		this.sqmQueryPart = queryPart;
	}

	protected AbstractSqmSelectQuery(
			NodeBuilder builder,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType) {
		super( builder );
		this.cteStatements = cteStatements;
		this.resultType = resultType;
	}

	public AbstractSqmSelectQuery(
			SqmQueryPart<T> queryPart,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType,
			NodeBuilder builder) {
		super( builder );
		this.cteStatements = cteStatements;
		this.resultType = resultType;
		this.sqmQueryPart = queryPart;
	}

	protected Map<String, SqmCteStatement<?>> copyCteStatements(SqmCopyContext context) {
		final Map<String, SqmCteStatement<?>> copies = new LinkedHashMap<>( cteStatements.size() );
		for ( var entry : cteStatements.entrySet() ) {
			copies.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return copies;
	}

	@Override
	@Nonnull
	public Collection<SqmCteStatement<?>> getCteStatements() {
		return cteStatements.values();
	}

	Map<String, SqmCteStatement<?>> getCteStatementMap() {
		return new LinkedHashMap<>( cteStatements );
	}

	void addCteStatements(Map<String, SqmCteStatement<?>> cteStatements) {
		this.cteStatements.putAll( cteStatements );
	}

	@Override
	public @Nullable SqmCteStatement<?> getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	@Nonnull
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return cteStatements.values();
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public <X> JpaCteCriteria<X> getCteCriteria(@Nonnull String cteName) {
		return (JpaCteCriteria<X>) cteStatements.get( cteName );
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public <X> JpaCteCriteria<X> with(@Nonnull AbstractQuery<X> criteria) {
		// Use of acquireUniqueAlias() results in interpretation cache miss
		return withInternal( "_" + acquireUniqueAlias(), criteria );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, true, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> with(@Nonnull String name, @Nonnull AbstractQuery<X> criteria) {
		return withInternal( validateCteName( name ), criteria );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			@Nonnull String name,
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Nonnull
	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			@Nonnull String name,
			@Nonnull AbstractQuery<X> baseCriteria,
			@Nonnull Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, true, recursiveCriteriaProducer );
	}

	private String validateCteName(String name) {
		if ( name == null || name.isBlank() ) {
			throw new IllegalArgumentException( "Illegal empty CTE name" );
		}
		if ( !isAlphabetic( name.charAt( 0 ) ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Illegal CTE name [%s]. Names must start with an alphabetic character!",
							name
					)
			);
		}
		return name;
	}

	protected <X> JpaCteCriteria<X> withInternal(String name, AbstractQuery<X> criteria) {
		final var cteStatement = new SqmCteStatement<>(
				name,
				(SqmSelectQuery<X>) criteria,
				this,
				nodeBuilder()
		);
		if ( cteStatements.putIfAbsent( name, cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
		}
		return cteStatement;
	}

	protected <X> JpaCteCriteria<X> withInternal(
			String name,
			AbstractQuery<X> baseCriteria,
			boolean unionDistinct,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		final var cteStatement = new SqmCteStatement<>(
				name,
				(SqmSelectQuery<X>) baseCriteria,
				unionDistinct,
				recursiveCriteriaProducer,
				this,
				nodeBuilder()
		);
		if ( cteStatements.putIfAbsent( name, cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
		}
		return cteStatement;
	}

	@Nonnull
	@Override
	public Class<T> getResultType() {
		return resultType;
	}

	@Nonnull
	@Override
	public SqmQuerySpec<T> getQuerySpec() {
		return sqmQueryPart.getFirstQuerySpec();
	}

	@Nonnull
	@Override
	public SqmQueryPart<T> getQueryPart() {
		return sqmQueryPart;
	}

	public void setQueryPart(SqmQueryPart<T> sqmQueryPart) {
		this.sqmQueryPart = sqmQueryPart;
	}

	@Nonnull
	@Override
	public Set<Root<?>> getRoots() {
		return unmodifiableSet( getQuerySpec().getRoots() );
	}

	/**
	 * @see org.hibernate.query.criteria.JpaCriteriaQuery#getRootList()
	 */
	public List<? extends JpaRoot<?>> getRootList() {
		return getQuerySpec().getRootList();
	}

	/**
	 * @see org.hibernate.query.criteria.JpaCriteriaQuery#getRoot(int, Class)
	 */
	public <E> JpaRoot<? extends E> getRoot(int position, Class<E> type) {
		final var rootList = getQuerySpec().getRootList();
		if ( rootList.size() <= position ) {
			throw new IllegalArgumentException( "Not enough root entities" );
		}
		return castRoot( rootList.get( position ), type );
	}

	/**
	 * @see org.hibernate.query.criteria.JpaCriteriaQuery#getRoot(String, Class)
	 */
	public <E> JpaRoot<? extends E> getRoot(String alias, Class<E> type) {
		for ( var root : getQuerySpec().getRootList() ) {
			final String rootAlias = root.getAlias();
			if ( rootAlias != null && rootAlias.equals( alias ) ) {
				return castRoot( root, type );
			}
		}
		throw new IllegalArgumentException( "No root entity with alias " + alias );
	}

	private static <E> JpaRoot<? extends E> castRoot(JpaRoot<?> root, Class<E> type) {
		final var rootEntityType = root.getJavaType();
		if ( rootEntityType == null ) {
			throw new AssertionFailure( "Java type of root entity was null" );
		}
		if ( !type.isAssignableFrom( rootEntityType ) ) {
			throw new IllegalArgumentException( "Root entity of type '" + rootEntityType.getTypeName()
												+ "' did not have the given type '" + type.getTypeName() + "'");
		}
		@SuppressWarnings("unchecked") // safe, we just checked
		final var result = (JpaRoot<? extends E>) root;
		return result;
	}

	@Nonnull
	@Override
	public <X> SqmRoot<X> from(@Nonnull Class<X> entityClass) {
		return addRoot(
				new SqmRoot<>(
						nodeBuilder().getDomainModel().entity( entityClass ),
						generateAlias(),
						true,
						nodeBuilder()
				)
		);
	}

	@Nonnull
	@Override
	public <X> SqmDerivedRoot<X> from(@Nonnull Subquery<X> subquery) {
		validateComplianceFromSubQuery();
		final var root = new SqmDerivedRoot<>( (SqmSubQuery<X>) subquery, null );
		addRoot( root );
		return root;
	}

	@Nonnull
	public <X> JpaRoot<X> from(@Nonnull JpaCteCriteria<X> cte) {
		final var root = new SqmCteRoot<>( ( SqmCteStatement<X> ) cte, null );
		addRoot( root );
		return root;
	}

	@Nonnull
	@Override
	public <X> JpaFunctionRoot<X> from(@Nonnull JpaSetReturningFunction<X> function) {
		final var root = new SqmFunctionRoot<>( (SqmSetReturningFunction<X>) function, null );
		addRoot( root );
		return root;
	}

	private <X> SqmRoot<X> addRoot(SqmRoot<X> root) {
		getQuerySpec().addRoot( root );
		return root;
	}

	@Nonnull
	@Override
	public <X> SqmRoot<X> from(@Nonnull EntityType<X> entityType) {
		return addRoot(
				new SqmRoot<>(
						(EntityDomainType<X>) entityType,
						generateAlias(),
						true,
						nodeBuilder()
				)
		);
	}

	private void validateComplianceFromSubQuery() {
		if ( nodeBuilder().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries in the from clause. "
					+ "Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selection

	@Override
	public boolean isDistinct() {
		return getQuerySpec().isDistinct();
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> distinct(boolean distinct) {
		getQuerySpec().setDistinct( distinct );
		return this;
	}

	@Nullable
	@Override
	public JpaSelection<T> getSelection() {
		final var selectClause = getQuerySpec().getSelectClause();
		final var selections = selectClause.getSelections();
		return (JpaSelection<T>) switch ( selections.size() ) {
			case 0 -> null;
			case 1 -> selections.get( 0 ).getSelectableNode();
			default -> selectClause;
		};
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restriction

	@Nullable
	@Override
	public SqmPredicate getRestriction() {
		return getQuerySpec().getRestriction();
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> where(@Nonnull Expression<Boolean> restriction) {
		getQuerySpec().setRestriction( restriction );
		return this;
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> where(@Nonnull BooleanExpression... restrictions) {
		getQuerySpec().setRestriction( restrictions );
		return this;
	}

	@Nonnull
	@Override
	public JpaSelectCriteria<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		getQuerySpec().setRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping

	@Nonnull
	@Override
	public List<Expression<?>> getGroupList() {
		return unmodifiableList( getQuerySpec().getGroupingExpressions() );
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> groupBy(@Nonnull Expression<?>... expressions) {
		getQuerySpec().setGroupingExpressions( expressions );
		return this;
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> groupBy(@Nonnull List<Expression<?>> grouping) {
		getQuerySpec().setGroupingExpressions( grouping );
		return this;
	}

	@Nullable
	@Override
	public SqmPredicate getGroupRestriction() {
		return getQuerySpec().getGroupRestriction();
	}

	@Nonnull
	@Override
	public SqmSelectQuery<T> having(@Nonnull Expression<Boolean> booleanExpression) {
		getQuerySpec().setGroupRestriction( booleanExpression );
		return this;
	}

	@Nonnull
	@Override
	public JpaSelectCriteria<T> having(@Nonnull BooleanExpression... restrictions) {
		getQuerySpec().setGroupRestriction( restrictions );
		return this;
	}

	@Nonnull
	@Override
	public JpaSelectCriteria<T> having(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		getQuerySpec().setGroupRestriction( nodeBuilder().wrap( restrictions ) );
		return this;
	}

	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( !cteStatements.isEmpty() ) {
			hql.append( "with " );
			for ( var value : cteStatements.values() ) {
				value.appendHqlString( hql, context );
				hql.append( ", " );
			}
			hql.setLength( hql.length() - 2 );
		}
		sqmQueryPart.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AbstractSqmSelectQuery<?> that
			&& Objects.equals( this.resultType, that.resultType ) // for performance!
			&& this.sqmQueryPart.equals( that.sqmQueryPart )
			&& Objects.equals( this.cteStatements, that.cteStatements );
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode( cteStatements );
		result = 31 * result + sqmQueryPart.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof AbstractSqmSelectQuery<?> that
			&& Objects.equals( this.resultType, that.resultType ) // for performance!
			&& this.sqmQueryPart.isCompatible( that.sqmQueryPart )
			&& SqmCacheable.areCompatible( this.cteStatements, that.cteStatements );
	}

	@Override
	public int cacheHashCode() {
		int result = SqmCacheable.cacheHashCode( cteStatements );
		result = 31 * result + sqmQueryPart.cacheHashCode();
		return result;
	}

	@SuppressWarnings("unchecked")
	protected Selection<? extends T> getResultSelection(Selection<?>[] selections) {
		final var resultType = getResultType();
		if ( resultType == Object.class ) {
			return switch ( selections.length ) {
				case 0 -> throw new IllegalArgumentException( "Empty selections passed to criteria query typed as Object" );
				case 1 -> (Selection<? extends T>) selections[0];
				default -> (Selection<? extends T>) nodeBuilder().array( selections );
			};
		}
		else if ( Tuple.class.isAssignableFrom( resultType ) ) {
			return (Selection<? extends T>) nodeBuilder().tuple( selections );
		}
		else if ( resultType.isArray() ) {
			return nodeBuilder().array( resultType, selections );
		}
		else {
			return nodeBuilder().construct( resultType, selections );
		}
	}

}
