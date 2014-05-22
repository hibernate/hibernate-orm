/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.query.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.query.spi.ParamLocationRecognizer;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.sql.SQLCustomQuery;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterStandardImpl implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterStandardImpl INSTANCE = new NativeQueryInterpreterStandardImpl();

	@Override
	public ParameterMetadata getParameterMetadata(String nativeQuery) {
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

		return new ParameterMetadata( ordinalDescriptors, namedParamDescriptorMap );
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
