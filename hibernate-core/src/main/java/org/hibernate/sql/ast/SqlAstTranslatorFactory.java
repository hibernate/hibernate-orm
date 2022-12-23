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
import org.hibernate.sql.exec.spi.JdbcOperationQueryDelete;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcOperationQueryUpdate;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Factory for obtaining single-use SQL AST translators
 *
 * @author Steve Ebersole
 */
public interface SqlAstTranslatorFactory {
	/**
	 * Builds a single-use select translator
	 */
	SqlAstTranslator<JdbcOperationQuerySelect> buildSelectTranslator(SessionFactoryImplementor sessionFactory, SelectStatement statement);

	/**
	 * Builds a single-use delete translator
	 */
	SqlAstTranslator<JdbcOperationQueryDelete> buildDeleteTranslator(SessionFactoryImplementor sessionFactory, DeleteStatement statement);

	/**
	 * Builds a single-use insert-select translator
	 */
	SqlAstTranslator<JdbcOperationQueryInsert> buildInsertTranslator(SessionFactoryImplementor sessionFactory, InsertStatement statement);

	/**
	 * Builds a single-use update translator
	 */
	SqlAstTranslator<JdbcOperationQueryUpdate> buildUpdateTranslator(SessionFactoryImplementor sessionFactory, UpdateStatement statement);

	/**
	 * Builds a single-use translator for dealing with model mutations
	 */
	<O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(TableMutation<O> mutation, SessionFactoryImplementor sessionFactory);

}
