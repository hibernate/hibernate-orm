/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Interceptor;
import org.hibernate.SessionException;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * @author Steve Ebersole
 */
public class SharedStatelessSessionBuilderImpl
		implements SharedStatelessSessionBuilder, CommonSharedSessionCreationOptions {

	private final SharedSessionContractImplementor original;

	private Interceptor interceptor;
	private StatementInspector statementInspector;
	private Object tenantIdentifier;
	private boolean shareTransactionContext;

	public SharedStatelessSessionBuilderImpl(SharedSessionContractImplementor original) {
		this.original = original;

		this.tenantIdentifier = original.getTenantIdentifierValue();
		this.interceptor = original.getSessionFactory().getSessionFactoryOptions().getInterceptor();
		this.statementInspector = original.getSessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SharedStatelessSessionBuilder

	@Override
	public StatelessSessionImplementor open() {
		if ( original.getSessionFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
			if ( shareTransactionContext ) {
				assert original.getTenantIdentifierValue() != null;
				if ( Objects.equals( original.getTenantIdentifierValue(), tenantIdentifier ) ) {
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
	public SharedStatelessSessionBuilder interceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder noInterceptor() {
		interceptor = EmptyInterceptor.INSTANCE;
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder statementInspector(UnaryOperator<String> operator) {
		this.statementInspector = operator::apply;
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder statementInspector() {
		this.statementInspector = original.getJdbcSessionContext().getStatementInspector();
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder noStatementInspector() {
		this.statementInspector = StatementInspector.NONE;
		return this;
	}

	@Override
	public SharedStatelessSessionBuilder tenantIdentifier(Object tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
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
		return null;
	}

	@Override
	public boolean isTransactionCoordinatorShared() {
		return shareTransactionContext;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return isTransactionCoordinatorShared()
				? original.getTransactionCoordinator()
				: null;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return isTransactionCoordinatorShared()
				? original.getJdbcCoordinator()
				: null;
	}
}
