/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.type.Type;

/**
 * implementation of the <tt>Query</tt> interface for collection filters
 *
 * @author Gavin King
 */
public class CollectionFilterImpl extends org.hibernate.query.internal.AbstractProducedQuery {
	private final String queryString;
	private Object collection;
	private final QueryParameterBindingsImpl queryParameterBindings;

	public CollectionFilterImpl(
			String queryString,
			Object collection,
			SharedSessionContractImplementor session,
			ParameterMetadataImpl parameterMetadata) {
		super( session, parameterMetadata );
		this.queryString = queryString;
		this.collection = collection;
		this.queryParameterBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				session.getFactory(),
				session.isQueryParametersValidationEnabled()
		);
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		return queryParameterBindings;
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
	public Iterator iterate() throws HibernateException {
		getQueryParameterBindings().verifyParametersBound( false );

		final String expandedQuery = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getProducer() );
		return getProducer().iterateFilter(
				collection,
				expandedQuery,
				makeQueryParametersForExecution( expandedQuery )
		);
	}

	@Override
	public List list() throws HibernateException {
		getQueryParameterBindings().verifyParametersBound( false );

		final String expandedQuery = getQueryParameterBindings().expandListValuedParameters( getQueryString(), getProducer() );
		return getProducer().listFilter(
				collection,
				expandedQuery,
				makeQueryParametersForExecution( expandedQuery )
		);
	}

	@Override
	public ScrollableResultsImplementor scroll() throws HibernateException {
		throw new UnsupportedOperationException( "Can't scroll filters" );
	}

	@Override
	public ScrollableResultsImplementor scroll(ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "Can't scroll filters" );
	}

	@Override
	protected Type[] getPositionalParameterTypes() {
		final Type[] explicitParameterTypes = super.getPositionalParameterTypes();
		final Type[] expandedParameterTypes = new Type[ explicitParameterTypes.length + 1 ];

		// previously this logic would only add an additional slot in the array, not fill it.  carry that logic here, for now
		System.arraycopy( explicitParameterTypes, 0, expandedParameterTypes, 1, explicitParameterTypes.length );

		return expandedParameterTypes;
	}

	@SuppressWarnings("deprecation")
	protected Object[] getPositionalParameterValues() {
		final Object[] explicitParameterValues = super.getPositionalParameterValues();
		final Object[] expandedParameterValues = new Object[ explicitParameterValues.length + 1 ];

		// previously this logic would only add an additional slot in the array, not fill it.  carry that logic here, for now
		System.arraycopy( explicitParameterValues, 0, expandedParameterValues, 1, explicitParameterValues.length );

		return expandedParameterValues;
	}

	@Override
	public Type[] getReturnTypes() {
		return getProducer().getFactory().getReturnTypes( getQueryString() );
	}

	@Override
	public String[] getReturnAliases() {
		return getProducer().getFactory().getReturnAliases( getQueryString() );
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
