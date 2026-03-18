/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcSelect;
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
	SqlAstTranslator<JdbcSelect> buildSelectTranslator(SessionFactoryImplementor sessionFactory, SelectStatement statement);

	/**
	 * Builds a single-use mutation translator
	 */
	SqlAstTranslator<? extends JdbcOperationQueryMutation> buildMutationTranslator(SessionFactoryImplementor sessionFactory, MutationStatement statement);

	/**
	 * Builds a single-use translator for dealing with model mutations
	 */
	default <O extends JdbcOperation> SqlAstTranslator buildGraphMutationTranslator(
			org.hibernate.action.queue.mutation.ast.TableMutation<O> mutation,
			SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	<O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(TableMutation<O> mutation, SessionFactoryImplementor sessionFactory);

}
