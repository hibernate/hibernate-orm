/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * A mapper which maps from a parent mapper and a "main" one, but adds only to the "main". The "main" mapper
 * should be the mapper of the subclass.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class SubclassPropertyMapper extends AbstractPropertyMapper implements ExtendedPropertyMapper {
	private ExtendedPropertyMapper main;
	private ExtendedPropertyMapper parentMapper;

	public SubclassPropertyMapper(ExtendedPropertyMapper main, ExtendedPropertyMapper parentMapper) {
		this.main = main;
		this.parentMapper = parentMapper;
	}

	@Override
	public boolean map(
			SessionImplementor session,
			Map<String, Object> data,
			String[] propertyNames,
			Object[] newState,
			Object[] oldState) {
		final boolean parentDiffs = parentMapper.map( session, data, propertyNames, newState, oldState );
		final boolean mainDiffs = main.map( session, data, propertyNames, newState, oldState );

		return parentDiffs || mainDiffs;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		final boolean parentDiffs = parentMapper.mapToMapFromEntity( session, data, newObj, oldObj );
		final boolean mainDiffs = main.mapToMapFromEntity( session, data, newObj, oldObj );

		return parentDiffs || mainDiffs;
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		parentMapper.mapModifiedFlagsToMapFromEntity( session, data, newObj, oldObj );
		main.mapModifiedFlagsToMapFromEntity( session, data, newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		parentMapper.mapModifiedFlagsToMapForCollectionChange( collectionPropertyName, data );
		main.mapModifiedFlagsToMapForCollectionChange( collectionPropertyName, data );
	}

	@Override
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		parentMapper.mapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
		main.mapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
	}

	@Override
	public Object mapToEntityFromMap(
			EnversService enversService,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		return null;
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session, String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Object id) {
		final List<PersistentCollectionChangeData> parentCollectionChanges = parentMapper.mapCollectionChanges(
				session,
				referencingPropertyName,
				newColl,
				oldColl,
				id
		);

		final List<PersistentCollectionChangeData> mainCollectionChanges = main.mapCollectionChanges(
				session,
				referencingPropertyName,
				newColl,
				oldColl,
				id
		);

		if ( parentCollectionChanges == null ) {
			return mainCollectionChanges;
		}
		else {
			if ( mainCollectionChanges != null ) {
				parentCollectionChanges.addAll( mainCollectionChanges );
			}
			return parentCollectionChanges;
		}
	}

	@Override
	public CompositeMapperBuilder addComponent(
			PropertyData propertyData,
			Class componentClass, EmbeddableInstantiator instantiator) {
		return main.addComponent( propertyData, componentClass, instantiator );
	}

	@Override
	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
		main.addComposite( propertyData, propertyMapper );
	}

	@Override
	public void add(PropertyData propertyData) {
		main.add( propertyData );
	}

	@Override
	public Map<PropertyData, PropertyMapper> getProperties() {
		final Map<PropertyData, PropertyMapper> joinedProperties = new HashMap<>();
		joinedProperties.putAll( parentMapper.getProperties() );
		joinedProperties.putAll( main.getProperties() );
		return joinedProperties;
	}

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		// checks all properties, exposed both by the main mapper and parent mapper.
		for ( PropertyData property : getProperties().keySet() ) {
			if ( property.isUsingModifiedFlag() ) {
				return true;
			}
		}
		return false;
	}

}
