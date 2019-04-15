/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * Main entry point into building semantic queries.
 *
 * @see SessionFactoryImplementor#getQueryEngine()
 * @see QueryEngine#getSemanticQueryProducer()
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SemanticQueryProducer {
	/**
	 * Performs the interpretation of a HQL/JPQL query string to SQM.
	 *
	 * @param query The HQL/JPQL query string to interpret
	 *
	 * @return The semantic representation of the incoming query.
	 */
	SqmStatement interpret(String query);

	default void close() {
		// nothing to do generally speaking
	}
}
