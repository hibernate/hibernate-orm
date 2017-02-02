/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.internal.entities.mapper.ExtendedPropertyMapper;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Chris Cranford
 */
public class EntityConfiguration {
	private String versionsEntityName;
	/**
	 * Holds the className for instantiation the configured entity
	 */
	private String entityClassName;
	private IdMappingData idMappingData;
	private ExtendedPropertyMapper propertyMapper;
	// Maps from property name
	private Map<String, RelationDescription> relations;
	private String parentEntityName;

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
						fromPropertyName, RelationType.TO_ONE, toEntityName, null, idMapper, null,
						null, insertable, ignoreNotFound
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
						fromPropertyName, RelationType.TO_ONE_NOT_OWNING, toEntityName, mappedByPropertyName,
						idMapper, null, null, true, ignoreNotFound
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
						fromPropertyName, RelationType.TO_MANY_NOT_OWNING, toEntityName, mappedByPropertyName,
						idMapper, fakeBidirectionalRelationMapper, fakeBidirectionalRelationIndexMapper, true, indexed
				)
		);
	}

	public void addToManyMiddleRelation(String fromPropertyName, String toEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName, RelationType.TO_MANY_MIDDLE, toEntityName, null, null, null, null, true, false
				)
		);
	}

	public void addToManyMiddleNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName, RelationType.TO_MANY_MIDDLE_NOT_OWNING, toEntityName, mappedByPropertyName,
						null, null, null, true, false
				)
		);
	}

	public boolean isRelation(String propertyName) {
		return relations.get( propertyName ) != null;
	}

	public RelationDescription getRelationDescription(String propertyName) {
		return relations.get( propertyName );
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
