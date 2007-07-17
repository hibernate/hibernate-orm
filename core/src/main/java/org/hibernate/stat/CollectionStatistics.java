//$Id: CollectionStatistics.java 7093 2005-06-09 06:33:06Z oneovthafew $
package org.hibernate.stat;


/**
 * Collection related statistics
 * 
 * @author Gavin King
 */
public class CollectionStatistics extends CategorizedStatistics {
	
	CollectionStatistics(String role) {
		super(role);
	}
	
	long loadCount;
	long fetchCount;
	long updateCount;
	long removeCount;
	long recreateCount;
	
	public long getLoadCount() {
		return loadCount;
	}
	public long getFetchCount() {
		return fetchCount;
	}
	public long getRecreateCount() {
		return recreateCount;
	}
	public long getRemoveCount() {
		return removeCount;
	}
	public long getUpdateCount() {
		return updateCount;
	}

	public String toString() {
		return new StringBuffer()
		    .append("CollectionStatistics")
			.append("[loadCount=").append(this.loadCount)
			.append(",fetchCount=").append(this.fetchCount)
			.append(",recreateCount=").append(this.recreateCount)
			.append(",removeCount=").append(this.removeCount)
			.append(",updateCount=").append(this.updateCount)
			.append(']')
			.toString();
	}
}