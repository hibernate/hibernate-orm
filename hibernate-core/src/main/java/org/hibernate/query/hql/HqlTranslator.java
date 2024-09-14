/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hql;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * Main entry point into building semantic queries.
 *
 * @see SessionFactoryImplementor#getQueryEngine()
 * @see QueryEngine#getHqlTranslator()
 *
 * @author Steve Ebersole
 */
@Incubating
public interface HqlTranslator {
	/**
	 * Performs the interpretation of a HQL/JPQL query string to SQM.
	 *
	 * @param hql The HQL/JPQL query string to interpret
	 * @param expectedResultType The type specified when creating the query
	 *
	 * @return The semantic representation of the incoming query.
	 */
	<R> SqmStatement<R> translate(String hql, Class<R> expectedResultType);
}
