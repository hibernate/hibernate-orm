/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Objects;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.internal.SessionFactoryObserverFactory;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;

/// Builds a runtime SessionFactoryImplementor from resolved boot products.
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactoryPipeline {
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
			StandardServiceRegistry serviceRegistry) {
		return build(
				SettingsResolver.resolveSessionFactorySettings( bootstrapSettings, serviceRegistry ),
				resolvedMetadata,
				serviceRegistry
		);
	}

	/// Build a SessionFactoryImplementor from finalized legacy Metadata and
	/// collected factory customizations.
	public static SessionFactoryImplementor build(
			Metadata metadata,
			SessionFactoryOptionsCollector optionsCollector) {
		if ( metadata instanceof MetadataImplementor metadataImplementor ) {
			return build( metadataImplementor, optionsCollector );
		}
		throw new IllegalArgumentException(
				"SessionFactory construction requires MetadataImplementor"
		);
	}

	/// Build a SessionFactoryImplementor from finalized legacy Metadata and
	/// collected factory customizations.
	public static SessionFactoryImplementor build(
			MetadataImplementor metadata,
			SessionFactoryOptionsCollector optionsCollector) {
		final var unwrappedMetadata = unwrapMetadata( metadata );
		return build( unwrappedMetadata, unwrappedMetadata.getMetadataBuildingOptions().getServiceRegistry(), optionsCollector );
	}

	/// Build a SessionFactoryImplementor from finalized legacy Metadata, an
	/// explicit factory service registry, and collected factory customizations.
	public static SessionFactoryImplementor build(
			MetadataImplementor metadata,
			StandardServiceRegistry serviceRegistry,
			SessionFactoryOptionsCollector optionsCollector) {
		final var unwrappedMetadata = unwrapMetadata( metadata );
		final var optionsBuilder = new SessionFactoryOptionsBuilder(
				serviceRegistry,
				unwrappedMetadata.getBootstrapContext()
		);
		optionsBuilder.addSessionFactoryObservers( SessionFactoryObserverFactory.createObservers( unwrappedMetadata ) );
		if ( unwrappedMetadata.getSqlFunctionMap() != null ) {
			unwrappedMetadata.getSqlFunctionMap().forEach( optionsBuilder::applySqlFunction );
		}
		return SessionFactoryConstructionCoordinator.buildSessionFactory(
				unwrappedMetadata,
				optionsCollector.buildOptions( optionsBuilder ),
				unwrappedMetadata.getBootstrapContext()
		);
	}

	private static MetadataImpl unwrapMetadata(MetadataImplementor metadata) {
		if ( metadata instanceof ResolvedMetadataImplementor resolvedMetadata ) {
			return unwrapMetadata( resolvedMetadata.getResolvedMetadata().metadata() );
		}
		if ( metadata instanceof MetadataImpl metadataImpl ) {
			return metadataImpl;
		}
		throw new IllegalArgumentException(
				"SessionFactory construction requires metadata exposing its BootstrapContext"
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
		if ( resolvedMetadata.metadata() instanceof InFlightMetadataCollector ) {
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
					metadata.getBootstrapContext(),
					resolvedMetadata.bindingState().getBootBindingModel()
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
		final var result = new SessionFactoryObserver[first.length + second.length];
		System.arraycopy( first, 0, result, 0, first.length );
		System.arraycopy( second, 0, result, first.length, second.length );
		return result;
	}
}
