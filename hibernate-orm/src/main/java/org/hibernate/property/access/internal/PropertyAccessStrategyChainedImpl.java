/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

/**
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyChainedImpl implements PropertyAccessStrategy {
	private final PropertyAccessStrategy[] chain;

	public PropertyAccessStrategyChainedImpl(PropertyAccessStrategy... chain) {
		this.chain = chain;
	}

	@Override
	public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName) {
		for ( PropertyAccessStrategy candidate : chain ) {
			try {
				return candidate.buildPropertyAccess( containerJavaType, propertyName );
			}
			catch (Exception ignore) {
				// ignore
			}
		}

		throw new PropertyNotFoundException( "Could not resolve PropertyAccess for " + propertyName + " on " + containerJavaType );
	}
}
