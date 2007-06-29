//$Id$
package org.hibernate.jdbc;

import org.hibernate.Interceptor;


/**
 * Factory for <tt>Batcher</tt> instances.
 * @author Gavin King
 */
public interface BatcherFactory {
	public Batcher createBatcher(ConnectionManager connectionManager, Interceptor interceptor);
}
