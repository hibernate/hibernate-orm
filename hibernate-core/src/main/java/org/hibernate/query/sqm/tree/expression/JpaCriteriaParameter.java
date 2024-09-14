/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.Objects;

import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.BindableType;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

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
		super( toSqmType( type, nodeBuilder ), nodeBuilder );
		this.name = name;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}

	protected JpaCriteriaParameter(JpaCriteriaParameter<T> original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.name = original.name;
		this.allowsMultiValuedBinding = original.allowsMultiValuedBinding;
	}

	private static <T> SqmExpressible<T> toSqmType(BindableType<T> type, NodeBuilder nodeBuilder) {
		if ( type == null ) {
			return null;
		}
		return type.resolveExpressible( nodeBuilder );
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
	public BindableType<T> getHibernateType() {
		return getNodeType();
	}

	@Override
	public Class<T> getParameterType() {
		final SqmExpressible<T> nodeType = getNodeType();
		return nodeType == null ? null : nodeType.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	protected void internalApplyInferableType(SqmExpressible<?> newType) {
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
		sb.append( ':' );
		sb.append( getName() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JpaCriteriaParameter<?> parameter = (JpaCriteriaParameter<?>) o;
		return Objects.equals( name, parameter.name );
	}

	@Override
	public int hashCode() {
		if ( name == null ) {
			return super.hashCode();
		}
		return Objects.hash( name );
	}

	@Override
	public int compareTo(SqmParameter anotherParameter) {
		return anotherParameter instanceof JpaCriteriaParameter
				? Integer.compare( hashCode(), anotherParameter.hashCode() )
				: 1;
	}
}
