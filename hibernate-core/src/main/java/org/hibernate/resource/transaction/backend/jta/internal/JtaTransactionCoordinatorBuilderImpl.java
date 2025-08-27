/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;

/**
 * Concrete builder for JTA-based TransactionCoordinator instances.
 *
 * @author Steve Ebersole
 */
public class JtaTransactionCoordinatorBuilderImpl implements TransactionCoordinatorBuilder, ServiceRegistryAwareService {

	public static final String SHORT_NAME = "jta";

	private JtaPlatform jtaPlatform;

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		return new JtaTransactionCoordinatorImpl(
				this,
				owner,
				options.shouldAutoJoinTransaction(),
				jtaPlatform
		);
	}

	@Override
	public boolean isJta() {
		return true;
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		// todo : I want to change this to IMMEDIATE_ACQUISITION_AND_HOLD
		return DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
	}

	@Override
	public DdlTransactionIsolator buildDdlTransactionIsolator(JdbcContext jdbcContext) {
		return new DdlTransactionIsolatorJtaImpl( jdbcContext );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
	}

}
