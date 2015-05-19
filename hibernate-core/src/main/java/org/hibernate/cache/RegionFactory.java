/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache;

/**
 * Legacy (deprecated) namespace for the RegionFactory contract.
 *
 * @author Steve Ebersole
 *
 * @deprecated Moved, but still need this definition for ehcache 
 */
@Deprecated
public interface RegionFactory extends org.hibernate.cache.spi.RegionFactory {
}
