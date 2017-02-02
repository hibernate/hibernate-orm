/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessStrategyMapImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyMapImpl INSTANCE = new PropertyAccessStrategyMapImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
		return new PropertyAccessMapImpl( this, propertyName );
	}
}
