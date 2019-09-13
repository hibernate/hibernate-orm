/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * {@link JpaParameterExpression} created via JPA {@link javax.persistence.criteria.CriteriaBuilder}.
 *
 * Each occurence of a JpaParameterExpression results in a unique SqmParameter
 *
 * @see ParameterMetadata
 * @see NodeBuilder#parameter
 *
 * @author Steve Ebersole
 */
public class JpaCriteriaParameter<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T>, QueryParameterImplementor<T>, DomainResultProducer<T> {
	private final String name;
	private boolean allowsMultiValuedBinding;

	public JpaCriteriaParameter(
			String name,
			AllowableParameterType<T> type,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		this.name = name;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}
	public JpaCriteriaParameter(
			AllowableParameterType<T> type,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		this( null, type, allowsMultiValuedBinding, nodeBuilder );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		// for criteria anyway, these cannot be positional
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
	public AllowableParameterType<T> getAnticipatedType() {
		return getHibernateType();
	}

	@Override
	public void applyAnticipatedType(AllowableParameterType type) {
		super.internalApplyInferableType( type );
	}

	@Override
	public AllowableParameterType<T> getNodeType() {
		return (AllowableParameterType<T>) super.getNodeType();
	}

	@Override
	public SqmParameter<T> copy() {
		return new JpaCriteriaParameter<>(
				getName(),
				getAnticipatedType(),
				allowMultiValuedBinding(),
				nodeBuilder()
		);
	}

	@Override
	public AllowableParameterType<T> getHibernateType() {
		return this.getNodeType();
	}

	@Override
	public Class<T> getParameterType() {
		return this.getNodeType().getExpressableJavaTypeDescriptor().getJavaType();
	}

	@Override
	protected void internalApplyInferableType(SqmExpressable<?> newType) {
		super.internalApplyInferableType( newType );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitJpaCriteriaParameter( this );
	}

	public SqmJpaCriteriaParameterWrapper<T> makeSqmParameter() {
		return new SqmJpaCriteriaParameterWrapper<>(
				getHibernateType(),
				this,
				nodeBuilder()
		);
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		throw new UnsupportedOperationException( "ParameterMemento cannot be extracted from Criteria query parameter" );
	}
}
