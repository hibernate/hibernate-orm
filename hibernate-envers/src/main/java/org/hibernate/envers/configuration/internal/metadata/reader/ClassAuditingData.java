/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.envers.AuditTable;
import org.hibernate.mapping.PersistentClass;

/**
 * Boot-time audit data for a specific entity class.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Hern&aacut;n Chanfreau
 * @author Chris Cranford
 */
public class ClassAuditingData implements AuditedPropertiesHolder {

	private final PersistentClass persistentClass;
	private final Map<String, PropertyAuditingData> properties;
	private final Map<String, String> secondaryTableDictionary;

	private AuditTable auditTable;

	/**
	 * True if the class is audited globally (this helps to cover the cases when there are no fields in the class,
	 * but it's still audited).
	 */
	private boolean defaultAudited;

	public ClassAuditingData(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
		this.properties = new HashMap<>();
		this.secondaryTableDictionary = new HashMap<>();
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
	public List<AuditOverrideData> getAuditingOverrides() {
		return Collections.emptyList();
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	public Iterable<String> getPropertyNames() {
		return properties.keySet();
	}

	public Map<String, String> getSecondaryTableDictionary() {
		return secondaryTableDictionary;
	}

	public AuditTable getAuditTable() {
		return auditTable;
	}

	public void setAuditTable(AuditTable auditTable) {
		this.auditTable = auditTable;
	}

	public void setDefaultAudited(boolean defaultAudited) {
		this.defaultAudited = defaultAudited;
	}

	public boolean isAudited() {
		return defaultAudited || properties.size() > 0;
	}

	@Override
	public boolean contains(String propertyName) {
		return properties.containsKey( propertyName );
	}

	public Iterable<String> getNonSyntheticPropertyNames() {
		return properties.entrySet().stream()
				.filter( e -> !e.getValue().isSynthetic() )
				.map( Map.Entry::getKey )
				.collect( Collectors.toList() );
	}

	public Iterable<PropertyAuditingData> getSyntheticProperties() {
		return properties.values().stream()
				.filter( PropertyAuditingData::isSynthetic )
				.collect( Collectors.toList() );
	}
}
