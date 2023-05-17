/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.internal;

import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NativeSelectQueryPlanImpl;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterStandardImpl implements NativeQueryInterpreter {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterStandardImpl NATIVE_QUERY_INTERPRETER = new NativeQueryInterpreterStandardImpl();

	@Override
	public void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer, char namedParamPrefix, char ordinalParamPrefix) {
		ParameterParser.parse( nativeQuery, recognizer, namedParamPrefix, ordinalParamPrefix );
	}

	@Override
	public <R> NativeSelectQueryPlan<R> createQueryPlan(
			NativeSelectQueryDefinition<R> queryDefinition,
			SessionFactoryImplementor sessionFactory) {
		return new NativeSelectQueryPlanImpl<>(
				queryDefinition.getSqlString(),
				queryDefinition.getAffectedTableNames(),
				queryDefinition.getQueryParameterOccurrences(),
				queryDefinition.getResultSetMapping(),
				sessionFactory
		);
	}
}
