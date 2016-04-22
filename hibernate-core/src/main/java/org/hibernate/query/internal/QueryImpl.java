/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryImpl<R> extends AbstractProducedQuery<R> implements Query<R> {
	private final String queryString;

	public QueryImpl(
			SharedSessionContractImplementor producer,
			ParameterMetadata parameterMetadata,
			String queryString) {
		super( producer, parameterMetadata );
		this.queryString = queryString;
	}

	@Override
	public String getQueryString() {
		return queryString;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public Type[] getReturnTypes() {
		return getProducer().getFactory().getReturnTypes( queryString );
	}

	@Override
	public String[] getReturnAliases() {
		return getProducer().getFactory().getReturnAliases( queryString );
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
