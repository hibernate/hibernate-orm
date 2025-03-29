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
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableMutationStrategy extends LocalTemporaryTableStrategy implements SqmMultiTableMutationStrategy {

	public LocalTemporaryTableMutationStrategy(
			TemporaryTable idTable,
			SessionFactoryImplementor sessionFactory) {
		super( idTable, sessionFactory );
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedUpdateHandler(
				sqmUpdate,
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

	@Override
	public int executeDelete(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		final TableBasedDeleteHandler deleteHandler = new TableBasedDeleteHandler(
				sqmDelete,
				domainParameterXref,
				getTemporaryTable(),
				isDropIdTables()
						? AfterUseAction.DROP
						: getSessionFactory().getJdbcServices().getDialect().getTemporaryTableAfterUseAction(),
				session -> {
					throw new UnsupportedOperationException( "Unexpected call to access Session uid" );
				},
				getSessionFactory()
		);
		return deleteHandler.execute( context );
	}

}
