/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
