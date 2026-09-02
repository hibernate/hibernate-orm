/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.query.sqm.tree.spi.SqmStatement;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Global integration contract for translating HQL or JPQL text to SQM.
///
/// This is not a database Dialect contract. Framework integrations may supply
/// one translator through [org.hibernate.query.spi.QueryEngineOptions]; the
/// resulting instance is normally shared by the query engine and must therefore
/// be thread-safe. Implementations must produce a fresh semantic statement for
/// each invocation and must not infer database syntax.
///
/// @see SessionFactoryImplementor#getQueryEngine()
/// @see QueryEngineOptions#getCustomHqlTranslator()
/// @see QueryEngine#getHqlTranslator()
///
/// @author Steve Ebersole
@Incubating
@SPI({ IMPLEMENT, SUPPLY })
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
