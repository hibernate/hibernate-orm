// $Id: StandardQueryCacheFactory.java 4690 2004-10-26 09:35:46Z oneovthafew $
package org.hibernate.cache;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * Standard Hibernate implementation of the QueryCacheFactory interface.  Returns
 * instances of {@link StandardQueryCache}.
 */
public class StandardQueryCacheFactory implements QueryCacheFactory {

	public QueryCache getQueryCache(
	        final String regionName,
	        final UpdateTimestampsCache updateTimestampsCache,
	        final Settings settings,
	        final Properties props) 
	throws HibernateException {
		return new StandardQueryCache(settings, props, updateTimestampsCache, regionName);
	}

}
