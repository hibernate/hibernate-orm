/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache.management.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.hibernate.stat.QueryStatistics;


/**
 * Represent point in time state of the query stats of a given query
 *
 * @author gkeim
 * @author Alex Snaps
 */
public class QueryStats implements Serializable {
	private static final String COMPOSITE_TYPE_NAME = "QueryStats";
	private static final String COMPOSITE_TYPE_DESCRIPTION = "Statistics per Query";
	private static final String[] ITEM_NAMES = new String[] {
			"query", "cacheHitCount",
			"cacheMissCount",
			"cachePutCount",
			"executionCount",
			"executionRowCount",
			"executionAvgTime",
			"executionMaxTime",
			"executionMinTime",
	};
	private static final String[] ITEM_DESCRIPTIONS = new String[] {
			"query", "cacheHitCount",
			"cacheMissCount",
			"cachePutCount",
			"executionCount",
			"executionRowCount",
			"executionAvgTime",
			"executionMaxTime",
			"executionMinTime",
	};
	private static final OpenType[] ITEM_TYPES = new OpenType[] {
			SimpleType.STRING, SimpleType.LONG,
			SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
			SimpleType.LONG,
	};
	private static final CompositeType COMPOSITE_TYPE;
	private static final String TABULAR_TYPE_NAME = "Statistics by Query";
	private static final String TABULAR_TYPE_DESCRIPTION = "All Query Statistics";
	private static final String[] INDEX_NAMES = new String[] {"query",};
	private static final TabularType TABULAR_TYPE;

	static {
		try {
			COMPOSITE_TYPE = new CompositeType(
					COMPOSITE_TYPE_NAME, COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES,
					ITEM_DESCRIPTIONS, ITEM_TYPES
			);
			TABULAR_TYPE = new TabularType( TABULAR_TYPE_NAME, TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES );
		}
		catch (OpenDataException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * query
	 */
	protected final String query;

	/**
	 * cacheHitCount
	 */
	protected long cacheHitCount;

	/**
	 * cacheMissCount
	 */
	protected long cacheMissCount;

	/**
	 * cachePutCount
	 */
	protected long cachePutCount;

	/**
	 * executionCount
	 */
	protected long executionCount;

	/**
	 * executionRowCount
	 */
	protected long executionRowCount;

	/**
	 * executionAvgTime
	 */
	protected long executionAvgTime;

	/**
	 * executionMaxTime
	 */
	protected long executionMaxTime;

	/**
	 * executionMinTime
	 */
	protected long executionMinTime;

	/**
	 * Constructor
	 * @param name the query string
	 */
	public QueryStats(String name) {
		this.query = name;
	}

	/**
	 * Constructor
	 * @param name the query string
	 * @param src the source of the stats to this query
	 */
	public QueryStats(String name, QueryStatistics src) {
		this( name );

		try {
			this.cacheHitCount = BeanUtils.getLongBeanProperty( src, "cacheHitCount" );
			this.cacheMissCount = BeanUtils.getLongBeanProperty( src, "cacheMissCount" );
			this.cachePutCount = BeanUtils.getLongBeanProperty( src, "cachePutCount" );
			this.executionCount = BeanUtils.getLongBeanProperty( src, "executionCount" );
			this.executionRowCount = BeanUtils.getLongBeanProperty( src, "executionRowCount" );
			this.executionAvgTime = BeanUtils.getLongBeanProperty( src, "executionAvgTime" );
			this.executionMaxTime = BeanUtils.getLongBeanProperty( src, "executionMaxTime" );
			this.executionMinTime =
					BeanUtils.getLongBeanProperty( src, "executionMinTime" );
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException( "Exception retrieving statistics", e );
		}
	}

	/**
	 * Constructor
	 * @param cData CompositeDate to construct an instance from
	 */
	@SuppressWarnings("UnusedAssignment")
	public QueryStats(final CompositeData cData) {
		int i = 0;
		query = (String) cData.get( ITEM_NAMES[i++] );
		cacheHitCount = (Long) cData.get( ITEM_NAMES[i++] );
		cacheMissCount = (Long) cData.get( ITEM_NAMES[i++] );
		cachePutCount = (Long) cData.get( ITEM_NAMES[i++] );
		executionCount = (Long) cData.get( ITEM_NAMES[i++] );
		executionRowCount = (Long) cData.get( ITEM_NAMES[i++] );
		executionAvgTime = (Long) cData.get( ITEM_NAMES[i++] );
		executionMaxTime = (Long) cData.get( ITEM_NAMES[i++] );
		executionMinTime = (Long) cData.get( ITEM_NAMES[i++] );
	}

	/**
	 * Adds to the counter of this instance
	 * @param stats the counters to add to these ones
	 */
	public void add(QueryStats stats) {
		cacheHitCount += stats.getCacheHitCount();
		cacheMissCount += stats.getCacheMissCount();
		cachePutCount += stats.getCachePutCount();
		executionCount += stats.getExecutionCount();
		executionRowCount += stats.getExecutionRowCount();
		executionAvgTime += stats.getExecutionAvgTime();
		executionMaxTime += stats.getExecutionMaxTime();
		executionMinTime += stats.getExecutionMinTime();
	}

	@Override
	public String toString() {
		return "query=" + query + ", cacheHitCount=" + cacheHitCount + ", cacheMissCount=" + cacheMissCount
				+ ", cachePutCount=" + cachePutCount + ", executionCount=" + executionCount + ", executionRowCount="
				+ executionRowCount + ", executionAvgTime=" + executionAvgTime + ", executionMaxTime=" + executionMaxTime
				+ ", executionMinTime=" + executionMinTime;
	}

	/**
	 * Accessor to the queryString
	 * @return the query string
	 */
	@SuppressWarnings("UnusedDeclaration")
	public String getQuery() {
		return query;
	}

	/**
	 * The amount of hits for this query
	 * @return the hit count
	 */
	public long getCacheHitCount() {
		return cacheHitCount;
	}

	/**
	 * The amount of misses for this query
	 * @return the miss count
	 */
	public long getCacheMissCount() {
		return cacheMissCount;
	}

	/**
	 * The amount of puts for this query
	 * @return the put count
	 */
	public long getCachePutCount() {
		return cachePutCount;
	}

	/**
	 * The amount of execution of this query
	 * @return the execution count
	 */
	public long getExecutionCount() {
		return executionCount;
	}

	/**
	 * The amount of rows returned for this query
	 * @return the row count
	 */
	public long getExecutionRowCount() {
		return executionRowCount;
	}

	/**
	 * The avg time to execute this query
	 * @return the avg time in ms
	 */
	public long getExecutionAvgTime() {
		return executionAvgTime;
	}

	/**
	 * The max time to execute this query
	 * @return the max time in ms
	 */
	public long getExecutionMaxTime() {
		return executionMaxTime;
	}

	/**
	 * The minimum time to execute this query
	 * @return the min time in ms
	 */
	public long getExecutionMinTime() {
		return executionMinTime;
	}

	/**
	 * Creates a CompositeData instance of this instance
	 * @return the compositeData representation of this instance
	 */
	public CompositeData toCompositeData() {
		try {
			return new CompositeDataSupport(
					COMPOSITE_TYPE, ITEM_NAMES, new Object[] {
					query, cacheHitCount, cacheMissCount,
					cachePutCount,
					executionCount,
					executionRowCount,
					executionAvgTime,
					executionMaxTime,
					executionMinTime,
			}
			);
		}
		catch (OpenDataException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Creates a new TabularData
	 * @return a new TabularData instance
	 */
	public static TabularData newTabularDataInstance() {
		return new TabularDataSupport( TABULAR_TYPE );
	}

	/**
	 * Reads an array of queryStats from TabularData
	 * @param tabularData the tabularData with the {@link CompositeData} of stats to extract
	 * @return all queryStats as an array
	 */
	@SuppressWarnings({"unchecked", "UnusedDeclaration"})
	public static QueryStats[] fromTabularData(final TabularData tabularData) {
		final List<QueryStats> countList = new ArrayList( tabularData.size() );
		for ( Object o : tabularData.values() ) {
			countList.add( new QueryStats( (CompositeData) o ) );
		}
		return countList.toArray( new QueryStats[countList.size()] );
	}
}
