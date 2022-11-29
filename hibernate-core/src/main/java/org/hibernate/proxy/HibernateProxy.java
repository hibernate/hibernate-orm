/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;
import java.io.Serializable;

import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;

/**
 * Marker interface for entity proxies
 *
 * @author Gavin King
 */
public interface HibernateProxy extends Serializable, PrimeAmongSecondarySupertypes {

	/**
	 * Extract the LazyInitializer from the object, if
	 * and only if the object is actually an HibernateProxy.
	 * If not, null is returned.
	 * @param object any entity
	 * @return either null (if object is not an HibernateProxy) or the LazyInitializer of the HibernateProxy.
	 */
	static LazyInitializer extractLazyInitializer(final Object object) {
		if ( object instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) object;
			final HibernateProxy hibernateProxy = t.asHibernateProxy();
			if ( hibernateProxy != null ) {
				return hibernateProxy.getHibernateLazyInitializer();
			}
		}
		return null;
	}

	/**
	 * Perform serialization-time write-replacement of this proxy.
	 *
	 * @return The serializable proxy replacement.
	 */
	Object writeReplace();

	/**
	 * Get the underlying lazy initialization handler.
	 *
	 * @return The lazy initializer.
	 */
	LazyInitializer getHibernateLazyInitializer();

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default HibernateProxy asHibernateProxy() {
		return this;
	}
}
