/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.configuration.internal.metadata.reader.ComponentAuditingData;
import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;

/**
 * Runtime representation of an entity that may or may not be audited.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Chris Cranford
 */
public class EntityConfiguration {
	private final String versionsEntityName;
	/**
	 * Holds the className for instantiation the configured entity
	 */
	private final String entityClassName;
	private final IdMappingData idMappingData;
	private final ExtendedPropertyMapper propertyMapper;
	// Maps from property name
	private final Map<String, RelationDescription> relations;
	private final Map<String, ComponentDescription> components;
	private final String parentEntityName;

	public EntityConfiguration(
			String versionsEntityName,
			String entityClassName,
			IdMappingData idMappingData,
			ExtendedPropertyMapper propertyMapper,
			String parentEntityName) {
		this.versionsEntityName = versionsEntityName;
		this.entityClassName = entityClassName;
		this.idMappingData = idMappingData;
		this.propertyMapper = propertyMapper;
		this.parentEntityName = parentEntityName;

		this.relations = new HashMap<>();
		this.components = new HashMap<>();
	}

	public void addToOneRelation(
			String fromPropertyName,
			String toEntityName,
			IdMapper idMapper,
			boolean insertable,
			boolean ignoreNotFound) {
		relations.put(
				fromPropertyName,
				RelationDescription.toOne(
						fromPropertyName,
						RelationType.TO_ONE,
						toEntityName,
						null,
						idMapper,
						null,
						null,
						insertable,
						ignoreNotFound
				)
		);
	}

	public void addToOneNotOwningRelation(
			String fromPropertyName,
			String mappedByPropertyName,
			String toEntityName,
			IdMapper idMapper,
			boolean ignoreNotFound) {
		relations.put(
				fromPropertyName,
				RelationDescription.toOne(
						fromPropertyName,
						RelationType.TO_ONE_NOT_OWNING,
						toEntityName,
						mappedByPropertyName,
						idMapper,
						null,
						null,
						true,
						ignoreNotFound
				)
		);
	}

	public void addToManyNotOwningRelation(
			String fromPropertyName,
			String mappedByPropertyName,
			String toEntityName,
			IdMapper idMapper,
			PropertyMapper fakeBidirectionalRelationMapper,
			PropertyMapper fakeBidirectionalRelationIndexMapper,
			boolean indexed) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName,
						RelationType.TO_MANY_NOT_OWNING,
						toEntityName,
						mappedByPropertyName,
						idMapper,
						fakeBidirectionalRelationMapper,
						fakeBidirectionalRelationIndexMapper,
						null,
						null,
						null,
						true,
						indexed
				)
		);
	}

	public void addToManyMiddleRelation(
			String fromPropertyName,
			String toEntityName,
			MiddleIdData referencingIdData,
			MiddleIdData referencedIdData,
			String auditMiddleEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName,
						RelationType.TO_MANY_MIDDLE,
						toEntityName,
						null,
						null,
						null,
						null,
						referencingIdData,
						referencedIdData,
						auditMiddleEntityName,
						true,
						false
				)
		);
	}

	public void addToManyMiddleNotOwningRelation(
			String fromPropertyName,
			String mappedByPropertyName,
			String toEntityName,
			MiddleIdData referencingIdData,
			MiddleIdData referencedIdData,
			String auditMiddleEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName,
						RelationType.TO_MANY_MIDDLE_NOT_OWNING,
						toEntityName,
						mappedByPropertyName,
						null,
						null,
						null,
						referencingIdData,
						referencedIdData,
						auditMiddleEntityName,
						true,
						false
				)
		);
	}

	public void addToManyComponent(String propertyName, String auditMiddleEntityName, MiddleIdData middleIdData) {
		components.put( propertyName, ComponentDescription.many( propertyName, auditMiddleEntityName, middleIdData ) );
	}

	public void addToOneComponent(String propertyName, ComponentAuditingData auditingData) {
		components.put( propertyName, ComponentDescription.one( propertyName, auditingData ) );
	}

	public boolean isRelation(String propertyName) {
		return relations.get( propertyName ) != null;
	}

	public RelationDescription getRelationDescription(String propertyName) {
		return relations.get( propertyName );
	}

	public ComponentDescription getComponentDescription(String propertyName) {
		return components.get( propertyName );
	}

	public IdMappingData getIdMappingData() {
		return idMappingData;
	}

	public IdMapper getIdMapper() {
		return idMappingData.getIdMapper();
	}

	public ExtendedPropertyMapper getPropertyMapper() {
		return propertyMapper;
	}

	public String getParentEntityName() {
		return parentEntityName;
	}

	/**
	 * @return the className for the configured entity
	 */
	public String getEntityClassName() {
		return entityClassName;
	}

	// For use by EntitiesConfigurations

	String getVersionsEntityName() {
		return versionsEntityName;
	}

	Iterable<RelationDescription> getRelationsIterator() {
		return relations.values();
	}
}
