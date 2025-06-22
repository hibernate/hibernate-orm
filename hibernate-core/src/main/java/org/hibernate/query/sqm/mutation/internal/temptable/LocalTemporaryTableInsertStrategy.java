/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableInsertStrategy extends LocalTemporaryTableStrategy implements SqmMultiTableInsertStrategy {

	public LocalTemporaryTableInsertStrategy(
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
				isDropIdTables()
						? AfterUseAction.DROP
						: getSessionFactory().getJdbcServices().getDialect().getTemporaryTableAfterUseAction(),
				session -> {
					throw new UnsupportedOperationException( "Unexpected call to access Session uid" );
				},
				getSessionFactory()
		).execute( context );
	}
}
