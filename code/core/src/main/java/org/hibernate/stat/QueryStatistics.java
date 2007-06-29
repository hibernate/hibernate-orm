//$Id: QueryStatistics.java 9162 2006-01-27 23:40:32Z steveebersole $
package org.hibernate.stat;


/**
 * Query statistics (HQL and SQL)
 * 
 * Note that for a cached query, the cache miss is equals to the db count
 * 
 * @author Gavin King
 */
public class QueryStatistics extends CategorizedStatistics {

	/*package*/ long cacheHitCount;
	/*package*/ long cacheMissCount;
	/*package*/ long cachePutCount;
	private long executionCount;
	private long executionRowCount;
	private long executionAvgTime;
	private long executionMaxTime;
	private long executionMinTime;

	QueryStatistics(String query) {
		super(query);
	}

	/**
	 * queries executed to the DB
	 */
	public long getExecutionCount() {
		return executionCount;
	}
	
	/**
	 * Queries retrieved successfully from the cache
	 */
	public long getCacheHitCount() {
		return cacheHitCount;
	}
	
	public long getCachePutCount() {
		return cachePutCount;
	}
	
	public long getCacheMissCount() {
		return cacheMissCount;
	}
	
	/**
	 * Number of lines returned by all the executions of this query (from DB)
	 * For now, {@link org.hibernate.Query#iterate()} 
	 * and {@link org.hibernate.Query#scroll()()} do not fill this statistic
	 *
	 * @return The number of rows cumulatively returned by the given query; iterate
	 * and scroll queries do not effect this total as their number of returned rows
	 * is not known at execution time.
	 */
	public long getExecutionRowCount() {
		return executionRowCount;
	}

	/**
	 * average time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionAvgTime() {
		return executionAvgTime;
	}

	/**
	 * max time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMaxTime() {
		return executionMaxTime;
	}
	
	/**
	 * min time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMinTime() {
		return executionMinTime;
	}
	
	/**
	 * add statistics report of a DB query
	 * 
	 * @param rows rows count returned
	 * @param time time taken
	 */
	void executed(long rows, long time) {
		if (time < executionMinTime) executionMinTime = time;
		if (time > executionMaxTime) executionMaxTime = time;
		executionAvgTime = ( executionAvgTime * executionCount + time ) / ( executionCount + 1 );
		executionCount++;
		executionRowCount += rows;
	}

	public String toString() {
		return new StringBuffer()
				.append( "QueryStatistics" )
				.append( "[cacheHitCount=" ).append( this.cacheHitCount )
				.append( ",cacheMissCount=" ).append( this.cacheMissCount )
				.append( ",cachePutCount=" ).append( this.cachePutCount )
				.append( ",executionCount=" ).append( this.executionCount )
				.append( ",executionRowCount=" ).append( this.executionRowCount )
				.append( ",executionAvgTime=" ).append( this.executionAvgTime )
				.append( ",executionMaxTime=" ).append( this.executionMaxTime )
				.append( ",executionMinTime=" ).append( this.executionMinTime )
				.append( ']' )
				.toString();
	}

}
