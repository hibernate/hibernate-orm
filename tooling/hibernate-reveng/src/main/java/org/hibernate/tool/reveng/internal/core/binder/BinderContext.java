/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tool.reveng.api.core.RevengStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class BinderContext {

	public static BinderContext create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		return new BinderContext(
				metadataBuildingContext,
				metadataCollector,
				revengStrategy,
				properties);
	}

	public final MetadataBuildingContext metadataBuildingContext;
	public final InFlightMetadataCollector metadataCollector;
	public final RevengStrategy revengStrategy;
	public final Properties properties;
	private final List<CollectionAssociationFinalizer> collectionAssociationFinalizers = new ArrayList<>();

	private BinderContext(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.properties = properties;
	}

	public void addCollectionAssociationFinalizer(CollectionAssociationFinalizer finalizer) {
		collectionAssociationFinalizers.add( finalizer );
	}

	public void finalizeCollectionAssociations() {
		final var finalizers = List.copyOf( collectionAssociationFinalizers );
		collectionAssociationFinalizers.clear();
		finalizers.forEach( CollectionAssociationFinalizer::finalizeCollectionAssociation );
	}

}
