/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * {@link JpaParameterExpression} created via JPA {@link jakarta.persistence.criteria.CriteriaBuilder}.
 * <p>
 * Each occurrence of a {@code JpaParameterExpression} results in a unique {@link SqmParameter}.
 *
 * @see ParameterMetadata
 * @see NodeBuilder#parameter
 *
 * @author Steve Ebersole
 */
public class JpaCriteriaParameter<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T>, QueryParameterImplementor<T> {

	private final String name;
	private boolean allowsMultiValuedBinding;

	public JpaCriteriaParameter(
			String name,
			BindableType<? super T> type,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder.resolveExpressible( type ), nodeBuilder );
		this.name = name;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}

	protected JpaCriteriaParameter(JpaCriteriaParameter<T> original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.name = original.name;
		this.allowsMultiValuedBinding = original.allowsMultiValuedBinding;
	}

	@Override
	public JpaCriteriaParameter<T> copy(SqmCopyContext context) {
		// Don't create a copy of regular parameters because identity is important here
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	public T getValue() {
		return null;
	}

	@Override
	public Integer getPosition() {
		// for criteria anyway, these cannot be positional
		return null;
	}

	@Override
	public Integer getTupleLength() {
		// TODO: we should be able to do much better than this!
		return null;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowsMultiValuedBinding;
	}

	@Override
	public void disallowMultiValuedBinding() {
		allowsMultiValuedBinding = false;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return allowsMultiValuedBinding();
	}

	@Override
	public BindableType<T> getAnticipatedType() {
		return getHibernateType();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void applyAnticipatedType(BindableType type) {
		super.internalApplyInferableType( nodeBuilder().resolveExpressible( type ) );
	}

	@Override
	public SqmParameter<T> copy() {
		return new JpaCriteriaParameter<>( getName(), getAnticipatedType(), allowMultiValuedBinding(), nodeBuilder() );
	}

	@Override
	public BindableType<T> getHibernateType() {
		return getNodeType();
	}

	@Override
	public Class<T> getParameterType() {
		final SqmExpressible<T> nodeType = getNodeType();
		return nodeType == null ? null : nodeType.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	protected void internalApplyInferableType(SqmBindableType<?> newType) {
		super.internalApplyInferableType( newType );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitJpaCriteriaParameter( this );
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		throw new UnsupportedOperationException( "ParameterMemento cannot be extracted from Criteria query parameter" );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( ':' ).append( name( context ) );
	}

	private String name(SqmRenderContext context) {
		return name == null ? context.resolveParameterName( this ) : name;
	}

	@Override
	public boolean equals(Object object) {
		return this == object
			|| object instanceof JpaCriteriaParameter<?> that
				&& name != null
				&& Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return name == null ? super.hashCode() : name.hashCode();
	}

	// For caching, we can consider two parameters to be compatible if are unnamed, or they have the same name

	@Override
	public boolean isCompatible(Object object) {
		return getClass() == object.getClass()
			&& Objects.equals( name, ((JpaCriteriaParameter<?>) object).name );
	}

	@Override
	public int cacheHashCode() {
		return name == null ? 0 : name.hashCode();
	}
}
