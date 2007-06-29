//$Id: BatchingBatcherFactory.java 7683 2005-07-29 19:10:20Z maxcsaucdk $
package org.hibernate.jdbc;

import org.hibernate.Interceptor;


/**
 * A BatcherFactory implementation which constructs Batcher instances
 * capable of actually performing batch operations.
 * 
 * @author Gavin King
 */
public class BatchingBatcherFactory implements BatcherFactory {

	public Batcher createBatcher(ConnectionManager connectionManager, Interceptor interceptor) {
		return new BatchingBatcher( connectionManager, interceptor );
	}

}
