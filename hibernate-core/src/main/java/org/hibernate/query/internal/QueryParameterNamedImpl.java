/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.named.NamedQueryMemento;
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
	public static <T> QueryParameterNamedImpl<T> fromSqm(SqmParameter<T> parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;
		return new QueryParameterNamedImpl<>(
				parameter.getName(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType()
		);
	}

	public static <T> QueryParameterNamedImpl<T> fromNativeQuery(String name) {
		return new QueryParameterNamedImpl<>( name, true, null );
	}

	private final String name;

	private QueryParameterNamedImpl(String name, boolean allowMultiValuedBinding, @Nullable BindableType<T> anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public NamedQueryMemento.ParameterMemento toMemento() {
		return session -> new QueryParameterNamedImpl<>( getName(), allowsMultiValuedBinding(), getHibernateType() );
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
