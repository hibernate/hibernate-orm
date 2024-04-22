/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.sqm.spi;

import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.tree.SqmStatement;

public interface NamedSqmQueryMemento<E> extends NamedQueryMemento<E> {
	<T> SqmQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType);

	/**
	 * Convert the memento into an untyped executable query
	 */
	SqmQueryImplementor<E> toQuery(SharedSessionContractImplementor session);

	<T> SqmSelectionQuery<T> toSelectionQuery(Class<T> resultType, SharedSessionContractImplementor session);

	String getHqlString();

	SqmStatement<E> getSqmStatement();

	Integer getFirstResult();

	Integer getMaxResults();

	LockOptions getLockOptions();

	Map<String, String> getParameterTypes();

	@Override
	NamedSqmQueryMemento<E> makeCopy(String name);

}
