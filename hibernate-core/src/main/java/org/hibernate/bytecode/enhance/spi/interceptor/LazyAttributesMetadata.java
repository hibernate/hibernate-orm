/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallSet;

/**
 * Information about the bytecode lazy attributes for an entity
 *
 * @author Steve Ebersole
 */
public class LazyAttributesMetadata implements Serializable {

	/**
	 * Build a LazyFetchGroupMetadata based on the attributes defined for the
	 * PersistentClass
	 */
	public static LazyAttributesMetadata from(
			PersistentClass mappedEntity,
			boolean isEnhanced,
			boolean collectionsInDefaultFetchGroupEnabled,
			Metadata metadata) {
		final Map<String, LazyAttributeDescriptor> lazyAttributeDescriptorMap = new LinkedHashMap<>();
		final Map<String, Set<String>> fetchGroupToAttributesMap = new HashMap<>();

		int x = 0;
		final var properties = mappedEntity.getPropertyClosure();
		for ( int i=0; i<properties.size(); i++ ) {
			final var property = properties.get(i);
			final boolean lazy = ! EnhancementHelper.includeInBaseFetchGroup(
					property,
					isEnhanced,
					entityName -> {
						final var entityBinding = metadata.getEntityBinding( entityName );
						assert entityBinding != null;
						return entityBinding.hasSubclasses();
					},
					collectionsInDefaultFetchGroupEnabled
			);
			if ( lazy ) {
				final var lazyAttributeDescriptor = LazyAttributeDescriptor.from( property, i, x++ );
				lazyAttributeDescriptorMap.put( lazyAttributeDescriptor.getName(), lazyAttributeDescriptor );

				final var attributeSet = fetchGroupToAttributesMap.computeIfAbsent(
						lazyAttributeDescriptor.getFetchGroupName(),
						k -> new LinkedHashSet<>()
				);
				attributeSet.add( lazyAttributeDescriptor.getName() );
			}
		}

		if ( lazyAttributeDescriptorMap.isEmpty() ) {
			return new LazyAttributesMetadata( mappedEntity.getEntityName() );
		}

		for ( var entry : fetchGroupToAttributesMap.entrySet() ) {
			entry.setValue( unmodifiableSet( entry.getValue() ) );
		}

		return new LazyAttributesMetadata(
				mappedEntity.getEntityName(),
				unmodifiableMap( lazyAttributeDescriptorMap ),
				unmodifiableMap( fetchGroupToAttributesMap )
		);
	}

	public static LazyAttributesMetadata nonEnhanced(String entityName) {
		return new LazyAttributesMetadata( entityName );
	}

	private final String entityName;

	private final Map<String, LazyAttributeDescriptor> lazyAttributeDescriptorMap;
	private final Map<String,Set<String>> fetchGroupToAttributeMap;
	private final Set<String> fetchGroupNames;
	private final Set<String> lazyAttributeNames;

	public LazyAttributesMetadata(String entityName) {
		this( entityName, Collections.emptyMap(), Collections.emptyMap() );
	}

	public LazyAttributesMetadata(
			String entityName,
			Map<String, LazyAttributeDescriptor> lazyAttributeDescriptorMap,
			Map<String, Set<String>> fetchGroupToAttributeMap) {
		this.entityName = entityName;
		this.lazyAttributeDescriptorMap = lazyAttributeDescriptorMap;
		this.fetchGroupToAttributeMap = fetchGroupToAttributeMap;
		this.fetchGroupNames = toSmallSet( unmodifiableSet( fetchGroupToAttributeMap.keySet() ) );
		this.lazyAttributeNames = toSmallSet( unmodifiableSet( lazyAttributeDescriptorMap.keySet() ) );
	}

	public String getEntityName() {
		return entityName;
	}

	public boolean hasLazyAttributes() {
		return !lazyAttributeDescriptorMap.isEmpty();
	}

	public int lazyAttributeCount() {
		return lazyAttributeDescriptorMap.size();
	}

	public Set<String> getLazyAttributeNames() {
		return lazyAttributeNames;
	}

	/**
	 * @return an immutable set
	 */
	public Set<String> getFetchGroupNames() {
		return fetchGroupNames;
	}

	public boolean isLazyAttribute(String attributeName) {
		return lazyAttributeDescriptorMap.containsKey( attributeName );
	}

	public String getFetchGroupName(String attributeName) {
		return lazyAttributeDescriptorMap.get( attributeName ).getFetchGroupName();
	}

	public Set<String> getAttributesInFetchGroup(String fetchGroupName) {
		return fetchGroupToAttributeMap.get( fetchGroupName );
	}

	public List<LazyAttributeDescriptor> getFetchGroupAttributeDescriptors(String groupName) {
		final var attributeNames = fetchGroupToAttributeMap.get( groupName );
		final List<LazyAttributeDescriptor> list = new ArrayList<>( attributeNames.size() );
		for ( String attributeName : attributeNames ) {
			list.add( lazyAttributeDescriptorMap.get( attributeName ) );
		}
		return list;
	}
}
