/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
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

	private final TemporaryTable idTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;


	public TableBasedDeleteHandler(
			SqmDeleteStatement<?> sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		super( sqmDeleteStatement, sessionFactory );
		this.idTable = idTable;

		this.domainParameterXref = domainParameterXref;
		this.temporaryTableStrategy  = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;

		this.sessionUidAccess = sessionUidAccess;
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

	protected ExecutionDelegate resolveDelegate(DomainQueryExecutionContext executionContext) {
		if ( getEntityDescriptor().getSoftDeleteMapping() != null ) {
			return new SoftDeleteExecutionDelegate(
					getEntityDescriptor(),
					idTable,
					temporaryTableStrategy,
					forceDropAfterUse,
					getSqmDeleteOrUpdateStatement(),
					domainParameterXref,
					executionContext.getQueryOptions(),
					executionContext.getSession().getLoadQueryInfluencers(),
					executionContext.getQueryParameterBindings(),
					sessionUidAccess,
					getSessionFactory()
			);
		}

		return new RestrictedDeleteExecutionDelegate(
				getEntityDescriptor(),
				idTable,
				temporaryTableStrategy,
				forceDropAfterUse,
				getSqmDeleteOrUpdateStatement(),
				domainParameterXref,
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getQueryParameterBindings(),
				sessionUidAccess,
				getSessionFactory()
		);
	}

	// Getters for Hibernate Reactive
	protected TemporaryTable getIdTable() {
		return idTable;
	}

	protected AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	protected TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	protected boolean isForceDropAfterUse() {
		return forceDropAfterUse;
	}

	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	protected DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public SqmDeleteStatement<?> getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement<?>) super.getSqmDeleteOrUpdateStatement();
	}
}
