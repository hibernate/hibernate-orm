/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NativeSelectQueryPlanImpl;
import org.hibernate.query.sql.internal.ParameterParser;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.service.Service;

/**
 * Service contract for dealing with native queries.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
@Incubating
public interface NativeQueryInterpreter extends Service {
	/**
	 * Parse the given native query and inform the recognizer of all
	 * recognized parameter markers.
	 *
	 * @param nativeQuery The query to recognize parameters in
	 * @param recognizer The recognizer to call
	 */
	void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer, char namedParamPrefix, char ordinalParamPrefix);

	/**
	 * @deprecated use {@link #recognizeParameters(String, ParameterRecognizer, char, char)}
	 */
	@Deprecated
	default void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer) {
		recognizeParameters( nativeQuery, recognizer, ParameterParser.NAMED_PARAM_PREFIX, ParameterParser.ORDINAL_PARAM_PREFIX );
	}

	/**
	 * Creates a new query plan for the passed native query definition
	 */
	default <R> NativeSelectQueryPlan<R> createQueryPlan(
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
