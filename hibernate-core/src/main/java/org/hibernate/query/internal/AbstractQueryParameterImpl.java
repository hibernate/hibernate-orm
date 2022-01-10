/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.query.BindableType;
import org.hibernate.query.QueryParameter;

/**
 * QueryParameter implementation.
 *
 * NOTE: Unfortunately we need to model named and positional parameters separately still until 6.0.  For now
 * this is simply the base abstract class for those specific impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractQueryParameterImpl<T> implements QueryParameter<T> {
	private BindableType<T> expectedType;

	public AbstractQueryParameterImpl(BindableType<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public BindableType<T> getHibernateType() {
		return expectedType;
	}

	public void setHibernateType(BindableType<?> expectedType) {
		//noinspection unchecked
		this.expectedType = (BindableType<T>) expectedType;
	}

	@Override
	public Class<T> getParameterType() {
		if ( expectedType == null ) {
			return null;
		}
		return expectedType.getBindableJavaType();
	}
}
