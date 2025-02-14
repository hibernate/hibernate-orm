/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
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

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
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
		setQueryPart( queryPart );
	}

	protected AbstractSqmSelectQuery(
			NodeBuilder builder,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType) {
		super( builder );
		this.cteStatements = cteStatements;
		this.resultType = resultType;
	}

	protected Map<String, SqmCteStatement<?>> copyCteStatements(SqmCopyContext context) {
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>( this.cteStatements.size() );
		for ( Map.Entry<String, SqmCteStatement<?>> entry : this.cteStatements.entrySet() ) {
			cteStatements.put( entry.getKey(), entry.getValue().copy( context ) );
		}
		return cteStatements;
	}

	@Override
	public Collection<SqmCteStatement<?>> getCteStatements() {
		return cteStatements.values();
	}

	@Override
	public SqmCteStatement<?> getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	public Collection<? extends JpaCteCriteria<?>> getCteCriterias() {
		return cteStatements.values();
	}

	@Override
	public <X> JpaCteCriteria<X> getCteCriteria(String cteName) {
		return (JpaCteCriteria<X>) cteStatements.get( cteName );
	}

	@Override
	public <X> JpaCteCriteria<X> with(AbstractQuery<X> criteria) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), criteria );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionAll(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), baseCriteria, false, recursiveCriteriaProducer );
	}

	@Override
	public <X> JpaCteCriteria<X> withRecursiveUnionDistinct(
			AbstractQuery<X> baseCriteria,
			Function<JpaCteCriteria<X>, AbstractQuery<X>> recursiveCriteriaProducer) {
		return withInternal( SqmCreationHelper.acquireUniqueAlias(), baseCriteria, true, recursiveCriteriaProducer );
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
		if ( !Character.isAlphabetic( name.charAt( 0 ) ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Illegal CTE name [%s]. Names must start with an alphabetic character!",
							name
					)
			);
		}
		return name;
	}

	private <X> JpaCteCriteria<X> withInternal(String name, AbstractQuery<X> criteria) {
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

	private <X> JpaCteCriteria<X> withInternal(
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

	protected void setResultType(Class<T> resultType) {
		// No-op
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
	@SuppressWarnings("unchecked")
	public Set<Root<?>> getRoots() {
		return (Set) getQuerySpec().getRoots();
	}

	@Override
	public <X> SqmRoot<X> from(Class<X> entityClass) {
		return addRoot(
				new SqmRoot<>(
						nodeBuilder().getDomainModel().entity( entityClass ),
						null,
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

	private <X> SqmRoot<X> addRoot(SqmRoot<X> root) {
		getQuerySpec().addRoot( root );
		return root;
	}

	@Override
	public <X> SqmRoot<X> from(EntityType<X> entityType) {
		return addRoot( new SqmRoot<>( (EntityDomainType<X>) entityType, null, true, nodeBuilder() ) );
	}

	private void validateComplianceFromSubQuery() {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries in the from clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
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
	@SuppressWarnings("unchecked")
	public List<Expression<?>> getGroupList() {
		return (List) getQuerySpec().getGroupingExpressions();
	}

	@Override
	public SqmSelectQuery<T> groupBy(Expression<?>... expressions) {
		return groupBy( Arrays.asList( expressions ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectQuery<T> groupBy(List<Expression<?>> grouping) {
		getQuerySpec().setGroupingExpressions( (List) grouping );
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

//
//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// Limit
//
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public <X> ExpressionImplementor<X> getLimit() {
//		return limit;
//	}
//
//	@Override
//	public C setLimit(JpaExpression<?> limit) {
//		this.limit = (ExpressionImplementor) limit;
//		return this;
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public <X> ExpressionImplementor<X> getOffset() {
//		return offset;
//	}
//
//	@Override
//	public C setOffset(JpaExpression offset) {
//		this.offset = (ExpressionImplementor) offset;
//		return this;
//	}

	public void appendHqlString(StringBuilder sb) {
		if ( !cteStatements.isEmpty() ) {
			sb.append( "with " );
			for ( SqmCteStatement<?> value : cteStatements.values() ) {
				value.appendHqlString( sb );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
		}
		sqmQueryPart.appendHqlString( sb );
	}

	protected Selection<? extends T> getResultSelection(Selection<?>[] selections) {
		final Selection<? extends T> resultSelection;
		Class<T> resultType = getResultType();
		if ( resultType == null || resultType == Object.class ) {
			switch ( selections.length ) {
				case 0: {
					throw new IllegalArgumentException(
							"empty selections passed to criteria query typed as Object"
					);
				}
				case 1: {
					resultSelection = ( Selection<? extends T> ) selections[0];
					break;
				}
				default: {
					resultSelection = ( Selection<? extends T> ) nodeBuilder().array( selections );
				}
			}
		}
		else if ( Tuple.class.isAssignableFrom( resultType ) ) {
			resultSelection = ( Selection<? extends T> ) nodeBuilder().tuple( selections );
		}
		else if ( resultType.isArray() ) {
			resultSelection = nodeBuilder().array( resultType, selections );
		}
		else {
			resultSelection = nodeBuilder().construct( resultType, selections );
		}
		return resultSelection;
	}

}
