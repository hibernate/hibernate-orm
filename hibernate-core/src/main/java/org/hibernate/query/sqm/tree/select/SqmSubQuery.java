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
import java.util.Set;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.SqmQuery;
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
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmSubQuery<T> extends AbstractSqmSelectQuery<T> implements SqmSelectQuery<T>, JpaSubQuery<T>, SqmExpression<T> {
	private final SqmQuery<?> parent;

	private SqmExpressable<T> expressableType;
	private String alias;

	public SqmSubQuery(
			SqmQuery<?> parent,
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			NodeBuilder builder) {
		super( queryPart, resultType, builder );
		this.parent = parent;
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			Class<T> resultType,
			NodeBuilder builder) {
		super( resultType, builder );
		this.parent = parent;
	}

	public SqmSubQuery(
			SqmQuery<?> parent,
			NodeBuilder builder) {
		super( null, builder );
		this.parent = parent;
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
		return this;
	}

	@Override
	public SqmExpression<T> getSelection() {
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
		for ( SqmRoot<?> root : getQuerySpec().getFromClause().getRoots() ) {
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
	public SqmExpressable<T> getNodeType() {
		return expressableType;
	}

	@Override
	public void applyInferableType(SqmExpressable<?> type) {
		//noinspection unchecked
		this.expressableType = (SqmExpressable<T>) type;
		setResultType( type == null ? null : expressableType.getExpressableJavaTypeDescriptor().getJavaTypeClass() );
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
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		if ( getNodeType() == null ) {
			return null;
		}
		return getNodeType().getExpressableJavaTypeDescriptor();
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
