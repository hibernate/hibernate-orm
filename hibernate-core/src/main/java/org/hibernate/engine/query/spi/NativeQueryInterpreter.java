/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sql.internal.NativeSelectQueryPlanImpl;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterRecognizer;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Factory-owned strategy for dealing with native queries.
 * <p>
 * An implementation may be registered as a Java service. Exactly one
 * implementation may be visible while building a {@code SessionFactory};
 * when none is registered, Hibernate uses its standard implementation.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
@Incubating
@JavaServiceLoadable
public interface NativeQueryInterpreter {
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
		return new NativeSelectQueryPlanImpl<>(
				queryDefinition.getSqlString(),
				queryDefinition.getAffectedTableNames(),
				queryDefinition.getQueryParameterOccurrences(),
				queryDefinition.getResultSetMapping(),
				sessionFactory
		);
	}

}
