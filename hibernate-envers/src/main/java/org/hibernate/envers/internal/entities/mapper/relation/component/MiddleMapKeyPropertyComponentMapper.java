/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.component;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.persistence.Embedded;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.property.access.spi.Getter;

/**
 * A component mapper for the @MapKey mapping with the name parameter specified: the value of the map's key
 * is a property of the entity. This doesn't have an effect on the data stored in the versions tables,
 * so <code>mapToMapFromObject</code> is empty.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class MiddleMapKeyPropertyComponentMapper implements MiddleComponentMapper {
	private final String propertyName;
	private final String accessType;

	public MiddleMapKeyPropertyComponentMapper(String propertyName, String accessType) {
		this.propertyName = propertyName;
		this.accessType = accessType;
	}

	@Override
	public Object mapToObjectFromFullMap(
			final EntityInstantiator entityInstantiator,
			final Map<String, Object> data,
			final Object dataObject,
			Number revision) {
		// dataObject is not null, as this mapper can only be used in an index.
		return AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					@Override
					public Object run() {
						Class clazz = dataObject.getClass();
						if ( isNestedProperty( propertyName ) ) {
							clazz = getEmbeddedType( clazz, propertyName );
							int index = propertyName.indexOf( "." );
							String suffix = propertyName.substring( 0, index );
							final Getter getter = ReflectionTools.getGetter(
									clazz,
									suffix,
									accessType,
									entityInstantiator.getEnversService().getServiceRegistry() );
							return getter.get( dataObject );
						}
					 	final Getter getter = ReflectionTools.getGetter(
								clazz,
								propertyName,
								accessType,
								entityInstantiator.getEnversService().getServiceRegistry()
						);
						return getter.get( dataObject );
					}
				}
		);
	}
	
		
	private boolean isNestedProperty(String fieldName) {
		return fieldName != null && fieldName.contains( "." );
	}

	private Class getEmbeddedType(Class cls, String fieldName) {
		int index = fieldName.indexOf( "." );
		String prefix = fieldName.substring( 0, index );
		if ( !ReflectionTools.isEmbeddedProperty( cls, prefix ) ) {
			return cls;
		}
		try {
			return cls.getDeclaredField( fieldName ).getType();
		}
		catch (NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	@Override
	public void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object obj) {
		// Doing nothing.
	}

	@Override
	public void addMiddleEqualToQuery(
			Parameters parameters,
			String idPrefix1,
			String prefix1,
			String idPrefix2,
			String prefix2) {
		// Doing nothing.
	}
}
