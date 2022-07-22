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

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSqmSelectQuery<T>
		extends AbstractSqmNode
		implements SqmSelectQuery<T>, SqmCteContainer {
	private final Map<String, SqmCteStatement<?>> cteStatements;
	private boolean withRecursive;
	private SqmQueryPart<T> sqmQueryPart;
	private Class<T> resultType;

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
			boolean withRecursive,
			Class<T> resultType) {
		super( builder );
		this.cteStatements = cteStatements;
		this.withRecursive = withRecursive;
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
	public boolean isWithRecursive() {
		return withRecursive;
	}

	@Override
	public void setWithRecursive(boolean withRecursive) {
		this.withRecursive = withRecursive;
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
	public void addCteStatement(SqmCteStatement<?> cteStatement) {
		if ( cteStatements.putIfAbsent( cteStatement.getCteTable().getCteName(), cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getCteName() + " already exists" );
		}
	}

	@Override
	public Class<T> getResultType() {
		return resultType;
	}

	protected void setResultType(Class<T> resultType) {
		this.resultType = resultType;
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
		return from( subquery, false );
	}

	@Override
	public <X> SqmDerivedRoot<X> fromLateral(Subquery<X> subquery) {
		return from( subquery, true );
	}

	@Override
	public <X> SqmDerivedRoot<X> from(Subquery<X> subquery, boolean lateral) {
		validateComplianceFromSubQuery();
		final SqmDerivedRoot<X> root = new SqmDerivedRoot<>( (SqmSubQuery<X>) subquery, null, lateral );
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
			if ( withRecursive ) {
				sb.append( "recursive " );
			}
			for ( SqmCteStatement<?> value : cteStatements.values() ) {
				value.appendHqlString( sb );
				sb.append( ", " );
			}
			sb.setLength( sb.length() - 2 );
		}
		sqmQueryPart.appendHqlString( sb );
	}
}
