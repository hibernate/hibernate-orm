/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.orchestration.ResolvedMetadata;
import org.hibernate.boot.spi.AbstractDelegatingMetadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static java.lang.String.join;

/// Legacy Metadata view over a resolved metadata product.
///
/// The public Metadata contract remains the compatibility surface, while the
/// retained ResolvedMetadata lets Metadata -> SessionFactory avoid rebuilding
/// categorization and binding products.
public class ResolvedMetadataImplementor extends AbstractDelegatingMetadata {
	private final ResolvedBootstrapSettings bootstrapSettings;
	private final ResolvedMetadata resolvedMetadata;

	public ResolvedMetadataImplementor(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata) {
		super( resolvedMetadata.metadata() );
		this.bootstrapSettings = bootstrapSettings;
		this.resolvedMetadata = resolvedMetadata;
	}

	public ResolvedMetadata getResolvedMetadata() {
		return resolvedMetadata;
	}

	@Override
	public SessionFactoryBuilder getSessionFactoryBuilder() {
		final var defaultBuilder = getFactoryBuilder();
		SessionFactoryBuilder builder = null;
		List<String> activeFactoryNames = null;
		for ( var discoveredBuilderFactory : getSessionFactoryBuilderFactories() ) {
			final SessionFactoryBuilder returnedBuilder =
					discoveredBuilderFactory.getSessionFactoryBuilder( this, defaultBuilder );
			if ( returnedBuilder != null ) {
				if ( activeFactoryNames == null ) {
					activeFactoryNames = new ArrayList<>();
				}
				activeFactoryNames.add( discoveredBuilderFactory.getClass().getName() );
				builder = returnedBuilder;
			}
		}

		if ( activeFactoryNames != null && activeFactoryNames.size() > 1 ) {
			throw new HibernateException(
					"Multiple active SessionFactoryBuilderFactory definitions were discovered: "
							+ join( ", ", activeFactoryNames )
			);
		}

		return builder == null ? defaultBuilder : builder;
	}

	private Iterable<SessionFactoryBuilderFactory> getSessionFactoryBuilderFactories() {
		return getMetadataBuildingOptions()
				.getServiceRegistry()
				.requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class )
				.loadJavaServices( SessionFactoryBuilderFactory.class );
	}

	private SessionFactoryBuilderImplementor getFactoryBuilder() {
		final MetadataImplementor metadata = resolvedMetadata.metadata();
		if ( metadata instanceof MetadataImpl metadataImpl ) {
			return new SessionFactoryBuilderImpl(
					this,
					metadataImpl.getBootstrapContext(),
					bootstrapSettings,
					resolvedMetadata
			);
		}
		return (SessionFactoryBuilderImplementor) delegate().getSessionFactoryBuilder();
	}

	@Override
	public SessionFactoryImplementor buildSessionFactory() {
		return (SessionFactoryImplementor) getSessionFactoryBuilder().build();
	}
}
