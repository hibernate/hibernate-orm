/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionException;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.util.Objects;
import java.util.TimeZone;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * Builder for shared {@linkplain StatelessSessionImplementor stateless} sessions.
 * Exposes the builder state via its {@linkplain CommonSharedSessionCreationOptions} implementation
 * for use when creating the shared stateless session.
 *
 * @author Steve Ebersole
 */
public abstract class SharedStatelessSessionBuilderImpl
		extends AbstractCommonBuilder<SharedStatelessSessionBuilder>
		implements SharedStatelessSessionBuilder, CommonSharedSessionCreationOptions {

	protected final SharedSessionContractImplementor original;
	protected boolean shareTransactionContext;

	public SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original) {
		super( original.getSessionFactory() );
		this.original = original;
		final var options = original.getSessionFactory().getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		tenantIdentifier = original.getTenantIdentifierValue();
		// good idea to inherit this
		jdbcTimeZone = original.getJdbcTimeZone();
	}

	protected abstract StatelessSessionImplementor createStatelessSession();

	@Override
	protected SharedStatelessSessionBuilder getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedStatelessSessionBuilder

	@Override
	public StatelessSessionImplementor open() {
		CORE_LOGGER.openingStatelessSession( tenantIdentifier );
		if ( original.getSessionFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( shareTransactionContext ) {
				final var tenantId = original.getTenantIdentifierValue();
				assert tenantId != null;
				if ( Objects.equals( tenantId, tenantIdentifier ) ) {
					throw new SessionException( "Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
			}
		}
		return createStatelessSession();
	}

	@Override
	public StatelessSession openStatelessSession() {
		return open();
	}

	@Override
	public SharedStatelessSessionBuilder connection() {
		shareTransactionContext = true;
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder interceptor() {
		interceptor = original.getInterceptor();
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder statementInspector() {
		this.statementInspector = original.getJdbcSessionContext().getStatementInspector();
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CommonSharedSessionCreationOptions

	@Override
	public Interceptor getInterceptor() {
		return configuredInterceptor();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Override
	public Object getTenantIdentifierValue() {
		return tenantIdentifier;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public CacheMode getInitialCacheMode() {
		return cacheMode;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return jdbcTimeZone;
	}

	@Override @Deprecated(forRemoval = true)
	public StatelessSessionBuilder tenantIdentifier(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return null;
	}

	@Override @Deprecated
	public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
		return this;
	}

	@Override
	public boolean isTransactionCoordinatorShared() {
		return shareTransactionContext;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return shareTransactionContext
				? original.getTransactionCoordinator()
				: null;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return shareTransactionContext
				? original.getJdbcCoordinator()
				: null;
	}

	@Override
	public TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor() {
		return shareTransactionContext
				? original.getTransactionCompletionCallbacksImplementor()
				: null;
	}

	@Override
	public Transaction getTransaction() {
		return shareTransactionContext
				? original.getTransaction()
				: null;
	}
}
