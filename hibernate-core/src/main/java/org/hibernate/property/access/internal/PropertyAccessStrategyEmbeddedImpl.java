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
 * A PropertyAccessStrategy that deals with non-aggregated composites.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyEmbeddedImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyEmbeddedImpl INSTANCE = new PropertyAccessStrategyEmbeddedImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
		return new PropertyAccessEmbeddedImpl( this, containerJavaType, propertyName );
	}
}
