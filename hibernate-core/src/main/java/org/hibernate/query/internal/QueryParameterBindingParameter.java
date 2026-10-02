package org.hibernate.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Parameter;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.type.BindableType;
import org.hibernate.type.NullType;

/**
 * Exposes the type of a binding without modifying parameter metadata shared by cached queries.
 */
final class QueryParameterBindingParameter<T> implements QueryParameterImplementor<T> {
	private final QueryParameterImplementor<T> parameter;
	private final QueryParameterBinding<T> binding;

	QueryParameterBindingParameter(QueryParameterImplementor<T> parameter, QueryParameterBinding<T> binding) {
		this.parameter = parameter;
		this.binding = binding;
	}

	static <T> QueryParameterImplementor<T> unwrap(QueryParameterImplementor<T> parameter) {
		return parameter instanceof QueryParameterBindingParameter<T> wrapper ? wrapper.parameter : parameter;
	}

	static <T> @Nullable Class<T> getParameterTypeIfKnown(Parameter<T> parameter) {
		if ( parameter instanceof AbstractQueryParameter<T> queryParameter ) {
			return queryParameter.getParameterTypeIfKnown();
		}
		if ( parameter instanceof JpaCriteriaParameter<T> criteriaParameter ) {
			return criteriaParameter.getJavaTypeIfKnown();
		}
		try {
			return parameter.getParameterType();
		}
		catch (IllegalStateException ignored) {
			return null;
		}
	}

	@Override
	public @Nonnull Class<T> getParameterType() {
		final var javaType = getParameterTypeIfKnown( parameter );
		if ( javaType != null ) {
			return javaType;
		}
		final var bindType = binding.getBindType();
		// A JDBC null binding does not establish a Java parameter type.
		if ( bindType != null && !(bindType instanceof NullType) && bindType.getJavaType() != null ) {
			return bindType.getJavaType();
		}
		return parameter.getParameterType(); // throws when the type is still unknown
	}

	@Override
	public @Nullable String getName() {
		return parameter.getName();
	}

	@Override
	public @Nullable Integer getPosition() {
		return parameter.getPosition();
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return parameter.allowsMultiValuedBinding();
	}

	@Override
	public @Nullable BindableType<T> getHibernateType() {
		return parameter.getHibernateType();
	}

	@Override
	public void disallowMultiValuedBinding() {
		parameter.disallowMultiValuedBinding();
	}

	@Override
	public void applyAnticipatedType(@Nullable BindableType<?> type) {
		parameter.applyAnticipatedType( type );
	}

	@Override
	public @Nonnull NamedQueryMemento.ParameterMemento toMemento() {
		return parameter.toMemento();
	}
}
