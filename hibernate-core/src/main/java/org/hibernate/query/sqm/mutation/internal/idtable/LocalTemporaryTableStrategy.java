/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Strategy based on ANSI SQL's definition of a "local temporary table" (local to each db session).
 *
 * @author Steve Ebersole
 */
public class LocalTemporaryTableStrategy implements SqmMultiTableMutationStrategy {
	public static final String SHORT_NAME = "local_temporary";

	private final IdTable idTable;
	private final Supplier<IdTableExporter> idTableExporterAccess;
	private final AfterUseAction afterUseAction;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final SessionFactoryImplementor sessionFactory;

	public LocalTemporaryTableStrategy(
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SessionFactoryImplementor sessionFactory) {
		this.idTable = idTable;
		this.idTableExporterAccess = idTableExporterAccess;
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.sessionFactory = sessionFactory;
	}

	public LocalTemporaryTableStrategy(
			IdTable idTable,
			Function<Integer, String> databaseTypeNameResolver,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SessionFactoryImplementor sessionFactory) {
		this(
				idTable,
				() -> new TempIdTableExporter( true, databaseTypeNameResolver ),
				afterUseAction,
				ddlTransactionHandling,
				sessionFactory
		);
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			ExecutionContext context) {
		return new TableBasedUpdateHandler(
				sqmUpdate,
				domainParameterXref,
				idTable,
				session -> {
					throw new UnsupportedOperationException( "Unexpected call to access Session uid" );
				},
				idTableExporterAccess,
				BeforeUseAction.CREATE,
				afterUseAction,
				ddlTransactionHandling,
				sessionFactory
		).execute( context );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			ExecutionContext context) {
		return new TableBasedDeleteHandler(
				sqmDelete,
				domainParameterXref,
				idTable,
				session -> {
					throw new UnsupportedOperationException( "Unexpected call to access Session uid" );
				},
				idTableExporterAccess,
				BeforeUseAction.CREATE,
				afterUseAction,
				ddlTransactionHandling,
				sessionFactory
		).execute( context );
	}
}
