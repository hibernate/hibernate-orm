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
 * Defines a strategy for accessing property values via a get/set pair, which may be nonpublic.  This
 * is the default (and recommended) strategy.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PropertyAccessStrategyBasicImpl implements PropertyAccessStrategy {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyBasicImpl INSTANCE = new PropertyAccessStrategyBasicImpl();

	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, final String propertyName) {
		return new PropertyAccessBasicImpl( this, containerJavaType, propertyName );
	}
}
