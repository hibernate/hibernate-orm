/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaCteContainer;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedListJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSubQuery<T> extends AbstractSqmSelectQuery<T> implements SqmSelectQuery<T>, JpaSubQuery<T>, SqmExpression<T> {
	private final SqmQuery<?> parent;

	private SqmExpressible<T> expressibleType;
	private String alias;

	public SqmSubQuery(
			SqmQuery<?> parent,
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			NodeBuilder builder) {
		super( queryPart, resultType, builder );
		this.parent = parent;
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			Map<String, SqmCteStatement<?>> cteStatements,
			NodeBuilder builder) {
		super( builder, cteStatements, resultType );
		this.parent = parent;
		setQueryPart( queryPart );
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			Class<T> resultType,
			NodeBuilder builder) {
		super( resultType, builder );
		this.parent = parent;
		applyInferableType( resultType );
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			NodeBuilder builder) {
		super( null, builder );
		this.parent = parent;
	}

	private SqmSubQuery(
			NodeBuilder builder,
			Map<String, SqmCteStatement<?>> cteStatements,
			Class<T> resultType,
			SqmQuery<?> parent,
			SqmExpressible<T> expressibleType,
			String alias) {
		super( builder, cteStatements, resultType );
		this.parent = parent;
		this.expressibleType = expressibleType;
		this.alias = alias;
	}

	@Override
	public SqmSubQuery<T> copy(SqmCopyContext context) {
		final SqmSubQuery<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmSubQuery<T> statement = context.registerCopy(
				this,
				new SqmSubQuery<>(
						nodeBuilder(),
						copyCteStatements( context ),
						getResultType(),
						parent.copy( context ),
						getExpressible(),
						getAlias()
				)
		);
		statement.setQueryPart( getQueryPart().copy( context ) );
		return statement;
	}

	@Override
	public SqmCteStatement<?> getCteStatement(String cteLabel) {
		final SqmCteStatement<?> cteCriteria = super.getCteStatement( cteLabel );
		if ( cteCriteria != null || !( parent instanceof SqmCteContainer ) ) {
			return cteCriteria;
		}
		return ( (SqmCteContainer) parent ).getCteStatement( cteLabel );
	}

	@Override
	public <X> JpaCteCriteria<X> getCteCriteria(String cteName) {
		final JpaCteCriteria<X> cteCriteria = super.getCteCriteria( cteName );
		if ( cteCriteria != null || !( parent instanceof JpaCteContainer ) ) {
			return cteCriteria;
		}
		return ( (JpaCteContainer) parent ).getCteCriteria( cteName );
	}

	@Override
	public SqmQuery<?> getContainingQuery() {
		return parent;
	}

	@Override
	public SqmSelectQuery<?> getParent() {
		final SqmQuery<?> containingQuery = getContainingQuery();
		// JPA only allows sub-queries on select queries
		if ( !(containingQuery instanceof AbstractQuery) ) {
			throw new IllegalStateException( "Cannot call getParent on update/delete criterias" );
		}
		return (SqmSelectQuery<?>) containingQuery;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public SqmSubQuery<T> alias(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public SqmSubQuery<T> select(Expression<T> expression) {
		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		//noinspection unchecked
		querySpec.setSelection( (JpaSelection<T>) expression );
//		applyInferableType( (Class<T>) querySpec.getSelection().getJavaType() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSubQuery<T> multiselect(Selection<?>... selections) {
		validateComplianceMultiselect();

		final Selection<? extends T> resultSelection = getResultSelection( selections );

		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		querySpec.setSelection( (JpaSelection<T>) resultSelection );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSubQuery<T> multiselect(List<Selection<?>> selectionList) {
		validateComplianceMultiselect();

		final Selection<? extends T> resultSelection;
		final Class<T> resultType = getResultType();
		final List<? extends JpaSelection<?>> selections = (List<? extends JpaSelection<?>>) (List<?>) selectionList;
		if ( resultType == null || resultType == Object.class ) {
			switch ( selections.size() ) {
				case 0: {
					throw new IllegalArgumentException(
							"empty selections passed to criteria query typed as Object"
					);
				}
				case 1: {
					resultSelection = ( Selection<? extends T> ) selections.get( 0 );
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
		final SqmQuerySpec<T> querySpec = getQuerySpec();
		if ( querySpec.getSelectClause() == null ) {
			querySpec.setSelectClause( new SqmSelectClause( false, 1, nodeBuilder() ) );
		}
		querySpec.setSelection( (JpaSelection<T>) resultSelection );

		return this;
	}

	@Override
	public SqmExpression<T> getSelection() {
		final SqmSelectClause selectClause = getQuerySpec().getSelectClause();
		if ( selectClause == null ) {
			return null;
		}
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return getQuerySpec().getSelection().isCompoundSelection();
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return null;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		if ( ! isCompoundSelection() ) {
			throw new IllegalStateException( "JPA selection is not compound" );
		}
		return getQuerySpec().getSelection().getCompoundSelectionItems();
	}

	@Override
	public SqmSubQuery<T> distinct(boolean distinct) {
		return (SqmSubQuery<T>) super.distinct( distinct );
	}

	@Override
	public SqmSubQuery<T> where(Expression<Boolean> restriction) {
		return (SqmSubQuery<T>) super.where( restriction );
	}

	@Override
	public SqmSubQuery<T> where(Predicate... restrictions) {
		return (SqmSubQuery<T>) super.where( restrictions );
	}

	@Override
	public SqmSubQuery<T> groupBy(Expression<?>... expressions) {
		return (SqmSubQuery<T>) super.groupBy( expressions );
	}

	@Override
	public SqmSubQuery<T> groupBy(List<Expression<?>> grouping) {
		return (SqmSubQuery<T>) super.groupBy( grouping );
	}

	@Override
	public SqmSubQuery<T> having(Expression<Boolean> booleanExpression) {
		return (SqmSubQuery<T>) super.having( booleanExpression );
	}

	@Override
	public SqmSubQuery<T> having(Predicate... predicates) {
		return (SqmSubQuery<T>) super.having( predicates );
	}

	@Override
	public JpaExpression<Number> getOffset() {
		//noinspection unchecked
		return (JpaExpression<Number>) getQueryPart().getOffset();
	}

	@Override
	public JpaSubQuery<T> offset(JpaExpression<? extends Number> offset) {
		validateComplianceFetchOffset();
		getQueryPart().setOffset( offset );
		return this;
	}

	@Override
	public JpaSubQuery<T> offset(Number offset) {
		validateComplianceFetchOffset();
		getQueryPart().setOffset( nodeBuilder().value( offset ) );
		return this;
	}

	@Override
	public JpaExpression<Number> getFetch() {
		//noinspection unchecked
		return (JpaExpression<Number>) getQueryPart().getFetch();
	}

	@Override
	public JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( fetch, fetchClauseType );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(Number fetch) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ) );
		return this;
	}

	@Override
	public JpaSubQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType) {
		validateComplianceFetchOffset();
		getQueryPart().setFetch( nodeBuilder().value( fetch ), fetchClauseType );
		return this;
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return getQueryPart().getFetchClauseType();
	}

	@Override
	public List<JpaOrder> getOrderList() {
		//noinspection rawtypes,unchecked
		return (List) getQueryPart().getSortSpecifications();
	}

	@Override
	public JpaSubQuery<T> orderBy(Order... orders) {
		validateComplianceOrderBy();
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.length );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	@Override
	public JpaSubQuery<T> orderBy(List<Order> orders) {
		validateComplianceOrderBy();
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orders.size() );
		for ( Order order : orders ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) order );
		}
		getQueryPart().setOrderByClause( sqmOrderByClause );
		return this;
	}

	private void validateComplianceMultiselect() {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having multiple select items. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	private void validateComplianceOrderBy() {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having an order by clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	private void validateComplianceFetchOffset() {
		if ( nodeBuilder().getDomainModel().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			throw new IllegalStateException(
					"The JPA specification does not support subqueries having a fetch or offset clause. " +
							"Please disable the JPA query compliance if you want to use this feature." );
		}
	}

	@Override
	public <Y> SqmRoot<Y> correlate(Root<Y> parentRoot) {
		final SqmCorrelatedRoot<Y> correlated = ( (SqmRoot<Y>) parentRoot ).createCorrelation();
		getQuerySpec().addRoot( correlated );
		return correlated;
	}

	@Override
	public <X, Y> SqmAttributeJoin<X, Y> correlate(Join<X, Y> join) {
		if ( join instanceof PluralJoin<?, ?, ?> ) {
			final PluralJoin<?, ?, ?> pluralJoin = (PluralJoin<?, ?, ?>) join;
			switch ( pluralJoin.getModel().getCollectionType() ) {
				case COLLECTION:
					return correlate( (CollectionJoin<X, Y>) join );
				case LIST:
					return correlate( (ListJoin<X, Y>) join );
				case SET:
					return correlate( (SetJoin<X, Y>) join );
				case MAP:
					return correlate( (MapJoin<X, ?, Y>) join );
			}
		}
		final SqmCorrelatedSingularJoin<X, Y> correlated = ( (SqmSingularJoin<X, Y>) join ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmBagJoin<X, Y> correlate(CollectionJoin<X, Y> parentCollection) {
		final SqmCorrelatedBagJoin<X, Y> correlated = ( (SqmBagJoin<X, Y>) parentCollection ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmSetJoin<X, Y> correlate(SetJoin<X, Y> parentSet) {
		final SqmCorrelatedSetJoin<X, Y> correlated = ( (SqmSetJoin<X, Y>) parentSet ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, Y> SqmListJoin<X, Y> correlate(ListJoin<X, Y> parentList) {
		final SqmCorrelatedListJoin<X, Y> correlated = ( (SqmListJoin<X, Y>) parentList ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X, K, V> SqmMapJoin<X, K, V> correlate(MapJoin<X, K, V> parentMap) {
		final SqmCorrelatedMapJoin<X, K, V> correlated = ( (SqmMapJoin<X, K, V>) parentMap ).createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X> SqmCrossJoin<X> correlate(SqmCrossJoin<X> parentCrossJoin) {
		final SqmCorrelatedCrossJoin<X> correlated = parentCrossJoin.createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public <X> SqmEntityJoin<X> correlate(SqmEntityJoin<X> parentEntityJoin) {
		final SqmCorrelatedEntityJoin<X> correlated = parentEntityJoin.createCorrelation();
		getQuerySpec().addRoot( correlated.getCorrelatedRoot() );
		return correlated;
	}

	@Override
	public Set<Join<?, ?>> getCorrelatedJoins() {
		final Set<Join<?, ?>> correlatedJoins = new HashSet<>();
		final SqmFromClause fromClause = getQuerySpec().getFromClause();
		if ( fromClause == null ) {
			return correlatedJoins;
		}

		for ( SqmRoot<?> root : fromClause.getRoots() ) {
			if ( root instanceof SqmCorrelation<?, ?> ) {
				for ( SqmJoin<?, ?> sqmJoin : root.getSqmJoins() ) {
					if ( sqmJoin instanceof SqmCorrelation<?, ?> && sqmJoin instanceof Join<?, ?> ) {
						correlatedJoins.add( (Join<?, ?>) sqmJoin );
					}
				}
			}
		}
		return correlatedJoins;
	}

	@Override
	public Set<SqmJoin<?, ?>> getCorrelatedSqmJoins() {
		final Set<SqmJoin<?, ?>> correlatedJoins = new HashSet<>();
		for ( SqmRoot<?> root : getQuerySpec().getFromClause().getRoots() ) {
			if ( root instanceof SqmCorrelation<?, ?> ) {
				for ( SqmJoin<?, ?> sqmJoin : root.getSqmJoins() ) {
					if ( sqmJoin instanceof SqmCorrelation<?, ?> ) {
						correlatedJoins.add( sqmJoin );
					}
				}
			}
		}
		return correlatedJoins;
	}

	@Override
	public SqmPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	public SqmPredicate isNotNull() {
		return nodeBuilder().isNotNull( this );
	}

	@Override
	public SqmInPredicate<?> in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate<?> in(Expression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate<?> in(Collection<?> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate<?> in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return expressibleType;
	}

	@Override
	public void applyInferableType(SqmExpressible<?> type) {
		//noinspection unchecked
		this.expressibleType = (SqmExpressible<T>) type;
	}

	private void applyInferableType(Class<T> type) {
		final EntityDomainType<T> entityDescriptor = nodeBuilder().getSessionFactory().getRuntimeMetamodels()
				.getJpaMetamodel()
				.findEntityType( type );
		if ( entityDescriptor != null ) {
			this.expressibleType = entityDescriptor;
		}
		else {
			this.expressibleType = nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( type );
		}
	}

	@Override
	public SqmExpression<Long> asLong() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Long.class ) );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Float.class ) );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( Double.class ) );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigDecimal.class ) );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( BigInteger.class ) );
	}

	@Override
	public SqmExpression<String> asString() {
		return castAs( nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( String.class ) );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast( this, type );
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		if ( getNodeType() == null ) {
			return null;
		}
		return getNodeType().getExpressibleJavaType();
	}

	@Override
	public Class<? extends T> getJavaType() {
		return getResultType();
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	@Override
	public String asLoggableText() {
		return "<sub-query>";
	}

	@Override
	public <T1> T1 accept(SemanticQueryWalker<T1> walker) {
		return walker.visitSubQueryExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( '(' );
		super.appendHqlString( sb );
		sb.append( ')' );
	}

}
