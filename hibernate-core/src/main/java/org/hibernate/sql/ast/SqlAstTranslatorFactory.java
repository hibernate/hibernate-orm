/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * Factory for obtaining single-use SQL AST translators
 *
 * @author Steve Ebersole
 */
public interface SqlAstTranslatorFactory {
	/**
	 * Builds a single-use select translator
	 */
	SqlAstTranslator<JdbcSelect> buildSelectTranslator(SessionFactoryImplementor sessionFactory, SelectStatement statement);

	/**
	 * Builds a single-use delete translator
	 */
	SqlAstTranslator<JdbcDelete> buildDeleteTranslator(SessionFactoryImplementor sessionFactory, DeleteStatement statement);

	/**
	 * Builds a single-use insert-select translator
	 */
	SqlAstTranslator<JdbcInsert> buildInsertTranslator(SessionFactoryImplementor sessionFactory, InsertStatement statement);

	/**
	 * Builds a single-use update translator
	 */
	SqlAstTranslator<JdbcUpdate> buildUpdateTranslator(SessionFactoryImplementor sessionFactory, UpdateStatement statement);
}
