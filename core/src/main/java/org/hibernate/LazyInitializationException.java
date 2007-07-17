//$Id: LazyInitializationException.java 4458 2004-08-29 09:59:17Z oneovthafew $
package org.hibernate;

import org.apache.commons.logging.LogFactory;

/**
 * Indicates access to unfetched data outside of a session context.
 * For example, when an uninitialized proxy or collection is accessed 
 * after the session was closed.
 *
 * @see Hibernate#initialize(java.lang.Object)
 * @see Hibernate#isInitialized(java.lang.Object)
 * @author Gavin King
 */
public class LazyInitializationException extends HibernateException {

	public LazyInitializationException(String msg) {
		super(msg);
		LogFactory.getLog(LazyInitializationException.class).error(msg, this);
	}

}






