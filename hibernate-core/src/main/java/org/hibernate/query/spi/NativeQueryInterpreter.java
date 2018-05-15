/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NativeSelectQueryPlanImpl;
import org.hibernate.query.sql.spi.NativeNonSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeNonSelectQueryPlan;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.service.Service;
import org.hibernate.NotYetImplementedFor6Exception;

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
	 * Creates a new query plan for the passed native query definition
	 */
	default <R> NativeSelectQueryPlan<R> createQueryPlan(
			NativeSelectQueryDefinition<R> queryDefinition,
			SessionFactoryImplementor sessionFactory) {
		return new NativeSelectQueryPlanImpl<R>(
				queryDefinition.getSqlString(),
				queryDefinition.getAffectedTableNames(),
				queryDefinition.getQueryParameterList(),
				queryDefinition.getResultSetMapping(),
				queryDefinition.getRowTransformer()
		);
	}

	/**
	 * Creates a new query plan for the passed native query values
	 */
	default NativeNonSelectQueryPlan createQueryPlan(
			NativeNonSelectQueryDefinition queryDefinition,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
