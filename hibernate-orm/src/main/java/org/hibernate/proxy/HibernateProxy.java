/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;
import java.io.Serializable;

/**
 * Marker interface for entity proxies
 *
 * @author Gavin King
 */
public interface HibernateProxy extends Serializable {
	/**
	 * Perform serialization-time write-replacement of this proxy.
	 *
	 * @return The serializable proxy replacement.
	 */
	public Object writeReplace();

	/**
	 * Get the underlying lazy initialization handler.
	 *
	 * @return The lazy initializer.
	 */
	public LazyInitializer getHibernateLazyInitializer();
}
