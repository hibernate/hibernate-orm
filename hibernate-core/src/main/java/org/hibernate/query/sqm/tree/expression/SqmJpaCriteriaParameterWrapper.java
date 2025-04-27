/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Consumer;

import org.hibernate.query.BindableType;
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

	public SqmJpaCriteriaParameterWrapper(
			BindableType<T> type,
			JpaCriteriaParameter<T> jpaCriteriaParameter,
			NodeBuilder criteriaBuilder) {
		super( toSqmType( type, criteriaBuilder ), criteriaBuilder );
		this.jpaCriteriaParameter = jpaCriteriaParameter;
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
	public int compareTo(SqmParameter anotherParameter) {
		return anotherParameter instanceof SqmJpaCriteriaParameterWrapper<?> wrapper
				? getJpaCriteriaParameter().compareTo( wrapper.getJpaCriteriaParameter() )
				: 1;
	}

//	@Override
//	public boolean equals(Object object) {
//		return object instanceof SqmJpaCriteriaParameterWrapper<?> that
//			&& Objects.equals( this.jpaCriteriaParameter, that.jpaCriteriaParameter );
//	}
//
//	@Override
//	public int hashCode() {
//		return jpaCriteriaParameter.hashCode();
//	}
}
