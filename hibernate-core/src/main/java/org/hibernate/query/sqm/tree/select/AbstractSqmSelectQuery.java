/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetReturningFunction;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import static java.lang.Character.isAlphabetic;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

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
		for ( Map.Entry<String, SqmCteStatement<?>> entry : cteStatements.entrySet() ) {
			copies.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return copies;
	}

	@Override
	public Collection<SqmCteStatement<?>> getCteStatements() {
		return cteStatements.values();
	}

	Map<String, SqmCteStatement<?>> getCteStatementMap() {
		return new LinkedHashMap<>( cteStatements );
	}

	@Override
	public SqmCteStatement<?> getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return cteStatements.values();
	}

	@Override @SuppressWarnings("unchecked")
	public <X> JpaCteCriteria<X> getCteCriteria(String cteName) {
		return (JpaCteCriteria<X>) cteStatements.get( cteName );
	}

	@Override
	public <X> JpaCteCriteria<X> with(AbstractQuery<X> criteria) {
		return withInternal( generateAlias(), criteria );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( generateAlias(), baseCriteria, true, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> with(String name, AbstractQuery<X> criteria) {
		return withInternal( validateCteName( name ), criteria );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			String name,
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( validateCteName( name ), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			String name,
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
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
		final SqmCteStatement<X> cteStatement = new SqmCteStatement<>(
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
		final SqmCteStatement<X> cteStatement = new SqmCteStatement<>(
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

	@Override
	public Class<T> getResultType() {
		return resultType;
	}

	@Override
	public SqmQuerySpec<T> getQuerySpec() {
		return sqmQueryPart.getFirstQuerySpec();
	}

	@Override
	public SqmQueryPart<T> getQueryPart() {
		return sqmQueryPart;
	}

	public void setQueryPart(SqmQueryPart<T> sqmQueryPart) {
		this.sqmQueryPart = sqmQueryPart;
	}

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
		final List<SqmRoot<?>> rootList = getQuerySpec().getRootList();
		if ( rootList.size() <= position ) {
			throw new IllegalArgumentException( "Not enough root entities" );
		}
		return castRoot( rootList.get( position ), type );
	}

	/**
	 * @see org.hibernate.query.criteria.JpaCriteriaQuery#getRoot(String, Class)
	 */
	public <E> JpaRoot<? extends E> getRoot(String alias, Class<E> type) {
		final List<SqmRoot<?>> rootList = getQuerySpec().getRootList();
		for ( SqmRoot<?> root : rootList ) {
			final String rootAlias = root.getAlias();
			if ( rootAlias != null && rootAlias.equals( alias ) ) {
				return castRoot( root, type );
			}
		}
		throw new IllegalArgumentException( "No root entity with alias " + alias );
	}

	private static <E> JpaRoot<? extends E> castRoot(JpaRoot<?> root, Class<E> type) {
		final Class<?> rootEntityType = root.getJavaType();
		if ( rootEntityType == null ) {
			throw new AssertionFailure( "Java type of root entity was null" );
		}
		if ( !type.isAssignableFrom( rootEntityType ) ) {
			throw new IllegalArgumentException( "Root entity of type '" + rootEntityType.getTypeName()
												+ "' did not have the given type '" + type.getTypeName() + "'");
		}
		@SuppressWarnings("unchecked") // safe, we just checked
		final JpaRoot<? extends E> result = (JpaRoot<? extends E>) root;
		return result;
	}

	@Override
	public <X> SqmRoot<X> from(Class<X> entityClass) {
		return addRoot(
				new SqmRoot<>(
						nodeBuilder().getDomainModel().entity( entityClass ),
						generateAlias(),
						true,
						nodeBuilder()
				)
		);
	}

	@Override
	public <X> SqmDerivedRoot<X> from(Subquery<X> subquery) {
		validateComplianceFromSubQuery();
		final SqmDerivedRoot<X> root = new SqmDerivedRoot<>( (SqmSubQuery<X>) subquery, null );
		addRoot( root );
		return root;
	}

	public <X> JpaRoot<X> from(JpaCteCriteria<X> cte) {
		final SqmCteRoot<X> root = new SqmCteRoot<>( ( SqmCteStatement<X> ) cte, null );
		addRoot( root );
		return root;
	}

	@Override
	public <X> JpaFunctionRoot<X> from(JpaSetReturningFunction<X> function) {
		final SqmFunctionRoot<X> root = new SqmFunctionRoot<>( (SqmSetReturningFunction<X>) function, null );
		addRoot( root );
		return root;
	}

	private <X> SqmRoot<X> addRoot(SqmRoot<X> root) {
		getQuerySpec().addRoot( root );
		return root;
	}

	@Override
	public <X> SqmRoot<X> from(EntityType<X> entityType) {
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

	@Override
	public SqmSelectQuery<T> distinct(boolean distinct) {
		getQuerySpec().setDistinct( distinct );
		return this;
	}

	@Override
	public JpaSelection<T> getSelection() {
		return getQuerySpec().getSelection();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restriction

	@Override
	public SqmPredicate getRestriction() {
		return getQuerySpec().getRestriction();
	}

	@Override
	public SqmSelectQuery<T> where(Expression<Boolean> restriction) {
		getQuerySpec().setRestriction( restriction );
		return this;
	}

	@Override
	public SqmSelectQuery<T> where(Predicate... restrictions) {
		getQuerySpec().setRestriction( restrictions );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping

	@Override
	public List<Expression<?>> getGroupList() {
		return unmodifiableList( getQuerySpec().getGroupingExpressions() );
	}

	@Override
	public SqmSelectQuery<T> groupBy(Expression<?>... expressions) {
		getQuerySpec().setGroupingExpressions( List.of( expressions ) );
		return this;
	}

	@Override
	public SqmSelectQuery<T> groupBy(List<Expression<?>> grouping) {
		getQuerySpec().setGroupingExpressions( grouping );
		return this;
	}

	@Override
	public SqmPredicate getGroupRestriction() {
		return getQuerySpec().getGroupRestriction();
	}

	@Override
	public SqmSelectQuery<T> having(Expression<Boolean> booleanExpression) {
		getQuerySpec().setGroupRestriction( nodeBuilder().wrap( booleanExpression ) );
		return this;
	}

	@Override
	public SqmSelectQuery<T> having(Predicate... predicates) {
		getQuerySpec().setGroupRestriction( nodeBuilder().wrap( predicates ) );
		return this;
	}

	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( !cteStatements.isEmpty() ) {
			hql.append( "with " );
			for ( SqmCteStatement<?> value : cteStatements.values() ) {
				value.appendHqlString( hql, context );
				hql.append( ", " );
			}
			hql.setLength( hql.length() - 2 );
		}
		sqmQueryPart.appendHqlString( hql, context );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof AbstractSqmSelectQuery<?> that
			&& Objects.equals( this.resultType, that.resultType ) // for performance!
			&& Objects.equals( this.sqmQueryPart, that.sqmQueryPart )
			&& Objects.equals( this.cteStatements, that.cteStatements );
	}

	@Override
	public int hashCode() {
		return Objects.hash( cteStatements, sqmQueryPart );
	}

	@SuppressWarnings("unchecked")
	protected Selection<? extends T> getResultSelection(Selection<?>[] selections) {
		final Class<T> resultType = getResultType();
		if ( resultType == null || resultType == Object.class ) {
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
