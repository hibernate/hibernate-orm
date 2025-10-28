/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Consumer;

import org.hibernate.type.BindableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

import static org.hibernate.query.sqm.tree.expression.SqmExpressionHelper.toSqmType;

/**
 * Acts as the per-use wrapper for a {@link JpaCriteriaParameter}
 * ({@link jakarta.persistence.criteria.CriteriaBuilder#parameter}).
 * <p>
 * {@code JpaCriteriaParameter} is the "domain query parameter"
 * ({@link org.hibernate.query.QueryParameter} while
 * {@code SqmJpaCriteriaParameterWrapper} is the {@link SqmParameter}
 */
public class SqmJpaCriteriaParameterWrapper<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T> {
	private final JpaCriteriaParameter<T> jpaCriteriaParameter;
	private final int criteriaParameterId;
	private final int unnamedParameterId;

	public SqmJpaCriteriaParameterWrapper(
			BindableType<T> type,
			JpaCriteriaParameter<T> jpaCriteriaParameter,
			int criteriaParameterId,
			int unnamedParameterId,
			NodeBuilder criteriaBuilder) {
		super( toSqmType( type, criteriaBuilder ), criteriaBuilder );
		this.jpaCriteriaParameter = jpaCriteriaParameter;
		this.criteriaParameterId = criteriaParameterId;
		this.unnamedParameterId = unnamedParameterId;
	}

	@Override
	public SqmJpaCriteriaParameterWrapper<T> copy(SqmCopyContext context) {
		final SqmJpaCriteriaParameterWrapper<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmJpaCriteriaParameterWrapper<>(
						getNodeType(),
						jpaCriteriaParameter.copy( context ),
						criteriaParameterId,
						unnamedParameterId,
						nodeBuilder()
				)
		);
	}

	@Override
	public String getName() {
		return jpaCriteriaParameter.getName();
	}

	@Override
	public Integer getPosition() {
		// for criteria anyway, these cannot be positional
		return null;
	}

	public JpaCriteriaParameter<T> getJpaCriteriaParameter() {
		return jpaCriteriaParameter;
	}

	/**
	 * The 0-based encounter of a {@link JpaCriteriaParameter} instance in a
	 * {@link org.hibernate.query.sqm.SqmQuerySource#CRITERIA} query.
	 *
	 * @see org.hibernate.query.sqm.tree.jpa.ParameterCollector
	 */
	public int getCriteriaParameterId() {
		return criteriaParameterId;
	}

	/**
	 * The 0-based encounter of an unnamed {@link JpaCriteriaParameter} instance in a
	 * {@link org.hibernate.query.sqm.SqmQuerySource#CRITERIA} query.
	 * If the {@link #getJpaCriteriaParameter()} has a name, returns -1.
	 *
	 * @see org.hibernate.query.sqm.tree.jpa.ParameterCollector
	 */
	public int getUnnamedParameterId() {
		return unnamedParameterId;
	}

	@Override
	public Class<T> getParameterType() {
		return jpaCriteriaParameter.getParameterType();
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return jpaCriteriaParameter.allowsMultiValuedBinding();
	}

	@Override
	public BindableType<T> getAnticipatedType() {
		return getNodeType();
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmJpaCriteriaParameterWrapper<>(
				getNodeType(),
				jpaCriteriaParameter,
				criteriaParameterId,
				unnamedParameterId,
				nodeBuilder()
		);
	}

	/**
	 * Unsupported.  Visitation for a criteria parameter should be handled
	 * as part of {@link SemanticQueryWalker#visitJpaCriteriaParameter}.
	 * This wrapper is intended just for representing unique SqmParameter
	 * references for each {@link JpaCriteriaParameter} occurrence in the
	 * SQM tree as part of the {@link org.hibernate.query.QueryParameter}
	 * to {@link SqmParameter} to {@link JdbcParameter} transformation.
	 * Each occurrence requires a unique {@link SqmParameter} to make
	 * sure we ultimately get the complete set of {@code JdbcParameter}
	 * references.
	 */
	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		throw new UnsupportedOperationException(
				"""
				Direct SemanticQueryWalker visitation of a SqmJpaCriteriaParameterWrapper \
				is not supported. Visitation for a criteria parameter should be handled \
				during SemanticQueryWalker#visitJpaCriteriaParameter. This wrapper is \
				intended only for representing unique SQM parameter nodes for each criteria \
				parameter in the SQM tree as part of the QueryParameter -> SqmParameter -> JdbcParameter \
				transformation. Each occurrence requires a unique SqmParameter to make sure we \
				ultimately get the complete set of JdbcParameter references.\
				"""
		);
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		// nothing to do
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		jpaCriteriaParameter.appendHqlString( hql, context );
	}

	@Override
	public final boolean equals(Object o) {
		return o instanceof SqmJpaCriteriaParameterWrapper<?> that
			&& criteriaParameterId == that.criteriaParameterId;
	}

	@Override
	public int hashCode() {
		return criteriaParameterId;
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
