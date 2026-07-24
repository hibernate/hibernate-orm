/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionRequest;
import org.hibernate.boot.pipeline.spi.SessionFactoryProducer;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;

import static java.lang.String.join;
import static org.hibernate.internal.util.StringHelper.isBlank;

/// Coordinates the final SessionFactory construction step.
///
/// This class is the first seam for moving construction work out of
/// [SessionFactoryImpl].  For now, the default producer still constructs
/// ORM's standard factory and therefore keeps an internal reference to the
/// [BootstrapContext].  That reference is deliberately not exposed through
/// the public [SessionFactoryConstructionRequest] SPI.
///
/// @since 9.0
/// @author Steve Ebersole
public final class SessionFactoryConstructionCoordinator {
	private SessionFactoryConstructionCoordinator() {
	}

	public static SessionFactoryImplementor buildSessionFactory(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings resolvedSettings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			RuntimeMappingHandoff runtimeMappingHandoff) {
		final var state = new SessionFactoryConstructionState(
				metadata,
				resolvedSettings,
				identity,
				options,
				bootstrapContext,
				runtimeMappingHandoff
		);
		final var request = new ConstructionRequest( state );
		final var producer = resolveProducer( producerSelectionContext( request.getServiceRegistry() ) );
		return producer == null
				? buildDefaultSessionFactory( state )
				: Objects.requireNonNull(
						producer.buildSessionFactory( request ),
						"SessionFactoryProducer returned null"
				);
	}

	public static SessionFactoryImplementor buildSessionFactory(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			RuntimeMappingHandoff runtimeMappingHandoff) {
		final var state = SessionFactoryConstructionState.legacy(
				metadata,
				options,
				bootstrapContext,
				runtimeMappingHandoff
		);
		final var request = new ConstructionRequest( state );
		final var producer = resolveProducer( producerSelectionContext( request.getServiceRegistry() ) );
		return producer == null
				? buildDefaultSessionFactory( state )
				: Objects.requireNonNull(
						producer.buildSessionFactory( request ),
						"SessionFactoryProducer returned null"
				);
	}

	private static ProducerSelectionContext producerSelectionContext(ServiceRegistry serviceRegistry) {
		return new ProducerSelectionContext(
				serviceRegistry.requireService( ClassLoaderService.class ),
				serviceRegistry.requireService( ConfigurationService.class )
		);
	}

	private static SessionFactoryProducer resolveProducer(ProducerSelectionContext context) {
		final var selectedProducerName = getSelectedProducerName( context );
		SessionFactoryProducer producer = null;
		List<String> activeProducerNames = null;
		for ( var discoveredProducer : context.classLoaderService().loadJavaServices( SessionFactoryProducer.class ) ) {
			final var discoveredProducerName = discoveredProducer.getProducerName();
			if ( isBlank( discoveredProducerName ) ) {
				throw new HibernateException(
						"SessionFactoryProducer " + discoveredProducer.getClass().getName()
								+ " returned a blank producer name"
				);
			}
			if ( activeProducerNames == null ) {
				activeProducerNames = new ArrayList<>();
			}
			activeProducerNames.add( producerDescription( discoveredProducer, discoveredProducerName ) );
			if ( selectedProducerName == null ) {
				producer = discoveredProducer;
			}
			else if ( selectedProducerName.equals( discoveredProducerName ) ) {
				if ( producer != null ) {
					throw new HibernateException(
							"Multiple SessionFactoryProducer definitions were discovered for selected producer name '"
									+ selectedProducerName + "': " + join( ", ", activeProducerNames )
					);
				}
				producer = discoveredProducer;
			}
		}

		if ( selectedProducerName == null && activeProducerNames != null && activeProducerNames.size() > 1 ) {
			throw new HibernateException(
					"Multiple active SessionFactoryProducer definitions were discovered: "
							+ join( ", ", activeProducerNames )
			);
		}
		if ( selectedProducerName != null && producer == null ) {
			throw new HibernateException(
					"No SessionFactoryProducer named '" + selectedProducerName + "' was discovered"
							+ ( activeProducerNames == null ? "" : ". Discovered producers: " + join( ", ", activeProducerNames ) )
			);
		}

		return producer;
	}

	private static String getSelectedProducerName(ProducerSelectionContext context) {
		final var selectedProducerName = context.configurationService()
				.getSettings()
				.get( PersistenceSettings.SESSION_FACTORY_PRODUCER );
		return selectedProducerName == null || isBlank( selectedProducerName.toString() )
				? null
				: selectedProducerName.toString();
	}

	private static String producerDescription(SessionFactoryProducer producer, String producerName) {
		return producer.getClass().getName() + " [" + producerName + "]";
	}

	private static SessionFactoryImplementor buildDefaultSessionFactory(SessionFactoryConstructionState state) {
		return new SessionFactoryImpl( SessionFactoryConstructionPlanBuilder.build( state ) );
	}

	private record ProducerSelectionContext(
			ClassLoaderService classLoaderService,
			ConfigurationService configurationService) {
	}

	private static class ConstructionRequest implements SessionFactoryConstructionRequest {
		private final SessionFactoryConstructionState state;

		private ConstructionRequest(SessionFactoryConstructionState state) {
			this.state = state;
		}

		@Override
		public MetadataImplementor getMetadata() {
			return state.metadata();
		}

		@Override
		public SessionFactoryOptions getOptions() {
			return state.options();
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return state.options().getServiceRegistry();
		}
	}
}
