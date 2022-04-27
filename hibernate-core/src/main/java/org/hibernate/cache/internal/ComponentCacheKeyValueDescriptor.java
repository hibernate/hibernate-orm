/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class ComponentCacheKeyValueDescriptor implements CacheKeyValueDescriptor {

	private SessionFactoryImplementor sessionFactory;
	private NavigableRole role;
	private Type[] propertyTypes;
	private CompositeUserType<Object> compositeUserType;
	private int propertySpan;

	public ComponentCacheKeyValueDescriptor(
			SessionFactoryImplementor sessionFactory,
			NavigableRole role,
			Type[] propertyTypes,
			CompositeUserType<Object> compositeUserType,
			int propertySpan) {
		this.sessionFactory = sessionFactory;
		this.role = role;
		this.propertyTypes = propertyTypes;
		this.compositeUserType = compositeUserType;
		this.propertySpan = propertySpan;
	}

	@Override
	public int getHashCode(Object key) {
		if ( compositeUserType != null ) {
			return compositeUserType.hashCode( key );
		}
		int result = 17;
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = getPropertyValue( key, i );
			result *= 37;
			if ( y != null ) {
				result += propertyTypes[i].getHashCode( y );
			}
		}
		return result;
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		if ( key1 == key2 ) {
			return true;
		}
		if ( compositeUserType != null ) {
			return compositeUserType.equals( key1, key2 );
		}
		// null value and empty component are considered equivalent
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( getPropertyValue( key1, i ), getPropertyValue( key2, i ) ) ) {
				return false;
			}
		}
		return true;
	}

	public Object getPropertyValue(Object component, int i) {
		if ( component == null ) {
			return null;
		}

		if ( component instanceof Object[] ) {
			// A few calls to hashCode pass the property values already in an
			// Object[] (ex: QueryKey hash codes for cached queries).
			// It's easiest to just check for the condition here prior to
			// trying reflection.
			return ( (Object[]) component )[i];
		}
		else {
			return sessionFactory.getRuntimeMetamodels().getEmbedded( role )
					.getEmbeddableTypeDescriptor()
					.getValue( component, i );
		}
	}
}
