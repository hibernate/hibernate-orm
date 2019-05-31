/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
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
	private AllowableParameterType<T> expectedType;

	public AbstractQueryParameterImpl(AllowableParameterType<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public AllowableParameterType<T> getHibernateType() {
		return expectedType;
	}

	public void setHibernateType(AllowableParameterType<?> expectedType) {
		//noinspection unchecked
		this.expectedType = (AllowableParameterType) expectedType;
	}

	@Override
	public Class<T> getParameterType() {
		return expectedType == null ? null : expectedType.getJavaType();
	}
}
