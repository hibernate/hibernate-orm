/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;

/**
 * This is a strategy that mimics temporary tables for databases which do not support
 * temporary tables.  It follows a pattern similar to the ANSI SQL definition of global
 * temporary table using a "session id" column to segment rows from the various sessions.
 *
 * @author Steve Ebersole
 */
public class PersistentTableInsertStrategy extends PersistentTableStrategy implements SqmMultiTableInsertStrategy {

	public PersistentTableInsertStrategy(
			TemporaryTable entityTable,
			SessionFactoryImplementor sessionFactory) {
		super( entityTable, sessionFactory );
	}

	@Override
	public int executeInsert(
			SqmInsertStatement<?> sqmInsertStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedInsertHandler(
				sqmInsertStatement,
				domainParameterXref,
				getTemporaryTable(),
				getSessionFactory().getJdbcServices().getDialect().getTemporaryTableAfterUseAction(),
				session -> session.getSessionIdentifier().toString(),
				getSessionFactory()
		).execute( context );
	}
}
