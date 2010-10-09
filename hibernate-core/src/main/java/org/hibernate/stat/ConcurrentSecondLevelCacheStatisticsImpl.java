package org.hibernate.stat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.CacheKey;
import org.hibernate.cache.Region;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class ConcurrentSecondLevelCacheStatisticsImpl extends CategorizedStatistics implements SecondLevelCacheStatistics {

	private final transient Region region;
	private AtomicLong hitCount = new AtomicLong();
	private AtomicLong missCount = new AtomicLong();
	private AtomicLong putCount = new AtomicLong();

	ConcurrentSecondLevelCacheStatisticsImpl(Region region) {
		super(region.getName());
		this.region = region;
	}

	public long getHitCount() {
		return hitCount.get();
	}

	public long getMissCount() {
		return missCount.get();
	}

	public long getPutCount() {
		return putCount.get();
	}

	public long getElementCountInMemory() {
		return region.getElementCountInMemory();
	}

	public long getElementCountOnDisk() {
		return region.getElementCountOnDisk();
	}

	public long getSizeInMemory() {
		return region.getSizeInMemory();
	}

	public Map getEntries() {
		Map map = new HashMap();
		Iterator iter = region.toMap().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry me = (Map.Entry) iter.next();
			map.put(((CacheKey) me.getKey()).getKey(), me.getValue());
		}
		return map;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder()
				.append("SecondLevelCacheStatistics")
				.append("[hitCount=").append(this.hitCount)
				.append(",missCount=").append(this.missCount)
				.append(",putCount=").append(this.putCount);
		//not sure if this would ever be null but wanted to be careful
		if (region != null) {
			buf.append(",elementCountInMemory=").append(this.getElementCountInMemory())
					.append(",elementCountOnDisk=").append(this.getElementCountOnDisk())
					.append(",sizeInMemory=").append(this.getSizeInMemory());
		}
		buf.append(']');
		return buf.toString();
	}

	void incrementHitCount() {
		hitCount.getAndIncrement();
	}

	void incrementMissCount() {
		missCount.getAndIncrement();
	}

	void incrementPutCount() {
		putCount.getAndIncrement();
	}
}
