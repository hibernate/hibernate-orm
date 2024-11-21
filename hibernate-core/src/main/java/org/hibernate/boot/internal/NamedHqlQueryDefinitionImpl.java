/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.internal;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.spi.AbstractNamedQueryDefinition;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedHqlQueryDefinition {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;
	private final Map<String, String> parameterTypes;

	public NamedHqlQueryDefinitionImpl(
			String name,
			String hqlString,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String,String> parameterTypes,
			Map<String,Object> hints) {
		super(
				name,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public NamedSqmQueryMemento resolve(SessionFactoryImplementor factory) {
		return new NamedHqlQueryMementoImpl(
				getRegistrationName(),
				null,
				hqlString,
				firstResult,
				maxResults,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				parameterTypes,
				getHints()
		);
	}

}
