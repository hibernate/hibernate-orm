/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hibernate.envers.internal.tools.Tools.newHashMap;

/**
 * Audit mapping meta-data for component.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 * @author Chris Cranford
 */
public class ComponentAuditingData extends PropertyAuditingData implements AuditedPropertiesHolder {
	private final Map<String, PropertyAuditingData> properties;

	public ComponentAuditingData() {
		this.properties = newHashMap();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData) {
		properties.put( propertyName, auditingData );
	}

	@Override
	public PropertyAuditingData getPropertyAuditingData(String propertyName) {
		return properties.get( propertyName );
	}

	@Override
	public boolean contains(String propertyName) {
		return properties.containsKey( propertyName );
	}

	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	public Iterable<String> getNonSyntheticPropertyNames() {
		return properties.entrySet().stream()
				.filter( e -> !e.getValue().isSynthetic() )
				.map( Map.Entry::getKey )
				.collect( Collectors.toList() );
	}

}
