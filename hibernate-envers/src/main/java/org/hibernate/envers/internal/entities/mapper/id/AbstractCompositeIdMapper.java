/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.ServiceRegistry;

/**
 * An abstract identifier mapper implementation specific for composite identifiers.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class AbstractCompositeIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	protected final Class<?> compositeIdClass;

	protected Map<PropertyData, AbstractIdMapper> ids;

	protected AbstractCompositeIdMapper(Class<?> compositeIdClass, ServiceRegistry serviceRegistry) {
		super( serviceRegistry );
		this.compositeIdClass = compositeIdClass;
		ids = Tools.newLinkedHashMap();
	}

	@Override
	public void add(PropertyData propertyData) {
		add( propertyData, new SingleIdMapper( getServiceRegistry(), propertyData ) );
	}

	@Override
	public void add(PropertyData propertyData, AbstractIdMapper idMapper) {
		ids.put( propertyData, idMapper );
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		if ( compositeIdClass.isRecord() ) {
			return mapToRecordFromMap( data );
		}

		final Object compositeId = instantiateCompositeId();
		for ( AbstractIdMapper mapper : ids.values() ) {
			if ( !mapper.mapToEntityFromMap( compositeId, data ) ) {
				return null;
			}
		}

		return compositeId;
	}

	protected Object mapToRecordFromMap(Map data) {
		final var map = new HashMap<String, Object>();
		for ( AbstractIdMapper mapper : ids.values() ) {
			if ( !(mapper instanceof SingleIdMapper single && single.mapToMapFromMap( map, data )) ) {
				return null;
			}
		}
		return recordFromMap( map );
	}

	protected Object recordFromMap(Map<String, Object> map) {
		final var recordComponents = compositeIdClass.getRecordComponents();
		final var constructoryTypes = new Class[recordComponents.length];
		final var constructoryParameters = new Object[recordComponents.length];
		for ( var i = 0; i < recordComponents.length; ++i ) {
			constructoryTypes[i] = recordComponents[i].getType();
			if ( !map.containsKey( recordComponents[i].getName() ) ) {
				return null;
			}
			constructoryParameters[i] = map.get( recordComponents[i].getName() );
		}
		try {
			final Constructor<?> idClassConstructor = compositeIdClass.getConstructor( constructoryTypes );
			return idClassConstructor.newInstance( constructoryParameters );
		}
		catch (ReflectiveOperationException e) {
			return null;
		}
	}

	@Override
	public void mapToEntityFromEntity(Object objectTo, Object objectFrom) {
		// no-op; does nothing
	}

	protected Object instantiateCompositeId() {
		try {
			return ReflectHelper.getDefaultConstructor( compositeIdClass ).newInstance();
		}
		catch ( Exception e ) {
			throw new AuditException( e );
		}
	}
}
