/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
