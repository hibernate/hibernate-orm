/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * Information about all of the bytecode lazy attributes for an entity
 *
 * @author Steve Ebersole
 */
public class LazyAttributesMetadata implements Serializable {
	/**
	 * Build a LazyFetchGroupMetadata based on the attributes defined for the
	 * PersistentClass
	 *
	 * @param mappedEntity The entity definition
	 *
	 * @return The built LazyFetchGroupMetadata
	 */
	public static LazyAttributesMetadata from(PersistentClass mappedEntity) {
		final Map<String, LazyAttributeDescriptor> lazyAttributeDescriptorMap = new LinkedHashMap<>();
		final Map<String, Set<String>> fetchGroupToAttributesMap = new HashMap<>();

		int i = -1;
		int x = 0;
		final Iterator itr = mappedEntity.getPropertyClosureIterator();
		while ( itr.hasNext() ) {
			i++;
			final Property property = (Property) itr.next();
			if ( property.isLazy() ) {
				final LazyAttributeDescriptor lazyAttributeDescriptor = LazyAttributeDescriptor.from( property, i, x++ );
				lazyAttributeDescriptorMap.put( lazyAttributeDescriptor.getName(), lazyAttributeDescriptor );

				final Set<String> attributeSet = fetchGroupToAttributesMap.computeIfAbsent(
						lazyAttributeDescriptor.getFetchGroupName(),
						k -> new LinkedHashSet<>()
				);
				attributeSet.add( lazyAttributeDescriptor.getName() );
			}
		}

		if ( lazyAttributeDescriptorMap.isEmpty() ) {
			return new LazyAttributesMetadata( mappedEntity.getEntityName() );
		}

		for ( Map.Entry<String, Set<String>> entry : fetchGroupToAttributesMap.entrySet() ) {
			entry.setValue( Collections.unmodifiableSet( entry.getValue() ) );
		}

		return new LazyAttributesMetadata(
				mappedEntity.getEntityName(),
				Collections.unmodifiableMap( lazyAttributeDescriptorMap ),
				Collections.unmodifiableMap( fetchGroupToAttributesMap )
		);
	}

	public static LazyAttributesMetadata nonEnhanced(String entityName) {
		return new LazyAttributesMetadata( entityName );
	}

	private final String entityName;

	private final Map<String, LazyAttributeDescriptor> lazyAttributeDescriptorMap;
	private final Map<String,Set<String>> fetchGroupToAttributeMap;

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
		return lazyAttributeDescriptorMap.keySet();
	}

	public Set<String> getFetchGroupNames() {
		return fetchGroupToAttributeMap.keySet();
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
		final List<LazyAttributeDescriptor> list = new ArrayList<LazyAttributeDescriptor>();
		for ( String attributeName : fetchGroupToAttributeMap.get( groupName ) ) {
			list.add( lazyAttributeDescriptorMap.get( attributeName ) );
		}
		return list;
	}

	public Set<String> getAttributesInSameFetchGroup(String attributeName) {
		final String fetchGroupName = getFetchGroupName( attributeName );
		return getAttributesInFetchGroup( fetchGroupName );
	}
}
