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
import java.util.Iterator;
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

import org.hibernate.stat.EntityStatistics;

/**
 * When we only support Java 6, all of this OpenMBean scaffolding can be removed in favor or MXBeans.
 *
 * @author gkeim
 */
public class EntityStats implements Serializable {
	private static final String COMPOSITE_TYPE_NAME = "EntityStats";
	private static final String COMPOSITE_TYPE_DESCRIPTION = "Statistics per Entity";
	private static final String[] ITEM_NAMES = new String[] {
			"name", "shortName", "loadCount",
			"updateCount", "insertCount", "deleteCount", "fetchCount", "optimisticFailureCount",
	};
	private static final String[] ITEM_DESCRIPTIONS = new String[] {
			"name", "shortName", "loadCount",
			"updateCount", "insertCount", "deleteCount", "fetchCount", "optimisticFailureCount",
	};
	private static final OpenType[] ITEM_TYPES = new OpenType[] {
			SimpleType.STRING,
			SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
			SimpleType.LONG,
	};
	private static final CompositeType COMPOSITE_TYPE;
	private static final String TABULAR_TYPE_NAME = "Statistics by Entity";
	private static final String TABULAR_TYPE_DESCRIPTION = "All Entity Statistics";
	private static final String[] INDEX_NAMES = new String[] { "name", };
	private static final TabularType TABULAR_TYPE;

	static {
		try {
			COMPOSITE_TYPE = new CompositeType(
					COMPOSITE_TYPE_NAME, COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES,
					ITEM_DESCRIPTIONS, ITEM_TYPES
			);
			TABULAR_TYPE = new TabularType( TABULAR_TYPE_NAME, TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES );
		}
		catch ( OpenDataException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * name
	 */
	protected final String name;

	/**
	 * shortName
	 */
	protected final String shortName;

	/**
	 * loadCount
	 */
	protected long loadCount;

	/**
	 * updateCount
	 */
	protected long updateCount;

	/**
	 * insertCount
	 */
	protected long insertCount;

	/**
	 * deleteCount
	 */
	protected long deleteCount;

	/**
	 * fetchCount
	 */
	protected long fetchCount;

	/**
	 * optimisticFailureCount
	 */
	protected long optimisticFailureCount;

	/**
	 * @param name
	 */
	public EntityStats(String name) {
		this.name = name;
		this.shortName = CacheRegionUtils.determineShortName( name );
	}

	/**
	 * @param name
	 * @param src
	 */
	public EntityStats(String name, EntityStatistics src) {
		this( name );

		try {
			this.loadCount = BeanUtils.getLongBeanProperty( src, "loadCount" );
			this.updateCount = BeanUtils.getLongBeanProperty( src, "updateCount" );
			this.insertCount = BeanUtils.getLongBeanProperty( src, "insertCount" );
			this.deleteCount = BeanUtils.getLongBeanProperty( src, "deleteCount" );
			this.fetchCount = BeanUtils.getLongBeanProperty( src, "fetchCount" );
			this.optimisticFailureCount = BeanUtils.getLongBeanProperty( src, "optimisticFailureCount" );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new RuntimeException( "Exception retrieving statistics", e );
		}
	}

	/**
	 * @param cData
	 */
	public EntityStats(final CompositeData cData) {
		int i = 0;
		name = (String) cData.get( ITEM_NAMES[i++] );
		shortName = (String) cData.get( ITEM_NAMES[i++] );
		loadCount = (Long) cData.get( ITEM_NAMES[i++] );
		updateCount = (Long) cData.get( ITEM_NAMES[i++] );
		insertCount = (Long) cData.get( ITEM_NAMES[i++] );
		deleteCount = (Long) cData.get( ITEM_NAMES[i++] );
		fetchCount = (Long) cData.get( ITEM_NAMES[i++] );
		optimisticFailureCount = (Long) cData.get( ITEM_NAMES[i++] );
	}

	private static int safeParseInt(String s) {
		try {
			return Integer.parseInt( s );
		}
		catch ( Exception e ) {
			return -1;
		}
	}

	/**
	 * @param stats
	 */
	public void add(EntityStats stats) {
		loadCount += stats.getLoadCount();
		updateCount += stats.getUpdateCount();
		insertCount += stats.getInsertCount();
		deleteCount += stats.getDeleteCount();
		fetchCount += stats.getFetchCount();
		optimisticFailureCount += stats.getOptimisticFailureCount();
	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		return "name=" + name + ", shortName=" + shortName + ",loadCount=" + loadCount + ", updateCount=" + updateCount
				+ ", insertCount=" + insertCount + ", deleteCount=" + deleteCount + ", fetchCount=" + fetchCount
				+ ", optimisticFailureCount" + optimisticFailureCount;
	}

	/**
	 * getName
	 */
	public String getName() {
		return name;
	}

	/**
	 * getShortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * getLoadCount
	 */
	public long getLoadCount() {
		return loadCount;
	}

	/**
	 * getUpdateCount
	 */
	public long getUpdateCount() {
		return updateCount;
	}

	/**
	 * getInsertCount
	 */
	public long getInsertCount() {
		return insertCount;
	}

	/**
	 * getDeleteCount
	 */
	public long getDeleteCount() {
		return deleteCount;
	}

	/**
	 * getFetchCount
	 */
	public long getFetchCount() {
		return fetchCount;
	}

	/**
	 * getOptimisticFailureCount
	 */
	public long getOptimisticFailureCount() {
		return optimisticFailureCount;
	}

	/**
	 * toCompositeData
	 */
	public CompositeData toCompositeData() {
		try {
			return new CompositeDataSupport(
					COMPOSITE_TYPE, ITEM_NAMES, new Object[] {
					name, shortName, loadCount,
					updateCount, insertCount, deleteCount, fetchCount, optimisticFailureCount,
			}
			);
		}
		catch ( OpenDataException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * newTabularDataInstance
	 */
	public static TabularData newTabularDataInstance() {
		return new TabularDataSupport( TABULAR_TYPE );
	}

	/**
	 * fromTabularData
	 */
	public static EntityStats[] fromTabularData(final TabularData tabularData) {
		final List<EntityStats> countList = new ArrayList( tabularData.size() );
		for ( final Iterator pos = tabularData.values().iterator(); pos.hasNext(); ) {
			countList.add( new EntityStats( (CompositeData) pos.next() ) );
		}
		return countList.toArray( new EntityStats[countList.size()] );
	}

}
