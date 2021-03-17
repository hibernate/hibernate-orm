/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.id;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EmbeddedIdMapper extends AbstractCompositeIdMapper implements SimpleIdMapperBuilder {
	private PropertyData idPropertyData;

	public EmbeddedIdMapper(PropertyData idPropertyData, Class compositeIdClass, ServiceRegistry serviceRegistry) {
		super( compositeIdClass, serviceRegistry );

		this.idPropertyData = idPropertyData;
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		for ( IdMapper idMapper : ids.values() ) {
			idMapper.mapToMapFromEntity( data, obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, final Object obj) {
		if ( obj == null ) {
			return;
		}

		final Object value = AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						final Getter getter = ReflectionTools.getGetter(
								obj.getClass(),
								idPropertyData,
								getServiceRegistry()
						);
						return getter.get( obj );
					}
				}
		);

		mapToMapFromId( data, value );
	}

	@Override
	public boolean mapToEntityFromMap(final Object obj, final Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		return AccessController.doPrivileged(
				new PrivilegedAction<Boolean>() {
					@Override
					public Boolean run() {
						final Setter setter = ReflectionTools.getSetter( obj.getClass(), idPropertyData, getServiceRegistry() );

						try {
							final Object subObj = instantiateCompositeId();

							boolean ret = true;
							for ( IdMapper idMapper : ids.values() ) {
								ret &= idMapper.mapToEntityFromMap( subObj, data );
							}

							if ( ret ) {
								setter.set( obj, subObj, null );
							}

							return ret;
						}
						catch (Exception e) {
							throw new AuditException( e );
						}
					}
				}
		);
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		final EmbeddedIdMapper ret = new EmbeddedIdMapper( idPropertyData, compositeIdClass, getServiceRegistry() );

		for ( PropertyData propertyData : ids.keySet() ) {
			final String propertyName = propertyData.getName();
			ret.ids.put( propertyData, new SingleIdMapper( getServiceRegistry(), new PropertyData( prefix + propertyName, propertyData ) ) );
		}

		return ret;
	}

	@Override
	public Object mapToIdFromEntity(final Object data) {
		if ( data == null ) {
			return null;
		}

		return AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						final Getter getter = ReflectionTools.getGetter(
								data.getClass(),
								idPropertyData,
								getServiceRegistry()
						);
						return getter.get( data );
					}
				}
		);
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final Map<String, Object> data = new LinkedHashMap<>();
		mapToMapFromId( data, obj );

		final List<QueryParameterData> ret = new ArrayList<>();

		for ( Map.Entry<String, Object> propertyData : data.entrySet() ) {
			ret.add( new QueryParameterData( propertyData.getKey(), propertyData.getValue() ) );
		}

		return ret;
	}
}
