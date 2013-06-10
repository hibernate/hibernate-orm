/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	public EntityConfiguration(String versionsEntityName, String entityClassName, IdMappingData idMappingData,
							   ExtendedPropertyMapper propertyMapper, String parentEntityName) {
		this.versionsEntityName = versionsEntityName;
		this.entityClassName = entityClassName;
		this.idMappingData = idMappingData;
		this.propertyMapper = propertyMapper;
		this.parentEntityName = parentEntityName;

		this.relations = new HashMap<String, RelationDescription>();
	}

	public void addToOneRelation(String fromPropertyName, String toEntityName, IdMapper idMapper, boolean insertable,
								 boolean ignoreNotFound) {
		relations.put(
				fromPropertyName,
				RelationDescription.toOne(
						fromPropertyName, RelationType.TO_ONE, toEntityName, null, idMapper, null,
						null, insertable, ignoreNotFound
				)
		);
	}

	public void addToOneNotOwningRelation(String fromPropertyName, String mappedByPropertyName,
										  String toEntityName, IdMapper idMapper, boolean ignoreNotFound) {
		relations.put(
				fromPropertyName,
				RelationDescription.toOne(
						fromPropertyName, RelationType.TO_ONE_NOT_OWNING, toEntityName, mappedByPropertyName,
						idMapper, null, null, true, ignoreNotFound
				)
		);
	}

	public void addToManyNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName,
										   IdMapper idMapper, PropertyMapper fakeBidirectionalRelationMapper,
										   PropertyMapper fakeBidirectionalRelationIndexMapper) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName, RelationType.TO_MANY_NOT_OWNING, toEntityName, mappedByPropertyName,
						idMapper, fakeBidirectionalRelationMapper, fakeBidirectionalRelationIndexMapper, true
				)
		);
	}

	public void addToManyMiddleRelation(String fromPropertyName, String toEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName, RelationType.TO_MANY_MIDDLE, toEntityName, null, null, null, null, true
				)
		);
	}

	public void addToManyMiddleNotOwningRelation(String fromPropertyName, String mappedByPropertyName, String toEntityName) {
		relations.put(
				fromPropertyName,
				RelationDescription.toMany(
						fromPropertyName, RelationType.TO_MANY_MIDDLE_NOT_OWNING, toEntityName, mappedByPropertyName,
						null, null, null, true
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
