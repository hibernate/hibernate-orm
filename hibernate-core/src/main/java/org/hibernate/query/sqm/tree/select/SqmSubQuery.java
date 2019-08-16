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
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
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
			SqmQuerySpec<T> querySpec,
			NodeBuilder builder) {
		super( querySpec, builder );
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
		super( (Class<T>) null, builder );
		this.parent = parent;
	}

	@Override
	public SqmExpressable<T> getExpressableType() {
		return expressableType;
	}

	@Override
	public SqmQuery<?> getContainingQuery() {
		return parent;
	}

	@Override
	public SqmSelectQuery<?> getParent() {
		// JPA only allows sub-queries on select queries
		return (SqmSelectQuery<?>) getContainingQuery();
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
	@SuppressWarnings("unchecked")
	public SqmSubQuery<T> select(Expression<T> expression) {
		getQuerySpec().setSelection( (JpaSelection) expression );
		return this;
	}

	@Override
	public SqmExpression<T> getSelection() {
		return (SqmExpression<T>) super.getSelection();
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
	public <Y> SqmRoot<Y> correlate(Root<Y> parentRoot) {
		throw new NotYetImplementedFor6Exception();
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
	public <X, Y> SqmSingularJoin<X, Y> correlate(Join<X, Y> parentJoin) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X, Y> SqmBagJoin<X, Y> correlate(CollectionJoin<X, Y> parentCollection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X, Y> SqmSetJoin<X, Y> correlate(SetJoin<X, Y> parentSet) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X, Y> SqmListJoin<X, Y> correlate(ListJoin<X, Y> parentList) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X, K, V> SqmMapJoin<X, K, V> correlate(MapJoin<X, K, V> parentMap) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Set<Join<?, ?>> getCorrelatedJoins() {
		throw new NotYetImplementedFor6Exception();
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
	public SqmInPredicate in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate in(Expression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate in(Collection<?> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmInPredicate in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmExpressable<T> getNodeType() {
		return expressableType;
	}

	@Override
	public void applyInferableType(SqmExpressable<?> type) {
		//noinspection unchecked
		this.expressableType = (SqmExpressable) type;
		setResultType( type == null ? null : type.getExpressableJavaTypeDescriptor().getJavaType() );
	}

	@Override
	public SqmExpression<Long> asLong() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.LONG );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.INTEGER );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.FLOAT );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.DOUBLE );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.BIG_DECIMAL );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.BIG_INTEGER );
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.STRING );
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
		//noinspection unchecked
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

}
