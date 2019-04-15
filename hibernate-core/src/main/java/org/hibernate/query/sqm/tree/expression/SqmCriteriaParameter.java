/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.named.spi.ParameterMemento;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

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
		implements SqmParameter<T>, QueryParameterImplementor<T> {
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
	public AllowableParameterType<T> getExpressableType() {
		return (AllowableParameterType<T>) super.getExpressableType();
	}

	@Override
	public AllowableParameterType<T> getAnticipatedType() {
		return getExpressableType();
	}

	@Override
	public AllowableParameterType<T> getHibernateType() {
		return getExpressableType();
	}

	@Override
	public Class<T> getParameterType() {
		return getExpressableType().getJavaType();
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmCriteriaParameter<>(
				name,
				getExpressableType(),
				allowMultiValuedBinding(),
				nodeBuilder()
		);
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		super.internalApplyInferableType( newType );
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCriteriaParameter( this );
	}

	@Override
	public ParameterMemento toMemento() {
		throw new UnsupportedOperationException( "ParameterMemento cannot be extracted from Criteria query parameter" );
	}
}
