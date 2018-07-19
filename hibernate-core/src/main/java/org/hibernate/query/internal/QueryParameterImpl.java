/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * QueryParameter implementation.
 *
 * NOTE: Unfortunately we need to model named and positional parameters separately still until 6.0.  For now
 * this is simply the base abstract class for those specific impls
 *
 * @author Steve Ebersole
 */
public abstract class QueryParameterImpl<T> implements QueryParameter<T> {
	private Type expectedType;

	public QueryParameterImpl(Type expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public Type getHibernateType() {
		return expectedType;
	}

	public void setHibernateType(Type expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public Class<T> getParameterType() {
		return expectedType == null ? null : expectedType.getReturnedClass();
	}
}
