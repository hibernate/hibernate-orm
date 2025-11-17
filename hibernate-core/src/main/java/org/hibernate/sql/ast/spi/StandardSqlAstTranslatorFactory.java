/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Standard implementation of SqlAstTranslatorFactory
 *
 * @author Steve Ebersole
 */
public class StandardSqlAstTranslatorFactory implements SqlAstTranslatorFactory {

	@Override
	public SqlAstTranslator<JdbcOperationQuerySelect> buildSelectTranslator(SessionFactoryImplementor sessionFactory, SelectStatement statement) {
		return buildTranslator( sessionFactory, statement );
	}

	@Override
	public SqlAstTranslator<? extends JdbcOperationQueryMutation> buildMutationTranslator(SessionFactoryImplementor sessionFactory, MutationStatement statement) {
		return buildTranslator( sessionFactory, statement );
	}

	@Override
	public <O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(TableMutation<O> mutation, SessionFactoryImplementor sessionFactory) {
		return buildTranslator( sessionFactory, mutation );
	}

	/**
	 * Consolidated building of a translator for all Query cases
	 */
	protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		return new StandardSqlAstTranslator<>( sessionFactory, statement );
	}

}
