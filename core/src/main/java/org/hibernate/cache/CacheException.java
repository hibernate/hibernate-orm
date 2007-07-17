//$Id: CacheException.java 11492 2007-05-09 01:57:11Z steve.ebersole@jboss.com $
package org.hibernate.cache;

import org.hibernate.HibernateException;

/**
 * Something went wrong in the cache
 */
public class CacheException extends HibernateException {
	
	public CacheException(String s) {
		super(s);
	}

	public CacheException(String s, Throwable e) {
		super(s, e);
	}
	
	public CacheException(Throwable e) {
		super(e);
	}
	
}
