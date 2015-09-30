/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Setter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public class ComponentPropertyMapper implements PropertyMapper, CompositeMapperBuilder {
	private final PropertyData propertyData;
	private final MultiPropertyMapper delegate;
	private final Class componentClass;

	public ComponentPropertyMapper(PropertyData propertyData, Class componentClass) {
		this.propertyData = propertyData;
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
	public CompositeMapperBuilder addComponent(PropertyData propertyData, Class componentClass) {
		return delegate.addComponent( propertyData, componentClass );
	}

	@Override
	public void addComposite(PropertyData propertyData, PropertyMapper propertyMapper) {
		delegate.addComposite( propertyData, propertyMapper );
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		return delegate.mapToMapFromEntity( session, data, newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		if ( propertyData.isUsingModifiedFlag() ) {
			data.put(
					propertyData.getModifiedFlagPropertyName(),
					delegate.mapToMapFromEntity( session, new HashMap<String, Object>(), newObj, oldObj )
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
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		if ( data == null || obj == null ) {
			return;
		}

		if ( propertyData.getBeanName() == null ) {
			// If properties are not encapsulated in a component but placed directly in a class
			// (e.g. by applying <properties> tag).
			delegate.mapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
			return;
		}

		final Setter setter = ReflectionTools.getSetter( obj.getClass(), propertyData, enversService.getServiceRegistry() );

		// If all properties are null and single, then the component has to be null also.
		boolean allNullAndSingle = true;
		for ( Map.Entry<PropertyData, PropertyMapper> property : delegate.getProperties().entrySet() ) {
			if ( data.get(
					property.getKey()
							.getName()
			) != null || !( property.getValue() instanceof SinglePropertyMapper ) ) {
				allNullAndSingle = false;
				break;
			}
		}

		if ( allNullAndSingle ) {
			// single property, but default value need not be null, so we'll set it to null anyway
			setter.set( obj, null, null );
		}
		else {
			// set the component
			try {
				final Object subObj = ReflectHelper.getDefaultConstructor( componentClass ).newInstance();
				setter.set( obj, subObj, null );
				delegate.mapToEntityFromMap( enversService, subObj, data, primaryKey, versionsReader, revision );
			}
			catch ( Exception e ) {
				throw new AuditException( e );
			}
		}
	}

	@Override
	public List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session, String referencingPropertyName,
			PersistentCollection newColl,
			Serializable oldColl, Serializable id) {
		return delegate.mapCollectionChanges( session, referencingPropertyName, newColl, oldColl, id );
	}

	@Override
	public Map<PropertyData, PropertyMapper> getProperties() {
		return delegate.getProperties();
	}
}
