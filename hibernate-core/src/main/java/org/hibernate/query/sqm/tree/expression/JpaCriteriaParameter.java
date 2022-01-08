/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * {@link JpaParameterExpression} created via JPA {@link jakarta.persistence.criteria.CriteriaBuilder}.
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
	private final T value;
	private boolean allowsMultiValuedBinding;

	public JpaCriteriaParameter(
			AllowableParameterType<T> type,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		this( null, type, allowsMultiValuedBinding, nodeBuilder );
	}

	public JpaCriteriaParameter(
			String name,
			AllowableParameterType<T> type,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		super( toSqmType( type, nodeBuilder ), nodeBuilder );
		this.name = name;
		this.value = null;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}

	public JpaCriteriaParameter(
			String name,
			AllowableParameterType<T> type,
			T value,
			boolean allowsMultiValuedBinding,
			NodeBuilder nodeBuilder) {
		super( toSqmType( type, nodeBuilder ), nodeBuilder );
		this.name = name;
		this.value = value;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}

	private static <T> SqmExpressable<T> toSqmType(AllowableParameterType<T> type, NodeBuilder nodeBuilder) {
		if ( type == null ) {
			return null;
		}
		return type.resolveExpressable(
				nodeBuilder.getQueryEngine().getTypeConfiguration().getSessionFactory()
		);
	}

	public JpaCriteriaParameter(AllowableParameterType<T> type, T value, NodeBuilder nodeBuilder) {
		super( toSqmType( type, nodeBuilder ), nodeBuilder );
		this.name = null;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	public T getValue() {
		return value;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void applyAnticipatedType(AllowableParameterType type) {
		super.internalApplyInferableType( toSqmType( type, nodeBuilder() ) );
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
		return this.getNodeType().getExpressableJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	protected void internalApplyInferableType(SqmExpressable<?> newType) {
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
	public void appendHqlString(StringBuilder sb) {
		if ( getName() == null ) {
			sb.append( value );
		}
		else {
			sb.append( ':' );
			sb.append( getName() );
		}
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
