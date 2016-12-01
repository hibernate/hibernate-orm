/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.VersionedEntry;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.SizeCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.filter.NullValueConverter;
import org.infinispan.interceptors.CallInterceptor;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Note that this does not implement all commands, only those appropriate for {@link TombstoneAccessDelegate}
 * and {@link org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion}
 *
 * The behaviour here also breaks notifications, which are not used for 2LC caches.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class VersionedCallInterceptor extends CallInterceptor {
	private final Comparator<Object> versionComparator;
	private final Metadata expiringMetadata;
	private AdvancedCache cache;
	private Metadata defaultMetadata;

	public VersionedCallInterceptor(BaseTransactionalDataRegion region, Comparator<Object> versionComparator) {
		this.versionComparator = versionComparator;
		expiringMetadata = new EmbeddedMetadata.Builder().lifespan(region.getTombstoneExpiration(), TimeUnit.MILLISECONDS).build();
	}

	@Inject
	public void injectDependencies(AdvancedCache cache) {
		this.cache = cache;
	}

	@Start
	public void start() {
		defaultMetadata = new EmbeddedMetadata.Builder()
			.lifespan(cacheConfiguration.expiration().lifespan())
			.maxIdle(cacheConfiguration.expiration().maxIdle()).build();
	}

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		MVCCEntry e = (MVCCEntry) ctx.lookupEntry(command.getKey());
		if (e == null) {
			return null;
		}

		Object oldValue = e.getValue();
		Object oldVersion = null;
		long oldTimestamp = Long.MIN_VALUE;
		if (oldValue instanceof VersionedEntry) {
			oldVersion = ((VersionedEntry) oldValue).getVersion();
			oldTimestamp = ((VersionedEntry) oldValue).getTimestamp();
			oldValue = ((VersionedEntry) oldValue).getValue();
		}
		else if (oldValue instanceof org.hibernate.cache.spi.entry.CacheEntry) {
			oldVersion = ((org.hibernate.cache.spi.entry.CacheEntry) oldValue).getVersion();
		}

		Object newValue = command.getValue();
		Object newVersion = null;
		long newTimestamp = Long.MIN_VALUE;
		Object actualNewValue = newValue;
		boolean isRemoval = false;
		if (newValue instanceof VersionedEntry) {
			VersionedEntry ve = (VersionedEntry) newValue;
			newVersion = ve.getVersion();
			newTimestamp = ve.getTimestamp();
			if (ve.getValue() == null) {
				isRemoval = true;
			}
			else if (ve.getValue() instanceof org.hibernate.cache.spi.entry.CacheEntry) {
				actualNewValue = ve.getValue();
			}
		}
		else if (newValue instanceof org.hibernate.cache.spi.entry.CacheEntry) {
			newVersion = ((org.hibernate.cache.spi.entry.CacheEntry) newValue).getVersion();
		}

		if (newVersion == null) {
			// eviction or post-commit removal: we'll store it with given timestamp
			setValue(e, newValue, expiringMetadata);
			return null;
		}
		if (oldVersion == null) {
			assert oldValue == null || oldTimestamp != Long.MIN_VALUE;
			if (newTimestamp == Long.MIN_VALUE) {
				// remove, knowing the version
				setValue(e, newValue, expiringMetadata);
			}
			else if (newTimestamp <= oldTimestamp) {
				// either putFromLoad or regular update/insert - in either case this update might come
				// when it was evicted/region-invalidated. In both cases, with old timestamp we'll leave
				// the invalid value
				assert oldValue == null;
			}
			else {
				setValue(e, newValue, defaultMetadata);
			}
			return null;
		}
		int compareResult = versionComparator.compare(newVersion, oldVersion);
		if (isRemoval && compareResult >= 0) {
			setValue(e, newValue, expiringMetadata);
		}
		else if (compareResult > 0) {
			setValue(e, actualNewValue, defaultMetadata);
		}
		return null;
	}

	private Object setValue(MVCCEntry e, Object value, Metadata metadata) {
		if (e.isRemoved()) {
			e.setRemoved(false);
			e.setCreated(true);
			e.setValid(true);
		}
		else {
			e.setChanged(true);
		}
		e.setMetadata(metadata);
		return e.setValue(value);
	}

	@Override
	public Object visitSizeCommand(InvocationContext ctx, SizeCommand command) throws Throwable {
		Set<Flag> flags = command.getFlags();
		int size = 0;
		AdvancedCache decoratedCache = cache.getAdvancedCache().withFlags(flags != null ? flags.toArray(new Flag[flags.size()]) : null);
		// In non-transactional caches we don't care about context
		CloseableIterable<CacheEntry<Object, Void>> iterable = decoratedCache
				.filterEntries(VersionedEntry.EXCLUDE_EMPTY_EXTRACT_VALUE).converter(NullValueConverter.getInstance());
		try {
			for (CacheEntry<Object, Void> entry : iterable) {
				if (size++ == Integer.MAX_VALUE) {
					return Integer.MAX_VALUE;
				}
			}
		}
		finally {
			iterable.close();
		}
		return size;
	}
}
