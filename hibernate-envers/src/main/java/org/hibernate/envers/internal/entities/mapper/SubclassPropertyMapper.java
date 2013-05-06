/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * A mapper which maps from a parent mapper and a "main" one, but adds only to the "main". The "main" mapper
 * should be the mapper of the subclass.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class SubclassPropertyMapper implements ExtendedPropertyMapper {
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
			AuditConfiguration verCfg,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		parentMapper.mapToEntityFromMap( verCfg, obj, data, primaryKey, versionsReader, revision );
		main.mapToEntityFromMap( verCfg, obj, data, primaryKey, versionsReader, revision );
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session, String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
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
	public CompositeMapperBuilder addComponent(PropertyData propertyData, Class componentClass) {
		return main.addComponent( propertyData, componentClass );
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
		final Map<PropertyData, PropertyMapper> joinedProperties = new HashMap<PropertyData, PropertyMapper>();
		joinedProperties.putAll( parentMapper.getProperties() );
		joinedProperties.putAll( main.getProperties() );
		return joinedProperties;
	}
}
