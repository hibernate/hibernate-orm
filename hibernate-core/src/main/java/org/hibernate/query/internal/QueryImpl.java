/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.ReturnMetadata;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class QueryImpl<R> extends AbstractProducedQuery<R> implements Query<R> {
	private final String queryString;

	private final HQLQueryPlan hqlQueryPlan;

	private final QueryParameterBindingsImpl queryParameterBindings;

	public QueryImpl(
			SharedSessionContractImplementor producer,
			HQLQueryPlan hqlQueryPlan,
			String queryString) {
		super( producer, hqlQueryPlan.getParameterMetadata() );
		this.hqlQueryPlan = hqlQueryPlan;
		this.queryString = queryString;
		this.queryParameterBindings = QueryParameterBindingsImpl.from(
				hqlQueryPlan.getParameterMetadata(),
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

	public HQLQueryPlan getQueryPlan() {
		return hqlQueryPlan;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public Type[] getReturnTypes() {
		final ReturnMetadata metadata = hqlQueryPlan.getReturnMetadata();
		return metadata == null ? null : metadata.getReturnTypes();
	}

	@Override
	public String[] getReturnAliases() {
		final ReturnMetadata metadata = hqlQueryPlan.getReturnMetadata();
		return metadata == null ? null : metadata.getReturnAliases();
	}

	@Override
	public Query setEntity(int position, Object val) {
		return setParameter( position, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
	}

	@Override
	public Query setEntity(String name, Object val) {
		return setParameter( name, val, getProducer().getFactory().getTypeHelper().entity( resolveEntityName( val ) ) );
	}

	@Override
	protected boolean isSelect() {
		return hqlQueryPlan.isSelect();
	}

	@Override
	protected void appendQueryPlanToQueryParameters(
			String hql,
			QueryParameters queryParameters,
			HQLQueryPlan queryPlan) {
		if ( queryPlan != null ) {
			queryParameters.setQueryPlan( queryPlan );
		}
		else if ( hql.equals( getQueryString() )
				&& getQueryPlan().getEnabledFilterNames()
						.equals( getProducer().getLoadQueryInfluencers().getEnabledFilters().values() ) ) {
			queryParameters.setQueryPlan( getQueryPlan() );
		}
	}
}
