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
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.envers.tools.Pair;
import org.hibernate.property.Getter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public class MultiPropertyMapper implements ExtendedPropertyMapper {
	protected final Map<PropertyData, PropertyMapper> properties;
	private final Map<String, PropertyData> propertyDatas;

	public MultiPropertyMapper() {
		properties = Tools.newHashMap();
		propertyDatas = Tools.newHashMap();
	}

	@Override
	public void add(PropertyData propertyData) {
		final SinglePropertyMapper single = new SinglePropertyMapper();
		single.add( propertyData );
		properties.put( propertyData, single );
		propertyDatas.put( propertyData.getName(), propertyData );
	}

	@Override
	public CompositeMapperBuilder addComponent(PropertyData propertyData, Class componentClass) {
		if ( properties.get( propertyData ) != null ) {
			// This is needed for second pass to work properly in the components mapper
			return (CompositeMapperBuilder) properties.get( propertyData );
		}

		final ComponentPropertyMapper componentMapperBuilder = new ComponentPropertyMapper(
				propertyData,
				componentClass
		);
		addComposite( propertyData, componentMapperBuilder );

		return componentMapperBuilder;
	}

	@Override
	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
		properties.put( propertyData, propertyMapper );
		propertyDatas.put( propertyData.getName(), propertyData );
	}

	protected Object getAtIndexOrNull(Object[] array, int index) {
		return array == null ? null : array[index];
	}

	@Override
	public boolean map(
			SessionImplementor session,
			Map<String, Object> data,
			String[] propertyNames,
			Object[] newState,
			Object[] oldState) {
		boolean ret = false;
		for ( int i = 0; i < propertyNames.length; i++ ) {
			final String propertyName = propertyNames[i];

			if ( propertyDatas.containsKey( propertyName ) ) {
				final PropertyMapper propertyMapper = properties.get( propertyDatas.get( propertyName ) );
				final Object newObj = getAtIndexOrNull( newState, i );
				final Object oldObj = getAtIndexOrNull( oldState, i );
				ret |= propertyMapper.mapToMapFromEntity( session, data, newObj, oldObj );
				propertyMapper.mapModifiedFlagsToMapFromEntity( session, data, newObj, oldObj );
			}
		}

		return ret;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		boolean ret = false;
		for ( PropertyData propertyData : properties.keySet() ) {
			Getter getter;
			if ( newObj != null ) {
				getter = ReflectionTools.getGetter( newObj.getClass(), propertyData );
			}
			else if ( oldObj != null ) {
				getter = ReflectionTools.getGetter( oldObj.getClass(), propertyData );
			}
			else {
				return false;
			}

			ret |= properties.get( propertyData ).mapToMapFromEntity(
					session, data,
					newObj == null ? null : getter.get( newObj ),
					oldObj == null ? null : getter.get( oldObj )
			);
		}

		return ret;
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		for ( PropertyData propertyData : properties.keySet() ) {
			Getter getter;
			if ( newObj != null ) {
				getter = ReflectionTools.getGetter( newObj.getClass(), propertyData );
			}
			else if ( oldObj != null ) {
				getter = ReflectionTools.getGetter( oldObj.getClass(), propertyData );
			}
			else {
				return;
			}

			properties.get( propertyData ).mapModifiedFlagsToMapFromEntity(
					session, data,
					newObj == null ? null : getter.get( newObj ),
					oldObj == null ? null : getter.get( oldObj )
			);
		}
	}

	@Override
	public void mapToEntityFromMap(
			AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
			AuditReaderImplementor versionsReader, Number revision) {
		for ( PropertyMapper mapper : properties.values() ) {
			mapper.mapToEntityFromMap( verCfg, obj, data, primaryKey, versionsReader, revision );
		}
	}

	private Pair<PropertyMapper, String> getMapperAndDelegatePropName(String referencingPropertyName) {
		// Name of the property, to which we will delegate the mapping.
		String delegatePropertyName;

		// Checking if the property name doesn't reference a collection in a component - then the name will containa a .
		final int dotIndex = referencingPropertyName.indexOf( '.' );
		if ( dotIndex != -1 ) {
			// Computing the name of the component
			final String componentName = referencingPropertyName.substring( 0, dotIndex );
			// And the name of the property in the component
			final String propertyInComponentName = MappingTools.createComponentPrefix( componentName )
					+ referencingPropertyName.substring( dotIndex + 1 );

			// We need to get the mapper for the component.
			referencingPropertyName = componentName;
			// As this is a component, we delegate to the property in the component.
			delegatePropertyName = propertyInComponentName;
		}
		else {
			// If this is not a component, we delegate to the same property.
			delegatePropertyName = referencingPropertyName;
		}
		return Pair.make( properties.get( propertyDatas.get( referencingPropertyName ) ), delegatePropertyName );
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		final Pair<PropertyMapper, String> pair = getMapperAndDelegatePropName( collectionPropertyName );
		final PropertyMapper mapper = pair.getFirst();
		if ( mapper != null ) {
			mapper.mapModifiedFlagsToMapForCollectionChange( pair.getSecond(), data );
		}
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
		final Pair<PropertyMapper, String> pair = getMapperAndDelegatePropName( referencingPropertyName );
		final PropertyMapper mapper = pair.getFirst();
		if ( mapper != null ) {
			return mapper.mapCollectionChanges( session, pair.getSecond(), newColl, oldColl, id );
		}
		else {
			return null;
		}
	}

	@Override
	public Map<PropertyData, PropertyMapper> getProperties() {
		return properties;
	}

	public Map<String, PropertyData> getPropertyDatas() {
		return propertyDatas;
	}
}
