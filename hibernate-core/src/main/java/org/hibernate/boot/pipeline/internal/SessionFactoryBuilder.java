/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryObserverFactory;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;

/// Builds a runtime SessionFactoryImplementor from resolved boot products.
/// This is the gross target after metadata resolution.  Unlike settings and
/// metadata, the final product is not a "resolved" description; it is the runtime
/// [SessionFactoryImplementor] itself.
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactoryBuilder {
	/**
	 * Build a SessionFactoryImplementor from the resolved bootstrap settings root.
	 *
	 * @param bootstrapSettings Resolved bootstrap settings
	 * @param resolvedMetadata Resolved ORM metadata
	 * @param serviceRegistry Service registry for the factory build
	 *
	 * @return The runtime SessionFactoryImplementor
	 */
	public static SessionFactoryImplementor build(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata,
			ServiceRegistry serviceRegistry) {
		return build(
				SettingsResolver.resolveSessionFactorySettings( bootstrapSettings, serviceRegistry ),
				resolvedMetadata,
				serviceRegistry
		);
	}

	/// Build a SessionFactoryImplementor from resolved factory settings and metadata.
	///
	/// @param sessionFactorySettings Resolved SessionFactory settings
	/// @param resolvedMetadata Resolved ORM metadata
	/// @param serviceRegistry Service registry for the factory build
	///
	/// @return The runtime SessionFactoryImplementor
	public static SessionFactoryImplementor build(
			ResolvedSessionFactorySettings sessionFactorySettings,
			ResolvedMetadata resolvedMetadata,
			ServiceRegistry serviceRegistry) {
		return build(
				sessionFactorySettings,
				resolvedMetadata,
				serviceRegistry,
				new SessionFactoryObserver[0]
		);
	}

	/// Build a SessionFactoryImplementor from resolved factory settings and metadata.
	///
	/// @param sessionFactorySettings Resolved SessionFactory settings
	/// @param resolvedMetadata Resolved ORM metadata
	/// @param serviceRegistry Service registry for the factory build
	/// @param additionalObservers Additional lifecycle observers supplied by the entry point
	///
	/// @return The runtime SessionFactoryImplementor
	public static SessionFactoryImplementor build(
			ResolvedSessionFactorySettings sessionFactorySettings,
			ResolvedMetadata resolvedMetadata,
			ServiceRegistry serviceRegistry,
			SessionFactoryObserver... additionalObservers) {
		Objects.requireNonNull( sessionFactorySettings );
		Objects.requireNonNull( resolvedMetadata );
		Objects.requireNonNull( serviceRegistry );
		if ( resolvedMetadata.metadata() instanceof InFlightMetadataCollector metadataCollector ) {
			metadataCollector.getBootstrapContext();
			throw new IllegalArgumentException(
					"SessionFactory construction requires finalized metadata, not an in-flight collector"
			);
		}
		if ( resolvedMetadata.metadata() instanceof MetadataImpl metadata ) {
			final var constructionSettings = sessionFactorySettings.withSessionFactoryObservers(
					sessionFactoryObservers( sessionFactorySettings, metadata, additionalObservers )
			);
			final var identity = SessionFactoryConstructionIdentity.resolve( constructionSettings );
			final var options = SessionFactoryOptionsAdapter.create(
					constructionSettings,
					identity
			);
			if ( options.getServiceRegistry() != constructionSettings.serviceRegistry() ) {
				throw new IllegalStateException( "SessionFactoryOptions adapter used the wrong service registry" );
			}
			final var metadataBuildingContext = resolvedMetadata.bindingState().getMetadataBuildingContext();
			metadata.getTypeConfiguration().scope( metadataBuildingContext );
			return SessionFactoryConstructionCoordinator.buildSessionFactory(
					metadata,
					constructionSettings,
					identity,
					options,
					metadata.getBootstrapContext()
			);
		}
		throw new IllegalArgumentException(
				"SessionFactory construction requires metadata exposing its BootstrapContext"
		);
	}

	private static SessionFactoryObserver[] builtInObservers(MetadataImpl metadata) {
		return SessionFactoryObserverFactory.createObservers( metadata );
	}

	private static SessionFactoryObserver[] sessionFactoryObservers(
			ResolvedSessionFactorySettings sessionFactorySettings,
			MetadataImpl metadata,
			SessionFactoryObserver[] additionalObservers) {
		return concat(
				concat( builtInObservers( metadata ), sessionFactorySettings.sessionFactoryObservers() ),
				additionalObservers
		);
	}

	private static SessionFactoryObserver[] concat(
			SessionFactoryObserver[] first,
			SessionFactoryObserver[] second) {
		if ( first.length == 0 ) {
			return second.clone();
		}
		if ( second.length == 0 ) {
			return first.clone();
		}
		final var combined = java.util.Arrays.copyOf( first, first.length + second.length );
		System.arraycopy( second, 0, combined, first.length, second.length );
		return combined;
	}
}
