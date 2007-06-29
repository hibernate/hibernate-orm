//$Id: EntityStatistics.java 7864 2005-08-11 23:22:52Z oneovthafew $
package org.hibernate.stat;


/**
 * Entity related statistics
 * 
 * @author Gavin King
 */
public class EntityStatistics extends CategorizedStatistics {
	
	EntityStatistics(String name) {
		super(name);
	}

	long loadCount;
	long updateCount;
	long insertCount;
	long deleteCount;
	long fetchCount;
	long optimisticFailureCount;

	public long getDeleteCount() {
		return deleteCount;
	}
	public long getInsertCount() {
		return insertCount;
	}
	public long getLoadCount() {
		return loadCount;
	}
	public long getUpdateCount() {
		return updateCount;
	}
	public long getFetchCount() {
		return fetchCount;
	}
	public long getOptimisticFailureCount() {
		return optimisticFailureCount;
	}

	public String toString() {
		return new StringBuffer()
		    .append("EntityStatistics")
			.append("[loadCount=").append(this.loadCount)
			.append(",updateCount=").append(this.updateCount)
			.append(",insertCount=").append(this.insertCount)
			.append(",deleteCount=").append(this.deleteCount)
			.append(",fetchCount=").append(this.fetchCount)
			.append(",optimisticLockFailureCount=").append(this.optimisticFailureCount)
			.append(']')
			.toString();
	}

}
