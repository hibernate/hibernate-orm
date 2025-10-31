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
 * QueryParameter impl for positional-parameters in HQL, JPQL or Criteria queries.
 *
 * @author Steve Ebersole
 */
public class QueryParameterPositionalImpl<T> extends AbstractQueryParameter<T> {

	private final int position;

	/**
	 * Create a positional parameter descriptor from the SQM parameter
	 *
	 * @param parameter The source parameter info
	 *
	 * @return The parameter descriptor
	 */
	public static <T> QueryParameterPositionalImpl<T> fromSqm(SqmParameter<T> parameter) {
		assert parameter.getPosition() != null;
		assert parameter.getName() == null;

		return new QueryParameterPositionalImpl<>(
				parameter.getPosition(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType()
		);
	}

	public static <T> QueryParameterPositionalImpl<T> fromNativeQuery(int position) {
		return new QueryParameterPositionalImpl<>(
				position,
				true,
				null
		);
	}

	public QueryParameterPositionalImpl(
			Integer position,
			boolean allowMultiValuedBinding,
			@Nullable BindableType<T> anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.position = position;
	}

	@Override
	public Integer getPosition() {
		return position;
	}

	@Override
	public NamedQueryMemento.ParameterMemento toMemento() {
		return session -> new QueryParameterPositionalImpl<>( getPosition(), allowsMultiValuedBinding(), getHibernateType() );
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof QueryParameterPositionalImpl<?> that) ) {
			return false;
		}
		else {
			return position == that.position;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( position );
	}
}
