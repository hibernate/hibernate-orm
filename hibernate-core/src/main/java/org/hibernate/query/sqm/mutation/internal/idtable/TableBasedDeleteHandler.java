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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandler
		extends AbstractMutationHandler
		implements DeleteHandler {
	private static final Logger log = Logger.getLogger( TableBasedDeleteHandler.class );

	public interface ExecutionDelegate {
		int execute(DomainQueryExecutionContext executionContext);
	}

	private final IdTable idTable;
	private final TempTableDdlTransactionHandling ddlTransactionHandling;
	private final BeforeUseAction beforeUseAction;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final Supplier<IdTableExporter> exporterSupplier;

	private final DomainParameterXref domainParameterXref;


	public TableBasedDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			IdTable idTable,
			Function<SharedSessionContractImplementor,
			String> sessionUidAccess,
			Supplier<IdTableExporter> exporterSupplier,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			SessionFactoryImplementor sessionFactory) {
		super( sqmDeleteStatement, sessionFactory );
		this.idTable = idTable;
		this.ddlTransactionHandling = ddlTransactionHandling;
		this.beforeUseAction = beforeUseAction;
		this.afterUseAction = afterUseAction;

		this.domainParameterXref = domainParameterXref;

		this.sessionUidAccess = sessionUidAccess;
		this.exporterSupplier = exporterSupplier;
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table delete execution - %s",
					getSqmDeleteOrUpdateStatement().getRoot().getModel().getName()
			);
		}
		return resolveDelegate( executionContext ).execute( executionContext );
	}

	private ExecutionDelegate resolveDelegate(DomainQueryExecutionContext executionContext) {
		return new RestrictedDeleteExecutionDelegate(
				getEntityDescriptor(),
				idTable,
				getSqmDeleteOrUpdateStatement(),
				domainParameterXref,
				beforeUseAction,
				afterUseAction,
				ddlTransactionHandling,
				exporterSupplier,
				sessionUidAccess,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				getSessionFactory()
		);
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}
}
