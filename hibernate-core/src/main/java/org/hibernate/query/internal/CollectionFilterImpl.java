/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.type.Type;

/**
 * implementation of the <tt>Query</tt> interface for collection filters
 *
 * @author Gavin King
 */
public class CollectionFilterImpl extends org.hibernate.query.internal.AbstractProducedQuery {
	private final String queryString;
	private Object collection;

	public CollectionFilterImpl(
			String queryString,
			Object collection,
			SharedSessionContractImplementor session,
			ParameterMetadataImpl parameterMetadata) {
		super( session, parameterMetadata );
		this.queryString = queryString;
		this.collection = collection;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public Type[] getReturnTypes() {
		return getProducer().getFactory().getReturnTypes( getQueryString() );
	}

	/**
	 * @see org.hibernate.Query#scroll()
	 */
	public ScrollableResults scroll() throws HibernateException {
		throw new UnsupportedOperationException( "Can't scroll filters" );
	}

	@Override
	public Query setEntity(int position, Object val) {
		return setParameter( position, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
	}

	@Override
	public Query setEntity(String name, Object val) {
		return setParameter( name, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
	}

}
