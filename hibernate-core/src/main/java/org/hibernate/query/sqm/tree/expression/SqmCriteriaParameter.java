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
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * SqmParameter created via JPA {@link javax.persistence.criteria.CriteriaBuilder}
 *
 * @see ParameterMetadata
 * @see NodeBuilder#parameter
 *
 * @author Steve Ebersole
 */
public class SqmCriteriaParameter<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T>, QueryParameterImplementor<T>, DomainResultProducer<T> {
	private final String name;
	private boolean allowMultiValuedBinding;

	public SqmCriteriaParameter(
			String name,
			AllowableParameterType<T> type,
			boolean allowMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		this.name = name;
		this.allowMultiValuedBinding = allowMultiValuedBinding;
	}
	public SqmCriteriaParameter(
			AllowableParameterType<T> type,
			boolean allowMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		this( null, type, allowMultiValuedBinding, nodeBuilder );
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
	public boolean allowMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public void disallowMultiValuedBinding() {
		allowMultiValuedBinding = false;
	}

	@Override
	public void applyAnticipatedType(AllowableParameterType type) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public AllowableParameterType<T> getNodeType() {
		return (AllowableParameterType<T>) super.getNodeType();
	}

	@Override
	public AllowableParameterType<T> getAnticipatedType() {
		return this.getNodeType();
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
	public SqmParameter<T> copy() {
		return new SqmCriteriaParameter<>(
				name,
				this.getNodeType(),
				allowMultiValuedBinding(),
				nodeBuilder()
		);
	}

	@Override
	protected void internalApplyInferableType(SqmExpressable<?> newType) {
		super.internalApplyInferableType( newType );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCriteriaParameter( this );
	}

	@Override
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		throw new UnsupportedOperationException( "ParameterMemento cannot be extracted from Criteria query parameter" );
	}
}
