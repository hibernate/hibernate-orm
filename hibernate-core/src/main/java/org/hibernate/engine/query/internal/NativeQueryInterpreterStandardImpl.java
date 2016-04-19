/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.sql.SQLCustomQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterStandardImpl implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterStandardImpl INSTANCE = new NativeQueryInterpreterStandardImpl();

	@Override
	public ParameterMetadataImpl getParameterMetadata(String nativeQuery) {
		final ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( nativeQuery );

		final int size = recognizer.getOrdinalParameterLocationList().size();
		final OrdinalParameterDescriptor[] ordinalDescriptors = new OrdinalParameterDescriptor[ size ];
		for ( int i = 0; i < size; i++ ) {
			final Integer position = recognizer.getOrdinalParameterLocationList().get( i );
			ordinalDescriptors[i] = new OrdinalParameterDescriptor( i, null, position );
		}

		final Map<String, NamedParameterDescriptor> namedParamDescriptorMap = new HashMap<String, NamedParameterDescriptor>();
		final Map<String, ParamLocationRecognizer.NamedParameterDescription> map = recognizer.getNamedParameterDescriptionMap();

		for ( final String name : map.keySet() ) {
			final ParamLocationRecognizer.NamedParameterDescription description = map.get( name );
			namedParamDescriptorMap.put(
					name,
					new NamedParameterDescriptor(
							name,
							null,
							description.buildPositionsArray(),
							description.isJpaStyle()
					)
			);
		}

		return new ParameterMetadataImpl( ordinalDescriptors, namedParamDescriptorMap );
	}

	@Override
	public NativeSQLQueryPlan createQueryPlan(
			NativeSQLQuerySpecification specification,
			SessionFactoryImplementor sessionFactory) {

		CustomQuery customQuery = new SQLCustomQuery(
				specification.getQueryString(),
				specification.getQueryReturns(),
				specification.getQuerySpaces(),
				sessionFactory
		);

		return new NativeSQLQueryPlan( specification.getQueryString(), customQuery );
	}
}
