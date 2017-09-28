/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.internal;

import java.util.Collection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.spi.AbstractNamedQueryDescriptor;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;
import org.hibernate.query.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDescriptorImpl extends AbstractNamedQueryDescriptor implements NamedHqlQueryDescriptor {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedHqlQueryDescriptorImpl(
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
			String comment) {
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
				comment
		);
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public HqlQueryImplementor toQuery(SharedSessionContractImplementor session) {
		final QuerySqmImpl query = new QuerySqmImpl(
				hqlString,
				session.getFactory().getQueryEngine().getSemanticQueryProducer().interpret( hqlString ),
				null,
				session
		);

		if ( firstResult != null ) {
			query.setFirstResult( firstResult );
		}
		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}

		applyBaseOptions( query, session );

		return query;
	}

	@Override
	public NamedHqlQueryDescriptor makeCopy(String name) {
		return new NamedHqlQueryDescriptorImpl(
				name,
				getHqlString(),
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
				getComment()
		);
	}
}
