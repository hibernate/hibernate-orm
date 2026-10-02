package org.hibernate.query.internal;

import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmJpaCriteriaParameterWrapper;

/**
 * QueryParameter impl for named-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class QueryParameterNamedImpl<T> extends AbstractQueryParameter<T> {
	/**
	 * Create a named parameter descriptor from the SQM parameter
	 *
	 * @param parameter The source parameter info
	 *
	 * @return The parameter descriptor
	 */
	public static <T> QueryParameterNamedImpl<T> fromSqm(SqmParameter<T> parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;
		return new QueryParameterNamedImpl<>(
				parameter.getName(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType(),
				parameter instanceof SqmJpaCriteriaParameterWrapper<T> wrapper
						? wrapper.getJpaCriteriaParameter().getJavaTypeIfKnown() : null
		);
	}

	public static <T> QueryParameterNamedImpl<T> fromNativeQuery(String name) {
		return new QueryParameterNamedImpl<>( name, true, null );
	}

	private final String name;

	private QueryParameterNamedImpl(String name, boolean allowMultiValuedBinding, @Nullable BindableType<T> anticipatedType) {
		this( name, allowMultiValuedBinding, anticipatedType, null );
	}

	private QueryParameterNamedImpl(
			String name,
			boolean allowMultiValuedBinding,
			@Nullable BindableType<T> anticipatedType,
			@Nullable Class<T> declaredJavaType) {
		super( allowMultiValuedBinding, anticipatedType, declaredJavaType );
		this.name = name;
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	@Override
	@Nonnull
	public NamedQueryMemento.ParameterMemento toMemento() {
		return session -> new QueryParameterNamedImpl<>(
				getName(), allowsMultiValuedBinding(), getHibernateType(), declaredJavaType );
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof QueryParameterNamedImpl<?> that) ) {
			return false;
		}
		else {
			return Objects.equals( name, that.name );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}
}
