/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Arrays;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Legacy Metadata view over a resolved metadata product.
///
/// The public Metadata contract remains the compatibility surface, while the
/// retained ResolvedMetadata lets Metadata -> SessionFactory avoid rebuilding
/// categorization and binding products.
public class ResolvedMetadataImplementor extends AbstractDelegatingMetadata {
	private final ResolvedBootstrapSettings bootstrapSettings;
	private final ResolvedMetadata resolvedMetadata;
	private final SessionFactoryObserver[] additionalSessionFactoryObservers;

	public ResolvedMetadataImplementor(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata) {
		this( bootstrapSettings, resolvedMetadata, new SessionFactoryObserver[0] );
	}

	public ResolvedMetadataImplementor(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata,
			SessionFactoryObserver... additionalSessionFactoryObservers) {
		super( resolvedMetadata.metadata() );
		this.bootstrapSettings = bootstrapSettings;
		this.resolvedMetadata = resolvedMetadata;
		this.additionalSessionFactoryObservers = additionalSessionFactoryObservers == null
				? new SessionFactoryObserver[0]
				: additionalSessionFactoryObservers.clone();
	}

	public ResolvedMetadata getResolvedMetadata() {
		return resolvedMetadata;
	}

	@Override
	public SessionFactoryImplementor buildSessionFactory() {
		final MetadataImplementor metadata = resolvedMetadata.metadata();
		if ( metadata instanceof MetadataImpl metadataImpl ) {
			final var serviceRegistry = metadataImpl.getMetadataBuildingOptions().getServiceRegistry();
			return SessionFactoryPipeline.build(
					SettingsResolver.resolveSessionFactorySettings( bootstrapSettings, serviceRegistry ),
					resolvedMetadata,
					serviceRegistry,
					Arrays.copyOf( additionalSessionFactoryObservers, additionalSessionFactoryObservers.length )
			);
		}
		return delegate().buildSessionFactory();
	}
}
