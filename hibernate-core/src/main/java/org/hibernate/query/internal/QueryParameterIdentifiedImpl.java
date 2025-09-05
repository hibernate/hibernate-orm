/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.type.BindableType;


/**
 * QueryParameter impl for unnamed JPA Criteria-parameters.
 */
public class QueryParameterIdentifiedImpl<T> extends AbstractQueryParameter<T> {
	/**
	 * Create an identified parameter descriptor from the SQM parameter
	 *
	 * @param parameter The source parameter info
	 *
	 * @return The parameter descriptor
	 */
	public static <T> QueryParameterIdentifiedImpl<T> fromSqm(SqmJpaCriteriaParameterWrapper<T> parameter) {
		assert parameter.getName() == null;
		assert parameter.getPosition() == null;
		return new QueryParameterIdentifiedImpl<>(
				parameter.getUnnamedParameterId(),
				parameter.allowMultiValuedBinding(),
				parameter.getAnticipatedType()
		);
	}

	private final int unnamedParameterId;

	private QueryParameterIdentifiedImpl(int unnamedParameterId, boolean allowMultiValuedBinding, BindableType<T> anticipatedType) {
		super( allowMultiValuedBinding, anticipatedType );
		this.unnamedParameterId = unnamedParameterId;
	}

	public int getUnnamedParameterId() {
		return unnamedParameterId;
	}

	@Override
	public NamedQueryMemento.ParameterMemento toMemento() {
		return session -> new QueryParameterIdentifiedImpl<>( unnamedParameterId, allowsMultiValuedBinding(), getHibernateType() );
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			|| o instanceof QueryParameterIdentifiedImpl<?> that
				&& unnamedParameterId == that.unnamedParameterId;
	}

	@Override
	public int hashCode() {
		return unnamedParameterId;
	}
}
