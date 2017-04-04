/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.util.Iterator;
import java.util.List;

import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public abstract class AbstractIdMapper implements IdMapper {
	private final ServiceRegistry serviceRegistry;

	public AbstractIdMapper(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	private Parameters getParametersToUse(Parameters parameters, List<QueryParameterData> paramDatas) {
		if ( paramDatas.size() > 1 ) {
			return parameters.addSubParameters( "and" );
		}
		else {
			return parameters;
		}
	}

	@Override
	public void addIdsEqualToQuery(Parameters parameters, String prefix1, String prefix2) {
		final List<QueryParameterData> paramDatas = mapToQueryParametersFromId( null );

		final Parameters parametersToUse = getParametersToUse( parameters, paramDatas );

		for ( QueryParameterData paramData : paramDatas ) {
			parametersToUse.addWhere(
					paramData.getProperty( prefix1 ),
					false,
					"=",
					paramData.getProperty( prefix2 ),
					false
			);
		}
	}

	@Override
	public void addIdsEqualToQuery(Parameters parameters, String prefix1, IdMapper mapper2, String prefix2) {
		final List<QueryParameterData> paramDatas1 = mapToQueryParametersFromId( null );
		final List<QueryParameterData> paramDatas2 = mapper2.mapToQueryParametersFromId( null );

		final Parameters parametersToUse = getParametersToUse( parameters, paramDatas1 );

		final Iterator<QueryParameterData> paramDataIter1 = paramDatas1.iterator();
		final Iterator<QueryParameterData> paramDataIter2 = paramDatas2.iterator();
		while ( paramDataIter1.hasNext() ) {
			final QueryParameterData paramData1 = paramDataIter1.next();
			final QueryParameterData paramData2 = paramDataIter2.next();

			parametersToUse.addWhereOrNullRestriction(
					paramData1.getProperty( prefix1 ),
					false,
					"=",
					paramData2.getProperty( prefix2 ),
					false
			);
		}
	}

	@Override
	public void addIdEqualsToQuery(Parameters parameters, Object id, String alias, String prefix, boolean equals) {
		final List<QueryParameterData> paramDatas = mapToQueryParametersFromId( id );

		final Parameters parametersToUse = getParametersToUse( parameters, paramDatas );

		for ( QueryParameterData paramData : paramDatas ) {
			if ( paramData.getValue() == null ) {
				handleNullValue( parametersToUse, alias, paramData.getProperty( prefix ), equals );
			}
			else if ( alias == null ) {
				parametersToUse.addWhereWithParam(
						paramData.getProperty( prefix ),
						equals ? "=" : "<>",
						paramData.getValue()
				);
			}
			else {
				parametersToUse.addWhereWithParam(
						alias,
						paramData.getProperty( prefix ),
						equals ? "=" : "<>",
						paramData.getValue()
				);
			}
		}
	}

	@Override
	public void addNamedIdEqualsToQuery(Parameters parameters, String prefix, boolean equals) {
		final List<QueryParameterData> paramDatas = mapToQueryParametersFromId( null );

		final Parameters parametersToUse = getParametersToUse( parameters, paramDatas );

		for ( QueryParameterData paramData : paramDatas ) {
			parametersToUse.addWhereWithNamedParam(
					paramData.getProperty( prefix ),
					equals ? "=" : "<>",
					paramData.getQueryParameterName()
			);
		}
	}

	@Override
	public void addNamedIdEqualsToQuery(Parameters parameters, String prefix1, IdMapper mapper, boolean equals) {
		final List<QueryParameterData> paramDatas1 = mapToQueryParametersFromId( null );
		final List<QueryParameterData> paramDatas2 = mapper.mapToQueryParametersFromId( null );

		final Parameters parametersToUse = getParametersToUse( parameters, paramDatas1 );
		final Iterator<QueryParameterData> paramDataIter1 = paramDatas1.iterator();
		final Iterator<QueryParameterData> paramDataIter2 = paramDatas2.iterator();
		while ( paramDataIter1.hasNext() ) {
			final QueryParameterData paramData1 = paramDataIter1.next();
			final QueryParameterData paramData2 = paramDataIter2.next();
			parametersToUse.addWhereWithNamedParam(
					paramData1.getProperty( prefix1 ),
					equals ? "=" : "<>",
					paramData2.getQueryParameterName()
			);
		}
	}

	private void handleNullValue(Parameters parameters, String alias, String propertyName, boolean equals) {
		if ( equals ) {
			parameters.addNullRestriction( alias, propertyName );
		}
		else {
			parameters.addNotNullRestriction( alias, propertyName );
		}
	}
}
