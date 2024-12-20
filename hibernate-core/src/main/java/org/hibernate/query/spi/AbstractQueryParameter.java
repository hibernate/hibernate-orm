/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.query.BindableType;
import org.hibernate.query.QueryLogging;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQueryParameter<T> implements QueryParameterImplementor<T> {
	private boolean allowMultiValuedBinding;
	private BindableType<T> anticipatedType;

	public AbstractQueryParameter(
			boolean allowMultiValuedBinding,
			BindableType<T> anticipatedType) {
		this.allowMultiValuedBinding = allowMultiValuedBinding;
		this.anticipatedType = anticipatedType;
	}

	@Override
	public void disallowMultiValuedBinding() {
		QueryLogging.QUERY_MESSAGE_LOGGER.debugf( "QueryParameter#disallowMultiValuedBinding() called : %s", this );
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
	public void applyAnticipatedType(BindableType type) {
		//noinspection unchecked
		this.anticipatedType = type;
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
		return anticipatedType == null ? null : anticipatedType.getBindableJavaType();
	}
}
