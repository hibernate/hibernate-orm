/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.property.access.spi.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 * @author Chris Cranford
 */
public class ComponentPropertyMapper extends AbstractPropertyMapper implements CompositeMapperBuilder {
	private final PropertyData propertyData;
	private final MultiPropertyMapper delegate;
	private final Class componentClass;
	private final EmbeddableInstantiator embeddableInstantiator;

	public ComponentPropertyMapper(
			PropertyData propertyData,
			Class componentClass,
			EmbeddableInstantiator instantiator) {
		this.propertyData = propertyData;
		this.embeddableInstantiator = instantiator;
		//if class is a map it means that this is dynamic component
		if ( Map.class.isAssignableFrom( componentClass ) ) {
			this.delegate = new MultiDynamicComponentMapper( propertyData );
			this.componentClass = HashMap.class;
		}
		else {
			this.delegate = new MultiPropertyMapper();
			this.componentClass = componentClass;
		}
	}

	@Override
	public void add(PropertyData propertyData) {
		delegate.add( propertyData );
	}

	@Override
	public CompositeMapperBuilder addComponent(
			PropertyData propertyData,
			Class componentClass,
			EmbeddableInstantiator instantiator) {
		return delegate.addComponent( propertyData, componentClass, instantiator );
	}

	@Override
	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
		delegate.addComposite( propertyData, propertyMapper );
	}

	@Override
	public boolean mapToMapFromEntity(
			SharedSessionContractImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		return delegate.mapToMapFromEntity( session, data, newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SharedSessionContractImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		if ( propertyData.isUsingModifiedFlag() ) {
			data.put(
					propertyData.getModifiedFlagPropertyName(),
					delegate.mapToMapFromEntity( session, new HashMap<>(), newObj, oldObj )
			);
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		if ( propertyData.isUsingModifiedFlag() ) {
			boolean hasModifiedCollection = false;
			for ( PropertyData propData : delegate.getProperties().keySet() ) {
				if ( collectionPropertyName.equals( propData.getName() ) ) {
					hasModifiedCollection = true;
					break;
				}
			}
			data.put( propertyData.getModifiedFlagPropertyName(), hasModifiedCollection );
		}
	}

	@Override
	public void mapToEntityFromMap(
			final EnversService enversService,
			final Object obj,
			final Map data,
			final Object primaryKey,
			final AuditReaderImplementor versionsReader,
			final Number revision) {
		if ( data == null || obj == null ) {
			return;
		}

		if ( propertyData.getBeanName() == null ) {
			// If properties are not encapsulated in a component but placed directly in a class
			// (e.g. by applying <properties> tag).
			delegate.mapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
			return;
		}

		try {

			if ( isDynamicComponentMap() ) {
				final Object subObj = ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
				( (Map) obj ).put( propertyData.getBeanName(), subObj );
				delegate.mapToEntityFromMap( enversService, subObj, data, primaryKey, versionsReader, revision );
			}
			else {
				final Setter setter = ReflectionTools.getSetter(
						obj.getClass(),
						propertyData,
						enversService.getServiceRegistry()
				);

				if ( isAllPropertiesNull( data ) ) {
					// single property, but default value need not be null, so we'll set it to null anyway
					setter.set( obj, null );
				}
				else {
					final Object subObj;
					if ( embeddableInstantiator != null ) {
						final Object[] values = new Object[delegate.properties.size()];
						int i = 0;
						for ( Map.Entry<PropertyData, PropertyMapper> entry : delegate.properties.entrySet() ) {
							values[i] = entry.getValue().mapToEntityFromMap(
									enversService,
									data,
									primaryKey,
									versionsReader,
									revision
							);
							i++;
						}
						subObj = embeddableInstantiator.instantiate( () -> values );
					}
					else {
						subObj = ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
						delegate.mapToEntityFromMap(
								enversService,
								subObj,
								data,
								primaryKey,
								versionsReader,
								revision
						);
					}
					// set the component
					setter.set( obj, subObj );
				}
			}
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}

	@Override
	public Object mapToEntityFromMap(
			final EnversService enversService,
			final Map data,
			final Object primaryKey,
			final AuditReaderImplementor versionsReader,
			final Number revision) {
		if ( data == null || propertyData.getBeanName() == null ) {
			// If properties are not encapsulated in a component but placed directly in a class
			// (e.g. by applying <properties> tag).
			return null;
		}

		try {
			final Object subObj;
			if ( isDynamicComponentMap() ) {
				subObj = ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
				delegate.mapToEntityFromMap( enversService, subObj, data, primaryKey, versionsReader, revision );
			}
			else {
				if ( isAllPropertiesNull( data ) ) {
					// single property, but default value need not be null, so we'll set it to null anyway
					subObj = null;
				}
				else {
					if ( embeddableInstantiator != null ) {
						final Object[] values = new Object[delegate.properties.size()];
						int i = 0;
						for ( Map.Entry<PropertyData, PropertyMapper> entry : delegate.properties.entrySet() ) {
							values[i] = entry.getValue().mapToEntityFromMap(
									enversService,
									data,
									primaryKey,
									versionsReader,
									revision
							);
							i++;
						}
						subObj = embeddableInstantiator.instantiate( () -> values );
					}
					else {
						subObj = ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
						delegate.mapToEntityFromMap(
								enversService,
								subObj,
								data,
								primaryKey,
								versionsReader,
								revision
						);
					}
				}
			}
			return subObj;
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}

	private boolean isAllPropertiesNull(Map data) {
		for ( Map.Entry<PropertyData, PropertyMapper> property : delegate.getProperties().entrySet() ) {
			final Object value = data.get( property.getKey().getName() );
			if ( value != null || !( property.getValue() instanceof SinglePropertyMapper ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SharedSessionContractImplementor session,
			String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl,
			Object id) {
		return delegate.mapCollectionChanges( session, referencingPropertyName, newColl, oldColl, id );
	}

	@Override
	public Map<PropertyData, PropertyMapper> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		return delegate.hasPropertiesWithModifiedFlag();
	}
}
