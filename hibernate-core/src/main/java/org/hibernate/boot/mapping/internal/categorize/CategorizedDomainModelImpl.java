/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.models.spi.ClassDetails;

/// Internal categorized domain model.
///
/// @since 9.0
/// @author Steve Ebersole
public record CategorizedDomainModelImpl(
		Set<EntityHierarchyImpl> entityHierarchies,
		Map<String, ClassDetails> sourceClasses,
		Map<String, ClassDetails> mappedSuperclasses,
		Map<String, EmbeddableTypeMetadataImpl> embeddables,
		GlobalRegistrations globalRegistrations) implements CategorizedDomainModel {
	public CategorizedDomainModelImpl {
		entityHierarchies = Collections.unmodifiableSet( new LinkedHashSet<>( entityHierarchies ) );
		sourceClasses = Collections.unmodifiableMap( new LinkedHashMap<>( sourceClasses ) );
		mappedSuperclasses = Collections.unmodifiableMap( new LinkedHashMap<>( mappedSuperclasses ) );
		embeddables = Collections.unmodifiableMap( new LinkedHashMap<>( embeddables ) );
	}

	@Override
	public Set<EntityHierarchyImpl> getEntityHierarchies() {
		return entityHierarchies;
	}

	@Override
	public Map<String, ClassDetails> getSourceClasses() {
		return sourceClasses;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	@Override
	public Map<String, EmbeddableTypeMetadataImpl> getEmbeddables() {
		return embeddables;
	}

	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	public void forEachEntityHierarchy(IndexedConsumer<EntityHierarchyImpl> hierarchyConsumer) {
		int position = 0;
		for ( EntityHierarchyImpl entityHierarchy : entityHierarchies ) {
			hierarchyConsumer.accept( position++, entityHierarchy );
		}
	}

	public void forEachMappedSuperclass(KeyedConsumer<String, ClassDetails> consumer) {
		mappedSuperclasses.forEach( consumer::accept );
	}

	public void forEachEmbeddable(KeyedConsumer<String, EmbeddableTypeMetadataImpl> consumer) {
		embeddables.forEach( consumer::accept );
	}
}
