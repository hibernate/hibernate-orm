/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.stat.SecondLevelCacheStatistics;

/**
 * Bean for exposing region stats
 *
 * @author gkeim
 */
public class CacheRegionStats implements Serializable {
	private static final String COMPOSITE_TYPE_NAME = "CacheRegionStats";
	private static final String COMPOSITE_TYPE_DESCRIPTION = "Statistics per Cache-region";
	private static final String[] ITEM_NAMES = new String[] {
			"region", "shortName", "hitCount",
			"missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal",
	};
	private static final String[] ITEM_DESCRIPTIONS = new String[] {
			"region", "shortName", "hitCount",
			"missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal",
	};
	private static final OpenType[] ITEM_TYPES = new OpenType[] {
			SimpleType.STRING,
			SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG,
			SimpleType.LONG, SimpleType.LONG,
	};
	private static final CompositeType COMPOSITE_TYPE;
	private static final String TABULAR_TYPE_NAME = "Statistics by Cache-region";
	private static final String TABULAR_TYPE_DESCRIPTION = "All Cache Region Statistics";
	private static final String[] INDEX_NAMES = new String[] {"region",};
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
	 * region name
	 */
	protected final String region;

	/**
	 * region short name
	 */
	protected final String shortName;

	/**
	 * hit count
	 */
	protected long hitCount;

	/**
	 * miss count
	 */
	protected long missCount;

	/**
	 * put count
	 */
	protected long putCount;

	/**
	 * hit ratio
	 */
	protected double hitRatio;

	/**
	 * in-memory element count
	 */
	protected long elementCountInMemory;

	/**
	 * on-disk element count
	 */
	protected long elementCountOnDisk;

	/**
	 * total element count
	 */
	protected long elementCountTotal;

	/**
	 * Construct a CacheRegionStats
	 *
	 * @param region The region name
	 */
	public CacheRegionStats(String region) {
		this.region = region;
		this.shortName = CacheRegionUtils.determineShortName( region );
	}

	/**
	 * Construct a CacheRegionStats
	 *
	 * @param region The region name
	 * @param src The SecondLevelCacheStatistics reference
	 */
	public CacheRegionStats(String region, SecondLevelCacheStatistics src) {
		this( region );

		try {
			this.hitCount = BeanUtils.getLongBeanProperty( src, "hitCount" );
			this.missCount = BeanUtils.getLongBeanProperty( src, "missCount" );
			this.putCount = BeanUtils.getLongBeanProperty( src, "putCount" );
			this.hitRatio = determineHitRatio();
			this.elementCountInMemory = BeanUtils.getLongBeanProperty( src, "elementCountInMemory" );
			this.elementCountOnDisk = BeanUtils.getLongBeanProperty( src, "elementCountOnDisk" );
			this.elementCountTotal = BeanUtils.getLongBeanProperty( src, "elementCountOnDisk" );
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException( "Exception retrieving statistics", e );
		}
	}

	/**
	 * Construct a CacheRegionStats
	 *
	 * @param cData No idea
	 */
	@SuppressWarnings("UnusedAssignment")
	public CacheRegionStats(final CompositeData cData) {
		int i = 0;
		region = (String) cData.get( ITEM_NAMES[i++] );
		shortName = (String) cData.get( ITEM_NAMES[i++] );
		hitCount = (Long) cData.get( ITEM_NAMES[i++] );
		missCount = (Long) cData.get( ITEM_NAMES[i++] );
		putCount = (Long) cData.get( ITEM_NAMES[i++] );
		hitRatio = (Double) cData.get( ITEM_NAMES[i++] );
		elementCountInMemory = (Long) cData.get( ITEM_NAMES[i++] );
		elementCountOnDisk = (Long) cData.get( ITEM_NAMES[i++] );
		elementCountTotal = (Long) cData.get( ITEM_NAMES[i++] );
	}

	protected double determineHitRatio() {
		final long readCount = getHitCount() + getMissCount();
		double result = 0;
		if ( readCount > 0 ) {
			result = getHitCount() / ((double) readCount);
		}
		return result;
	}

	@Override
	public String toString() {
		return "region=" + getRegion() + "shortName=" + getShortName() + ", hitCount=" + getHitCount() + ", missCount="
				+ getMissCount() + ", putCount" + getPutCount() + ", hitRatio" + getHitRatio() + ", elementCountInMemory="
				+ getElementCountInMemory() + ", elementCountOnDisk=" + getElementCountOnDisk() + ", elementCountTotal="
				+ getElementCountTotal();
	}

	public String getRegion() {
		return region;
	}

	public String getShortName() {
		return shortName;
	}

	public long getHitCount() {
		return hitCount;
	}

	public long getMissCount() {
		return missCount;
	}

	public long getPutCount() {
		return putCount;
	}

	public double getHitRatio() {
		return hitRatio;
	}

	public long getElementCountInMemory() {
		return elementCountInMemory;
	}

	public long getElementCountOnDisk() {
		return elementCountOnDisk;
	}

	public long getElementCountTotal() {
		return elementCountTotal;
	}

	/**
	 * Convert our state into a JMX CompositeData
	 *
	 * @return composite data
	 */
	public CompositeData toCompositeData() {
		try {
			return new CompositeDataSupport(
					COMPOSITE_TYPE, ITEM_NAMES, new Object[] {
					getRegion(), getShortName(),
					getHitCount(), getMissCount(), getPutCount(), getHitRatio(), getElementCountInMemory(),
					getElementCountOnDisk(), getElementCountTotal(),
			}
			);
		}
		catch (OpenDataException e) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Convert our state into a JMX TabularData
	 *
	 * @return tabular data
	 */
	public static TabularData newTabularDataInstance() {
		return new TabularDataSupport( TABULAR_TYPE );
	}

	/**
	 * Re-build the CacheRegionStats from JMX tabular data
	 *
	 * @param tabularData The JMX tabular data
	 *
	 * @return array of region statistics
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static CacheRegionStats[] fromTabularData(final TabularData tabularData) {
		final List<CacheRegionStats> countList = new ArrayList<CacheRegionStats>( tabularData.size() );
		for ( Object o : tabularData.values() ) {
			countList.add( new CacheRegionStats( (CompositeData) o ) );
		}
		return countList.toArray( new CacheRegionStats[countList.size()] );
	}
}
