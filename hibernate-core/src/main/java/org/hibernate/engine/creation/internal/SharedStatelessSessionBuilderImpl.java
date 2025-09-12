/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionException;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.jboss.logging.Logger;

import java.util.Objects;

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

	private static final Logger LOG = CoreLogging.logger( SharedStatelessSessionBuilderImpl.class );

	protected final SharedSessionContractImplementor original;
	protected boolean shareTransactionContext;

	public SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original) {
		super( original.getSessionFactory() );
		this.original = original;
		final var options = original.getSessionFactory().getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		tenantIdentifier = original.getTenantIdentifierValue();
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
		LOG.tracef( "Opening StatelessSession [tenant=%s]", tenantIdentifier );
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
	public Transaction getTransaction() {
		return shareTransactionContext
				? original.getTransaction()
				: null;
	}
}
