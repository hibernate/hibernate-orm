/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryImpl<R> extends AbstractProducedQuery<R> implements Query<R> {
	private final String queryString;

	private final QueryParameterBindingsImpl queryParameterBindings;

	public QueryImpl(
			SharedSessionContractImplementor producer,
			ParameterMetadataImplementor parameterMetadata,
			String queryString) {
		super( producer, parameterMetadata );
		this.queryString = queryString;
		this.queryParameterBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				producer.getFactory(),
				producer.isQueryParametersValidationEnabled()
		);
	}

	@Override
	protected QueryParameterBindings getQueryParameterBindings() {
		return queryParameterBindings;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	public Query<R> setTupleTransformer(TupleTransformer<R> transformer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Query<R> setResultListTransformer(ResultListTransformer transformer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Query<R> setParameterList(String name, Collection values, Class type) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Query<R> setParameterList(int position, Collection values, Class type) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

//	@Override
//	public Type[] getReturnTypes() {
//		return getProducer().getFactory().getReturnTypes( queryString );
//	}
//
//	@Override
//	public String[] getReturnAliases() {
//		return getProducer().getFactory().getReturnAliases( queryString );
//	}
//
//	@Override
//	public Query setEntity(int position, Object val) {
//		return setParameter( position, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
//	}
//
//	@Override
//	public Query setEntity(String name, Object val) {
//		return setParameter( name, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
//	}
}
