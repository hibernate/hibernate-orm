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
import org.hibernate.internal.CommonSharedSessionCreationOptions;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.util.Objects;

/**
 * Builder for shared {@linkplain StatelessSessionImplementor stateless} sessions.
 * Exposes the builder state via its {@linkplain CommonSharedSessionCreationOptions} implementation
 * for use when creating the shared stateless session.
 *
 * @author Steve Ebersole
 */
public class SharedStatelessSessionBuilderImpl
		extends AbstractCommonBuilder<SharedStatelessSessionBuilder>
		implements SharedStatelessSessionBuilder, CommonSharedSessionCreationOptions {

	protected final SharedSessionContractImplementor original;
	protected boolean shareTransactionContext;

	public SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original) {
		super( (SessionFactoryImpl) original.getSessionFactory() );
		this.original = original;
		tenantIdentifier = original.getTenantIdentifierValue();
		final var options = original.getSessionFactory().getSessionFactoryOptions();
		interceptor = options.getInterceptor();
		statementInspector = options.getStatementInspector();
	}

	@Override
	protected SharedStatelessSessionBuilder getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedStatelessSessionBuilder

	@Override
	public StatelessSessionImplementor open() {
		if ( original.getSessionFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( shareTransactionContext ) {
				final var tenantId = original.getTenantIdentifierValue();
				assert tenantId != null;
				if ( Objects.equals( tenantId, tenantIdentifier ) ) {
					throw new SessionException( "Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
			}
		}
		return new StatelessSessionImpl( original.getSessionFactory(), this );
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
		return interceptor;
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
