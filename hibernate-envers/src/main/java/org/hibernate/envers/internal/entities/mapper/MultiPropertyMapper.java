/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.envers.tools.Pair;
import org.hibernate.property.access.spi.Getter;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 * @author Chris Cranford
 */
public class MultiPropertyMapper extends AbstractPropertyMapper implements ExtendedPropertyMapper {
	protected final Map<PropertyData, PropertyMapper> properties;
	private final Map<String, PropertyData> propertyDatas;

	public MultiPropertyMapper() {
		properties = Tools.newLinkedHashMap();
		propertyDatas = Tools.newLinkedHashMap();
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
			final SessionImplementor session,
			final Map<String, Object> data,
			final Object newObj,
			final Object oldObj) {
		return AccessController.doPrivileged(
				new PrivilegedAction<Boolean>() {
					@Override
					public Boolean run() {
						boolean ret = false;
						for ( Map.Entry<PropertyData, PropertyMapper> entry : properties.entrySet() ) {
							final PropertyData propertyData = entry.getKey();
							final PropertyMapper propertyMapper = entry.getValue();

							// synthetic properties are not part of the entity model; therefore they should be ignored.
							if ( propertyData.isSynthetic() ) {
								continue;
							}

							Getter getter;
							if ( newObj != null ) {
								getter = ReflectionTools.getGetter(
										newObj.getClass(),
										propertyData,
										session.getFactory().getServiceRegistry()
								);
							}
							else if ( oldObj != null ) {
								getter = ReflectionTools.getGetter(
										oldObj.getClass(),
										propertyData,
										session.getFactory().getServiceRegistry()
								);
							}
							else {
								return false;
							}

							ret |= propertyMapper.mapToMapFromEntity(
									session, data,
									newObj == null ? null : getter.get( newObj ),
									oldObj == null ? null : getter.get( oldObj )
							);
						}
						return ret;
					}
				}
		);
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(
			final SessionImplementor session,
			final Map<String, Object> data,
			final Object newObj,
			final Object oldObj) {
		AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						for ( Map.Entry<PropertyData, PropertyMapper> entry : properties.entrySet() ) {
							final PropertyData propertyData = entry.getKey();
							final PropertyMapper propertyMapper = entry.getValue();

							// synthetic properties are not part of the entity model; therefore they should be ignored.
							if ( propertyData.isSynthetic() ) {
								continue;
							}

							Getter getter;
							if ( newObj != null ) {
								getter = ReflectionTools.getGetter(
										newObj.getClass(),
										propertyData,
										session.getFactory().getServiceRegistry()
								);
							}
							else if ( oldObj != null ) {
								getter = ReflectionTools.getGetter(
										oldObj.getClass(),
										propertyData,
										session.getFactory().getServiceRegistry()
								);
							}
							else {
								break;
							}

							propertyMapper.mapModifiedFlagsToMapFromEntity(
									session, data,
									newObj == null ? null : getter.get( newObj ),
									oldObj == null ? null : getter.get( oldObj )
							);
						}

						return null;
					}
				}
		);
	}

	@Override
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		for ( PropertyMapper mapper : properties.values() ) {
			mapper.mapToEntityFromMap( enversService, obj, data, primaryKey, versionsReader, revision );
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

	@Override
	public boolean hasPropertiesWithModifiedFlag() {
		for ( PropertyData property : getProperties().keySet() ) {
			if ( property.isUsingModifiedFlag() ) {
				return true;
			}
		}
		return false;
	}
}
