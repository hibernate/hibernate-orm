/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.query.BindableType;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.AbstractQueryParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;

import org.checkerframework.checker.nullness.qual.Nullable;


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

	private QueryParameterIdentifiedImpl(int unnamedParameterId, boolean allowMultiValuedBinding, @Nullable BindableType<T> anticipatedType) {
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
			|| o instanceof QueryParameterIdentifiedImpl<?>
				&& unnamedParameterId == ( (QueryParameterIdentifiedImpl<?>) o ).unnamedParameterId;
	}

	@Override
	public int hashCode() {
		return unnamedParameterId;
	}
}
