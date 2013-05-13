/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache.management.impl;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Utility class used for getting {@link javax.management.ObjectName}'s for ehcache hibernate MBeans
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public abstract class EhcacheHibernateMbeanNames {

	/**
	 * Group id for all sampled mbeans registered
	 */
	public static final String GROUP_ID = "net.sf.ehcache.hibernate";

	/**
	 * Type for the ehcache backed hibernate second level cache statistics mbean
	 */
	public static final String EHCACHE_HIBERNATE_TYPE = "EhcacheHibernateStats";

	/**
	 * Filter out invalid ObjectName characters from s.
	 *
	 * @param s the name to be filtered out
	 *
	 * @return A valid JMX ObjectName attribute value.
	 */
	public static String mbeanSafe(String s) {
		return s == null ? "" : s.replaceAll( ":|=|\n", "." );
	}

	/**
	 * Returns an ObjectName for the passed name
	 *
	 * @param cacheManagerClusterUUID the UUID of the cacheManager within the cluster
	 * @param name the name to use, which should be made "mbean safe"
	 *
	 * @return An {@link javax.management.ObjectName} using the input name of cache manager
	 *
	 * @throws javax.management.MalformedObjectNameException The name derived from the params does not correspond to a valid ObjectName
	 */
	public static ObjectName getCacheManagerObjectName(String cacheManagerClusterUUID, String name)
			throws MalformedObjectNameException {
		return new ObjectName(
				GROUP_ID + ":type=" + EHCACHE_HIBERNATE_TYPE
						+ ",name=" + mbeanSafe( name ) + getBeanNameSuffix( cacheManagerClusterUUID )
		);
	}

	private static String getBeanNameSuffix(String cacheManagerClusterUUID) {
		String suffix = "";
		if ( !isBlank( cacheManagerClusterUUID ) ) {
			suffix = ",node=" + cacheManagerClusterUUID;
		}
		return suffix;
	}

	private static boolean isBlank(String param) {
		return param == null || "".equals( param.trim() );
	}
}
