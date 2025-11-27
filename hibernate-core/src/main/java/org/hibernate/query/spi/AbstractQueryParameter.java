/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.query.BindableType;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.spi.QueryParameterImplementor;

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
		this.allowMultiValuedBinding = false;
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
