/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * default implementation of the <tt>Query</tt> interface,
 * for "ordinary" HQL queries (not collection filters)
 *
 * @author Gavin King
 * @see CollectionFilterImpl
 */
public class QueryImpl extends AbstractQueryImpl {

	private LockOptions lockOptions = new LockOptions();

	public QueryImpl(
			String queryString,
			FlushMode flushMode,
			SessionImplementor session,
			ParameterMetadata parameterMetadata) {
		super( queryString, flushMode, session, parameterMetadata );
	}

	public QueryImpl(String queryString, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( queryString, null, session, parameterMetadata );
	}

	public Iterator iterate() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		try {
			return getSession().iterate(
					expandParameterLists( namedParams ),
					getQueryParameters( namedParams )
			);
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll() throws HibernateException {
		return scroll( session.getFactory().getDialect().defaultScrollMode() );
	}

	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		QueryParameters qp = getQueryParameters( namedParams );
		qp.setScrollMode( scrollMode );
		try {
			return getSession().scroll( expandParameterLists( namedParams ), qp );
		}
		finally {
			after();
		}
	}

	public List list() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		try {
			return getSession().list(
					expandParameterLists( namedParams ),
					getQueryParameters( namedParams )
			);
		}
		finally {
			after();
		}
	}

	public int executeUpdate() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		try {
			return getSession().executeUpdate(
					expandParameterLists( namedParams ),
					getQueryParameters( namedParams )
			);
		}
		finally {
			after();
		}
	}

	public Query setLockMode(String alias, LockMode lockMode) {
		lockOptions.setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	public Query setLockOptions(LockOptions lockOption) {
		this.lockOptions.setLockMode( lockOption.getLockMode() );
		this.lockOptions.setScope( lockOption.getScope() );
		this.lockOptions.setTimeOut( lockOption.getTimeOut() );
		return this;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public boolean isSelect() {
		return getSession().getFactory().getQueryPlanCache()
				.getHQLQueryPlan( getQueryString(), false, Collections.<String, Filter>emptyMap() )
				.isSelect();
	}
}
