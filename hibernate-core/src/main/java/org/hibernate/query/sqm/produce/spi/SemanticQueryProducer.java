/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;

/**
 * Main entry point into building semantic queries.
 *
 * @see SessionFactoryImplementor#getQueryEngine()
 * @see QueryEngine#getSemanticQueryProducer()
 *
 * @author Steve Ebersole
 */
public interface SemanticQueryProducer {
	/**
	 * Performs the interpretation of a HQL/JPQL query string to SQM.
	 *
	 * @param query The HQL/JPQL query string to interpret
	 *
	 * @return The semantic representation of the incoming query.
	 */
	SqmStatement interpret(String query);

	/**
	 * Perform the interpretation of a (select) criteria query.
	 *
	 * @param query The criteria query
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	SqmSelectStatement interpret(CriteriaQuery query);

	/**
	 * Perform the interpretation of a (delete) criteria query.
	 *
	 * @param criteria The DELETE criteria
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	SqmDeleteStatement interpret(CriteriaDelete criteria);

	/**
	 * Perform the interpretation of a (update) criteria query.
	 *
	 * @param criteria The criteria query
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	SqmUpdateStatement interpret(CriteriaUpdate criteria);

	default void close() {
		// nothing to do generally speaking
	}
}
