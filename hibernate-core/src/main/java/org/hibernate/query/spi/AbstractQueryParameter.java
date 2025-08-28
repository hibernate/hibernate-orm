/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;


import org.hibernate.Internal;
import org.hibernate.type.BindableType;

import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;

/**
 * Base implementation of {@link org.hibernate.query.QueryParameter}.
 *
 * @apiNote This class is now considered internal implementation
 * and will move to an internal package in a future version.
 * Application programs should never depend directly on this class.
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractQueryParameter<T> implements QueryParameterImplementor<T> {

	private boolean allowMultiValuedBinding;
	private BindableType<T> anticipatedType;

	public AbstractQueryParameter(boolean allowMultiValuedBinding, BindableType<T> anticipatedType) {
		this.allowMultiValuedBinding = allowMultiValuedBinding;
		this.anticipatedType = anticipatedType;
	}

	@Override
	public void disallowMultiValuedBinding() {
		QUERY_MESSAGE_LOGGER.debugf( "QueryParameter#disallowMultiValuedBinding() called: %s", this );
		this.allowMultiValuedBinding = true;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowMultiValuedBinding;
	}

	@Override
	public BindableType<T> getHibernateType() {
		return anticipatedType;
	}

	@Override
	public void applyAnticipatedType(BindableType<?> type) {
		//noinspection unchecked
		this.anticipatedType = (BindableType<T>) type;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public Class<T> getParameterType() {
		return anticipatedType == null ? null : anticipatedType.getJavaType();
	}
}
