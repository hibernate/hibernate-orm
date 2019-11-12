/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

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

	public LocalTemporaryTableStrategy(
			IdTable idTable,
			Supplier<IdTableExporter> idTableExporterAccess,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling) {
		this.idTable = idTable;
		this.idTableExporterAccess = idTableExporterAccess;
		this.afterUseAction = afterUseAction;
		this.ddlTransactionHandling = ddlTransactionHandling;
	}

	public LocalTemporaryTableStrategy(
			IdTable idTable,
			Function<Integer, String> databaseTypeNameResolver,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling) {
		this(
				idTable,
				() -> new TempIdTableExporter( true, databaseTypeNameResolver ),
				afterUseAction,
				ddlTransactionHandling
		);
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		if ( sqmDeleteStatement.getWhereClause() == null
				|| sqmDeleteStatement.getWhereClause().getPredicate() == null ) {
			// optimization - special handler not needing the temp table
			return new UnrestrictedTableBasedDeleteHandler(
					sqmDeleteStatement,
					idTable,
					ddlTransactionHandling,
					domainParameterXref,
					BeforeUseAction.NONE,
					afterUseAction,
					sessionContractImplementor -> null,
					creationContext
			);
		}

		return new TableBasedDeleteHandler(
				sqmDeleteStatement,
				idTable,
				idTableExporterAccess,
				BeforeUseAction.CREATE,
				afterUseAction,
				ddlTransactionHandling,
				domainParameterXref,
				creationContext
		);
	}
}
