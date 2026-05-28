/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal;

import org.hibernate.SessionException;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.creation.internal.options.SharedStatelessOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.Objects;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * Builder for shared {@linkplain StatelessSessionImplementor stateless} sessions.
 * @author Steve Ebersole
 */
public abstract class SharedStatelessSessionBuilderImpl
		extends AbstractCommonBuilder<SharedStatelessSessionBuilder>
		implements SharedStatelessSessionBuilder {

	protected final SharedSessionContractImplementor original;
	protected final SharedStatelessOptions options;

	public SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original) {
		this( original, new SharedStatelessOptions( original ) );
	}

	protected SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original, SharedStatelessOptions options) {
		super( original.getSessionFactory(), options );
		this.original = original;
		this.options = options;
	}

	protected abstract StatelessSessionImplementor createStatelessSession(SharedStatelessOptions options);

	@Override
	protected SharedStatelessSessionBuilder getThis() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedStatelessSessionBuilder

	@Override
	public StatelessSessionImplementor open() {
		CORE_LOGGER.openingStatelessSession( options.getTenantIdentifierValue() );
		if ( original.getSessionFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( options.isTransactionCoordinatorShared() ) {
				final var tenantId = original.getTenantIdentifierValue();
				assert tenantId != null;
				if ( !Objects.equals( tenantId, options.getTenantIdentifierValue() ) ) {
					throw new SessionException( "Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
			}
		}
		return createStatelessSession( options );
	}

	@Override
	public StatelessSession openStatelessSession() {
		return open();
	}

	@Override
	public SharedStatelessSessionBuilder connection() {
		options.shareTransactionContext();
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder interceptor() {
		options.useInterceptor( original.getInterceptor() );
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder statementInspector() {
		options.statementInspector( original.getJdbcSessionContext().getStatementInspector() );
		return this;
	}

	@Override @Deprecated
	public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
		options.statementInspector( statementInspector );
		return this;
	}
}
