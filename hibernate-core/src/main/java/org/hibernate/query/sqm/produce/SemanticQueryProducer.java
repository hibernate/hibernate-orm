/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.spi.RootQuery;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

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

	/**
	 * Interpret the JPA criteria tree into SQM
	 *
	 * @param query The criteria query
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	<R> SqmSelectStatement interpret(RootQuery<R> query);

	/**
	 * Interpret the JPA criteria tree into SQM
	 *
	 * @param criteria The DELETE criteria
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	<E> SqmDeleteStatement<E> interpret(CriteriaDelete<E> criteria);

	// todo (6.0) : whatever JpaCriteriaDelete becomes in SPI ^^
	//		and JpaCriteriaUpdate vv

	/**
	 * Interpret the JPA criteria tree into SQM
	 *
	 * @param criteria The UPDATE criteria
	 *
	 * @return The semantic representation of the incoming criteria query.
	 */
	SqmUpdateStatement interpret(CriteriaUpdate criteria);

	default void close() {
		// nothing to do generally speaking
	}
}
