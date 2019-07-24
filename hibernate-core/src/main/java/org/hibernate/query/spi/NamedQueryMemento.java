/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Named Query mementos are stored in the QueryEngine's
 * {@link NamedQueryRepository}.  This is the base contract
 * for all specific types of named query mementos
 *
 * @author Steve Ebersole
 */
public interface NamedQueryMemento {
	/**
	 * The name under which the query is registered
	 */
	String getRegistrationName();

	/**
	 * Makes a copy of the memento
	 */
	NamedQueryMemento makeCopy(String name);

	Boolean getCacheable();

	String getCacheRegion();

	CacheMode getCacheMode();

	FlushMode getFlushMode();

	Boolean getReadOnly();

	Integer getTimeout();

	Integer getFetchSize();

	String getComment();

	Map<String, Object> getHints();

	void validate(QueryEngine queryEngine);

	interface ParameterMemento {
		QueryParameterImplementor resolve(SharedSessionContractImplementor session);
	}
}
