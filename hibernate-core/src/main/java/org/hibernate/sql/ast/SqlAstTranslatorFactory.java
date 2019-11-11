/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Factory for obtaining single-use SQL AST translators
 *
 * @author Steve Ebersole
 */
public interface SqlAstTranslatorFactory {
	/**
	 * Builds a single-use select translator
	 */
	SqlAstSelectTranslator buildSelectTranslator(SessionFactoryImplementor sessionFactory);

	/**
	 * Builds a single-use delete translator
	 */
	SqlAstDeleteTranslator buildDeleteTranslator(SessionFactoryImplementor sessionFactory);

	/**
	 * Builds a single-use delete translator
	 */
	SqlAstInsertSelectTranslator buildInsertTranslator(SessionFactoryImplementor sessionFactory);

	// todo (6.0) : update, insert, etc
}
