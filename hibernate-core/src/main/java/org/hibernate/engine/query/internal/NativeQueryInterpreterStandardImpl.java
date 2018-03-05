/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.internal;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
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
	private final SessionFactoryImplementor sessionFactory;

	public NativeQueryInterpreterStandardImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public ParameterMetadataImpl getParameterMetadata(String nativeQuery) {
		final ParamLocationRecognizer recognizer = ParamLocationRecognizer.parseLocations( nativeQuery, sessionFactory );
		return new ParameterMetadataImpl(
				recognizer.getOrdinalParameterDescriptionMap(),
				recognizer.getNamedParameterDescriptionMap()
		);
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
