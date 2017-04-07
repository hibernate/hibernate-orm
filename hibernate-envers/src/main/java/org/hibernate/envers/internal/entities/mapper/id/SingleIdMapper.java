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
import java.util.List;
import java.util.Map;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SingleIdMapper extends AbstractIdMapper implements SimpleIdMapperBuilder {
	private PropertyData propertyData;

	public SingleIdMapper(ServiceRegistry serviceRegistry) {
		super( serviceRegistry );
	}

	public SingleIdMapper(ServiceRegistry serviceRegistry, PropertyData propertyData) {
		this( serviceRegistry );
		this.propertyData = propertyData;
	}

	@Override
	public void add(PropertyData propertyData) {
		if ( this.propertyData != null ) {
			throw new AuditException( "Only one property can be added!" );
		}

		this.propertyData = propertyData;
	}

	@Override
	public boolean mapToEntityFromMap(final Object obj, Map data) {
		if ( data == null || obj == null ) {
			return false;
		}

		final Object value = data.get( propertyData.getName() );
		if ( value == null ) {
			return false;
		}

		return AccessController.doPrivileged(
				new PrivilegedAction<Boolean>() {
					@Override
					public Boolean run() {
						final Setter setter = ReflectionTools.getSetter(
								obj.getClass(),
								propertyData,
								getServiceRegistry()
						);
						setter.set( obj, value, null );
						return true;
					}
				}
		);
	}

	@Override
	public Object mapToIdFromMap(Map data) {
		if ( data == null ) {
			return null;
		}

		return data.get( propertyData.getName() );
	}

	@Override
	public Object mapToIdFromEntity(final Object data) {
		if ( data == null ) {
			return null;
		}

		if ( data instanceof HibernateProxy ) {
			final HibernateProxy hibernateProxy = (HibernateProxy) data;
			return hibernateProxy.getHibernateLazyInitializer().getIdentifier();
		}
		else {
			return AccessController.doPrivileged(
					new PrivilegedAction<Object>() {
						@Override
						public Object run() {
							final Getter getter = ReflectionTools.getGetter(
									data.getClass(),
									propertyData,
									getServiceRegistry()
							);
							return getter.get( data );
						}
					}
			);
		}
	}

	@Override
	public void mapToMapFromId(Map<String, Object> data, Object obj) {
		if ( data != null ) {
			data.put( propertyData.getName(), obj );
		}
	}

	@Override
	public void mapToMapFromEntity(Map<String, Object> data, final Object obj) {
		if ( obj == null ) {
			data.put( propertyData.getName(), null );
		}
		else {
			if ( obj instanceof HibernateProxy ) {
				final HibernateProxy hibernateProxy = (HibernateProxy) obj;
				data.put( propertyData.getName(), hibernateProxy.getHibernateLazyInitializer().getIdentifier() );
			}
			else {
				final Object value = AccessController.doPrivileged(
						new PrivilegedAction<Object>() {
							@Override
							public Object run() {
								final Getter getter = ReflectionTools.getGetter(
										obj.getClass(),
										propertyData,
										getServiceRegistry()
								);
								return getter.get( obj );
							}
						}
				);
				data.put( propertyData.getName(), value );
			}
		}
	}

	public void mapToEntityFromEntity(final Object objTo, final Object objFrom) {
		if ( objTo == null || objFrom == null ) {
			return;
		}

		AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						final Getter getter = ReflectionTools.getGetter(
								objFrom.getClass(),
								propertyData,
								getServiceRegistry()
						);

						final Setter setter = ReflectionTools.getSetter(
								objTo.getClass(),
								propertyData,
								getServiceRegistry()
						);

						setter.set( objTo, getter.get( objFrom ), null );
						return null;
					}
				}
		);
	}

	@Override
	public IdMapper prefixMappedProperties(String prefix) {
		return new SingleIdMapper( getServiceRegistry(), new PropertyData( prefix + propertyData.getName(), propertyData ) );
	}

	@Override
	public List<QueryParameterData> mapToQueryParametersFromId(Object obj) {
		final List<QueryParameterData> ret = new ArrayList<>();

		ret.add( new QueryParameterData( propertyData.getName(), obj ) );

		return ret;
	}
}
