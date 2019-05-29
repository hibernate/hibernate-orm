/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.spi.AbstractNamedQueryMemento;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.type.BasicType;

/**
 * Definition of a named query, defined in the mapping metadata.
 *
 * Additionally, as of JPA 2.1, named query definition can also come
 * from a compiled query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NamedHqlQueryMementoImpl extends AbstractNamedQueryMemento implements NamedHqlQueryMemento, Serializable {
	private final String hqlString;

	private final Integer firstResult;
	private final Integer maxResults;

	private final LockOptions lockOptions;
	private final Map<String, String> parameterTypes;

	public NamedHqlQueryMementoImpl(
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
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
		this.lockOptions = lockOptions;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public NamedHqlQueryMemento makeCopy(String name) {
		return new NamedHqlQueryMementoImpl(
				name,
				hqlString,
				firstResult,
				maxResults,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				parameterTypes,
				hints
		);
	}

	@Override
	public <T> HqlQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		final QuerySqmImpl<T> query = new QuerySqmImpl<>(
				hqlString,
				session.getFactory().getQueryEngine().getSemanticQueryProducer().interpret( hqlString ),
				resultType,
				session
		);

		for ( Map.Entry<String, String> entry : parameterTypes.entrySet() ) {
			final BasicType hintedType = session.getFactory()
					.getMetamodel()
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.getRegisteredType( entry.getValue() );
			final QueryParameterImplementor<Object> queryParameter = query.getParameterMetadata().getQueryParameter( entry.getKey() );
			queryParameter.applyAnticipatedType( hintedType );
		}


		if ( firstResult != null ) {
			query.setFirstResult( firstResult );
		}
		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}

		applyBaseOptions( query, session );

		return query;
	}
}
