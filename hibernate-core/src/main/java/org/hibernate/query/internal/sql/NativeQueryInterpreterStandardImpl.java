/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.NativeQueryInterpreter;
import org.hibernate.query.spi.ParameterRecognizer;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterStandardImpl implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterStandardImpl INSTANCE = new NativeQueryInterpreterStandardImpl();

	@Override
	public void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer) {
		ParameterParser.parse( nativeQuery, recognizer );
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
