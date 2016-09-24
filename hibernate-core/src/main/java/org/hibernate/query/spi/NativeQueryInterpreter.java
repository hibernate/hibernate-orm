/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

/**
 * Service contract for dealing with native queries.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
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
	void recognizeParameters(String nativeQuery, ParameterRecognizer recognizer);

	/**
	 * Creates a new query plan for the specified native query.
	 *
	 * @param specification Describes the query to create a plan for
	 * @param sessionFactory The current session factory
	 *
	 * @return A query plan for the specified native query.
	 */
	NativeSQLQueryPlan createQueryPlan(
			NativeSQLQuerySpecification specification,
			SessionFactoryImplementor sessionFactory);
}
