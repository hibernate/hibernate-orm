/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.Objects;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.AbstractQueryParameter;
import org.hibernate.query.spi.NamedQueryMemento;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

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
	public static <T> QueryParameterNamedImpl<T> fromSqm(SqmParameter parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;

		return new QueryParameterNamedImpl<>(
				parameter.getName(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType()
		);
	}

	public static <T> QueryParameterNamedImpl<T> fromNativeQuery(String name) {
		return new QueryParameterNamedImpl<T>(
				name,
				false,
				null
		);
	}

	private final String name;

	private QueryParameterNamedImpl(
			String name,
			boolean allowMultiValuedBinding,
			AllowableParameterType anticipatedType) {
		//noinspection unchecked
		super( allowMultiValuedBinding, anticipatedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public NamedQueryMemento.ParameterMemento toMemento() {
		return session -> new QueryParameterNamedImpl( getName(), allowsMultiValuedBinding(), getHibernateType() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		QueryParameterNamedImpl<?> that = (QueryParameterNamedImpl<?>) o;
		return Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}
}
