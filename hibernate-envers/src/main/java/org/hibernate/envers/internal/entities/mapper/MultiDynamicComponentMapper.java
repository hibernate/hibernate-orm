/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.MapProxyTool;
import org.hibernate.envers.internal.tools.StringTools;

/**
 * Multi mapper for dynamic components (it knows that component is a map, not a class)
 *
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public class MultiDynamicComponentMapper extends MultiPropertyMapper {

	private PropertyData dynamicComponentData;

	public MultiDynamicComponentMapper(PropertyData dynamicComponentData) {
		this.dynamicComponentData = dynamicComponentData;
	}

	@Override
	public boolean mapToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		boolean ret = false;
		for ( Map.Entry<PropertyData, PropertyMapper> entry : properties.entrySet() ) {
			final PropertyData propertyData = entry.getKey();
			final PropertyMapper propertyMapper = entry.getValue();
			if ( newObj == null && oldObj == null ) {
				return false;
			}
			Object newValue = newObj == null ? null : getValue( newObj, propertyData );
			Object oldValue = oldObj == null ? null : getValue( oldObj, propertyData );

			ret |= propertyMapper.mapToMapFromEntity( session, data, newValue, oldValue );
		}

		return ret;
	}

	private Object getValue(Object newObj, PropertyData propertyData) {
		return ( (Map) newObj ).get( propertyData.getBeanName() );
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
			Map<String, PropertyData> propertyDatas = getPropertyDatas();
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
	public void mapModifiedFlagsToMapFromEntity(
			SessionImplementor session,
			Map<String, Object> data,
			Object newObj,
			Object oldObj) {
		for ( Map.Entry<PropertyData, PropertyMapper> entry : properties.entrySet() ) {
			final PropertyData propertyData = entry.getKey();
			final PropertyMapper propertyMapper = entry.getValue();
			if ( newObj == null && oldObj == null ) {
				return;
			}
			Object newValue = newObj == null ? null : getValue( newObj, propertyData );
			Object oldValue = oldObj == null ? null : getValue( oldObj, propertyData );
			propertyMapper.mapModifiedFlagsToMapFromEntity( session, data, newValue, oldValue );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void mapToEntityFromMap(
			EnversService enversService,
			Object obj,
			Map data,
			Object primaryKey,
			AuditReaderImplementor versionsReader,
			Number revision) {
		Object mapProxy = MapProxyTool.newInstanceOfBeanProxyForMap(
				generateClassName( data, dynamicComponentData.getBeanName() ),
				(Map) obj,
				properties.keySet(),
				enversService.getClassLoaderService()
		);
		for ( PropertyMapper mapper : properties.values() ) {
			mapper.mapToEntityFromMap( enversService, mapProxy, data, primaryKey, versionsReader, revision );
		}
	}

	private String generateClassName(Map data, String dynamicComponentPropertyName) {
		return ( data.get( "$type$" ) + StringTools.capitalizeFirst( dynamicComponentPropertyName ) ).replaceAll(
				"_",
				""
		);
	}

}
