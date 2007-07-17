//$Id: QueryImpl.java 8524 2005-11-04 21:28:49Z steveebersole $
package org.hibernate.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.ParameterMetadata;

/**
 * default implementation of the <tt>Query</tt> interface,
 * for "ordinary" HQL queries (not collection filters)
 * @see CollectionFilterImpl
 * @author Gavin King
 */
public class QueryImpl extends AbstractQueryImpl {

	private Map lockModes = new HashMap(2);

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
					expandParameterLists(namedParams),
			        getQueryParameters(namedParams)
				);
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll() throws HibernateException {
		return scroll( ScrollMode.SCROLL_INSENSITIVE );
	}

	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();
		QueryParameters qp = getQueryParameters(namedParams);
		qp.setScrollMode(scrollMode);
		try {
			return getSession().scroll( expandParameterLists(namedParams), qp );
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
					expandParameterLists(namedParams),
			        getQueryParameters(namedParams)
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
		lockModes.put(alias, lockMode);
		return this;
	}

	protected Map getLockModes() {
		return lockModes;
	}

}






