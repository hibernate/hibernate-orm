/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.engine.creation.internal.options.SharedStatefulOptions;
import org.hibernate.engine.creation.spi.SharedSessionBuilderImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class SharedSessionBuilderImpl
		extends AbstractCommonBuilder<SharedSessionBuilderImplementor>
		implements SharedSessionBuilderImplementor {

	private final SharedSessionContractImplementor original;
	private final SharedStatefulOptions options;

	private boolean tenantIdChanged;
	private boolean readOnlyChanged;

	public SharedSessionBuilderImpl(SharedSessionContractImplementor original) {
		this( original, new SharedStatefulOptions( original ) );
	}

	protected SharedSessionBuilderImpl(SharedSessionContractImplementor original, SharedStatefulOptions options) {
		super( original.getFactory(), options );
		this.original = original;
		this.options = options;
	}

	protected abstract SessionImplementor createSession(SharedStatefulOptions options);

	@Override
	protected SharedSessionBuilderImplementor getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedSessionBuilder

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor withOption(EntityManager.CreationOption option) {
		options.apply( option );
		return this;
	}

	@Override
	@Nonnull
	public SessionImplementor open() {
		CORE_LOGGER.openingSession( options.getTenantIdentifierValue() );
		if ( original.getFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( options.isTransactionCoordinatorShared() ) {
				if ( tenantIdChanged ) {
					throw new SessionException(
							"Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
				if ( readOnlyChanged ) {
					throw new SessionException(
							"Cannot redefine the read-only mode on a child session if the connection is reused" );
				}
			}
		}
		return createSession( options );
	}

	@Override
	@Nonnull
	public SessionImplementor openSession() {
		return open();
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor tenantIdentifier(Object tenantIdentifier) {
		super.tenantIdentifier( tenantIdentifier );
		tenantIdChanged = true;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor readOnly(boolean readOnly) {
		super.readOnly( readOnly );
		readOnlyChanged = true;
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor connection() {
		options.shareTransactionContext();
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor interceptor() {
		options.useInterceptor( original.getInterceptor() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor statementInspector() {
		options.statementInspector( original.getJdbcSessionContext().getStatementInspector() );
		return this;
	}

	private PhysicalConnectionHandlingMode getConnectionHandlingMode() {
		return original.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode();
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor connectionHandlingMode() {
		connectionHandlingMode( getConnectionHandlingMode() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoJoinTransactions() {
		options.autoJoinTransactions( original.shouldAutoJoinTransaction() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoJoinTransactions(boolean autoJoinTransactions) {
		options.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClose(boolean autoClose) {
		options.autoClose( autoClose );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor flushMode() {
		flushMode( original.getHibernateFlushMode() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClose() {
		autoClose( original instanceof SessionImplementor statefulSession
				&& statefulSession.isAutoCloseSessionEnabled() );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor identifierRollback(boolean identifierRollback) {
		options.identifierRollback( identifierRollback );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor clearEventListeners() {
		options.clearEventListeners();
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor flushMode(@Nonnull FlushMode flushMode) {
		options.flushMode( flushMode );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor autoClear(boolean autoClear) {
		options.autoClear( autoClear );
		return this;
	}

	@Override
	@Deprecated
	@Nonnull
	public SharedSessionBuilderImplementor statementInspector(@Nonnull StatementInspector statementInspector) {
		options.statementInspector( statementInspector );
		return this;
	}

	@Override
	@Deprecated
	@Nonnull
	public SharedSessionBuilderImplementor connectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode connectionHandlingMode) {
		options.connectionHandlingMode( connectionHandlingMode );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor eventListeners(@Nonnull SessionEventListener... listeners) {
		options.eventListeners( sessionFactory, listeners );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor defaultBatchFetchSize(int defaultBatchFetchSize) {
		options.defaultBatchFetchSize( defaultBatchFetchSize );
		return this;
	}

	@Override
	@Nonnull
	public SharedSessionBuilderImplementor subselectFetchEnabled(boolean subselectFetchEnabled) {
		options.subselectFetchEnabled( subselectFetchEnabled );
		return this;
	}
}
