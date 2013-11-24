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

import org.hibernate.stat.CollectionStatistics;

/**
 * CollectionStats
 *
 * @author gkeim
 */
public class CollectionStats implements Serializable {
	private static final String COMPOSITE_TYPE_NAME = "CollectionsStats";
	private static final String COMPOSITE_TYPE_DESCRIPTION = "Statistics per Collections";
	private static final String[] ITEM_NAMES = new String[] {
			"roleName", "shortName", "loadCount",
			"fetchCount", "updateCount", "removeCount", "recreateCount",
	};
	private static final String[] ITEM_DESCRIPTIONS = new String[] {
			"roleName", "shortName", "loadCount",
			"fetchCount", "updateCount", "removeCount", "recreateCount",
	};
	private static final OpenType[] ITEM_TYPES = new OpenType[] {
			SimpleType.STRING,
			SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
	};
	private static final CompositeType COMPOSITE_TYPE;
	private static final String TABULAR_TYPE_NAME = "Statistics by Collection";
	private static final String TABULAR_TYPE_DESCRIPTION = "All Collection Statistics";
	private static final String[] INDEX_NAMES = new String[] {"roleName",};
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
	 * roleName
	 */
	protected final String roleName;

	/**
	 * shortName
	 */
	protected final String shortName;

	/**
	 * loadCount
	 */
	protected long loadCount;

	/**
	 * fetchCount
	 */
	protected long fetchCount;

	/**
	 * updateCount
	 */
	protected long updateCount;

	/**
	 * removeCount
	 */
	protected long removeCount;

	/**
	 * recreateCount
	 */
	protected long recreateCount;


	/**
	 * Constructs a CollectionsStats
	 *
	 * @param role The collection role
	 */
	public CollectionStats(String role) {
		this.roleName = role;
		this.shortName = CacheRegionUtils.determineShortName( role );
	}

	/**
	 * Constructs a CollectionsStats
	 *
	 * @param role The collection role
	 * @param src The CollectionStatistics instance
	 */
	public CollectionStats(String role, CollectionStatistics src) {
		this( role );

		try {
			this.loadCount = BeanUtils.getLongBeanProperty( src, "loadCount" );
			this.fetchCount = BeanUtils.getLongBeanProperty( src, "fetchCount" );
			this.updateCount = BeanUtils.getLongBeanProperty( src, "updateCount" );
			this.removeCount = BeanUtils.getLongBeanProperty( src, "removeCount" );
			this.recreateCount = BeanUtils.getLongBeanProperty( src, "recreateCount" );
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException( "Exception retrieving statistics", e );
		}
	}

	/**
	 * Constructs a CollectionsStats from a JMX CompositeData
	 *
	 * @param cData The JMX CompositeData
	 */
	@SuppressWarnings("UnusedAssignment")
	public CollectionStats(final CompositeData cData) {
		int i = 0;
		roleName = (String) cData.get( ITEM_NAMES[i++] );
		shortName = (String) cData.get( ITEM_NAMES[i++] );
		loadCount = (Long) cData.get( ITEM_NAMES[i++] );
		fetchCount = (Long) cData.get( ITEM_NAMES[i++] );
		updateCount = (Long) cData.get( ITEM_NAMES[i++] );
		removeCount = (Long) cData.get( ITEM_NAMES[i++] );
		recreateCount = (Long) cData.get( ITEM_NAMES[i++] );
	}

	/**
	 * Update the internal stats
	 *
	 * @param stats The incoming stats
	 */
	public void add(CollectionStats stats) {
		loadCount += stats.getLoadCount();
		fetchCount += stats.getFetchCount();
		updateCount += stats.getUpdateCount();
		removeCount += stats.getRemoveCount();
		recreateCount += stats.getRecreateCount();
	}

	@Override
	public String toString() {
		return "roleName=" + roleName + "shortName=" + shortName + ", loadCount=" + loadCount + ", fetchCount="
				+ fetchCount + ", updateCount=" + updateCount + ", removeCount=" + removeCount + ", recreateCount"
				+ recreateCount;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getRoleName() {
		return roleName;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getShortName() {
		return shortName;
	}

	public long getLoadCount() {
		return loadCount;
	}

	public long getFetchCount() {
		return fetchCount;
	}

	public long getUpdateCount() {
		return updateCount;
	}

	public long getRemoveCount() {
		return removeCount;
	}

	public long getRecreateCount() {
		return recreateCount;
	}

	/**
	 * Builds a JMX CompositeData view of our state
	 *
	 * @return The JMX CompositeData
	 */
	public CompositeData toCompositeData() {
		try {
			return new CompositeDataSupport(
					COMPOSITE_TYPE,
					ITEM_NAMES,
					new Object[] { roleName, shortName, loadCount, fetchCount, updateCount, removeCount, recreateCount }
			);
		}
		catch (OpenDataException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Builds a JMX TabularData
	 *
	 * @return JMX TabularData
	 */
	public static TabularData newTabularDataInstance() {
		return new TabularDataSupport( TABULAR_TYPE );
	}

	/**
	 * Re-builds CollectionStats from JMX TabularData
	 *
	 * @param tabularData The JMX TabularData
	 *
	 * @return The CollectionsStats
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static CollectionStats[] fromTabularData(final TabularData tabularData) {
		final List<CollectionStats> countList = new ArrayList<CollectionStats>( tabularData.size() );
		for ( Object o : tabularData.values() ) {
			countList.add( new CollectionStats( (CompositeData) o ) );
		}
		return countList.toArray( new CollectionStats[countList.size()] );
	}

}
