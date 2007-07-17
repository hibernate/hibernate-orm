//$Id: BatcherFactory.java 7683 2005-07-29 19:10:20Z maxcsaucdk $
package org.hibernate.jdbc;

import org.hibernate.Interceptor;


/**
 * Factory for <tt>Batcher</tt> instances.
 * @author Gavin King
 */
public interface BatcherFactory {
	public Batcher createBatcher(ConnectionManager connectionManager, Interceptor interceptor);
}
