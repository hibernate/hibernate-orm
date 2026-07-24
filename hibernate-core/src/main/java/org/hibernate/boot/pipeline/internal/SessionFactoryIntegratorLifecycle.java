/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.metamodel.spi.SessionFactoryAccess;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/// Internal lifecycle adapter for legacy Integrator callbacks.
///
/// This keeps Integrator discovery and successful-integration bookkeeping out of
/// `SessionFactoryImpl` while preserving the existing legacy callback contract.
/// It is not a replacement integration SPI.
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactoryIntegratorLifecycle implements SessionFactoryObserver {
	private final SessionFactoryAccess sessionFactoryAccess;
	private final SessionFactoryServiceRegistry serviceRegistry;
	private final ArrayList<Integrator> integrated = new ArrayList<>();

	public SessionFactoryIntegratorLifecycle(
			SessionFactoryAccess sessionFactoryAccess,
			SessionFactoryServiceRegistry serviceRegistry) {
		this.sessionFactoryAccess = sessionFactoryAccess;
		this.serviceRegistry = serviceRegistry;
	}

	public void integrate(MetadataImplementor metadata, BootstrapContext bootstrapContext) {
		final var sessionFactory = sessionFactoryAccess.getSessionFactory();
		for ( var integrator : serviceRegistry.requireService( IntegratorService.class ).getIntegrators() ) {
			integrator.integrate( metadata, bootstrapContext, sessionFactory );
			integrated.add( integrator );
		}
	}

	public void disintegrate(Exception startupException) {
		final var sessionFactory = sessionFactoryAccess.getSessionFactory();
		for ( var integrator : integrated ) {
			try {
				integrator.disintegrate( sessionFactory, serviceRegistry );
			}
			catch (Throwable ex) {
				startupException.addSuppressed( ex );
			}
		}
		integrated.clear();
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		final var sessionFactory = sessionFactoryAccess.getSessionFactory();
		for ( var integrator : integrated ) {
			integrator.disintegrate( sessionFactory, serviceRegistry );
		}
		integrated.clear();
	}
}
