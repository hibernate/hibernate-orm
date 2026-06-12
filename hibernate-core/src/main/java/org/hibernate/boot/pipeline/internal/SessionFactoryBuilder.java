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
import org.hibernate.boot.pipeline.internal.settings.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
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
			final var options = SessionFactoryOptionsAdapter.create(
					sessionFactorySettings,
					builtInObservers( metadata ),
					additionalObservers
			);
			if ( options.getServiceRegistry() != sessionFactorySettings.serviceRegistry() ) {
				throw new IllegalStateException( "SessionFactoryOptions adapter used the wrong service registry" );
			}
			final var metadataBuildingContext = resolvedMetadata.bindingState().getMetadataBuildingContext();
			metadata.getTypeConfiguration().scope( metadataBuildingContext );
			return new SessionFactoryImpl(
					metadata,
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
}
