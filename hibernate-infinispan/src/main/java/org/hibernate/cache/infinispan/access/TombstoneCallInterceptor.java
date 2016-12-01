/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.TombstoneUpdate;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.SizeCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.ValueMatcher;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.CallInterceptor;
import org.infinispan.metadata.EmbeddedMetadata;
import org.infinispan.metadata.Metadata;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Note that this does not implement all commands, only those appropriate for {@link TombstoneAccessDelegate}
 * and {@link org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion}
 *
 * The behaviour here also breaks notifications, which are not used for 2LC caches.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneCallInterceptor extends CallInterceptor {
	private static final Log log = LogFactory.getLog(TombstoneCallInterceptor.class);
	private static final UUID ZERO = new UUID(0, 0);

	private final BaseTransactionalDataRegion region;
	private final Metadata expiringMetadata;
	private Metadata defaultMetadata;
	private AdvancedCache cache;

	public TombstoneCallInterceptor(BaseTransactionalDataRegion region) {
		this.region = region;
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
		log.tracef("In cache %s(%d) applying update %s to %s", cache.getName(), region.getLastRegionInvalidation(), command.getValue(), e.getValue());
		try {
			Object value = command.getValue();
			if (value instanceof TombstoneUpdate) {
				return handleTombstoneUpdate(e, (TombstoneUpdate) value, command);
			}
			else if (value instanceof Tombstone) {
				return handleTombstone(e, (Tombstone) value);
			}
			else if (value instanceof FutureUpdate) {
				return handleFutureUpdate(e, (FutureUpdate) value, command);
			}
			else {
				return super.visitPutKeyValueCommand(ctx, command);
			}
		}
		finally {
			log.tracef("Result is %s", e.getValue());
		}
	}

	private Object handleFutureUpdate(MVCCEntry e, FutureUpdate futureUpdate, PutKeyValueCommand command) {
		Object storedValue = e.getValue();
		if (storedValue instanceof Tombstone) {
			// Note that the update has to keep tombstone even if the transaction was unsuccessful;
			// before write we have removed the value and we have to protect the entry against stale putFromLoads
			Tombstone tombstone = (Tombstone) storedValue;
			setValue(e, tombstone.applyUpdate(futureUpdate.getUuid(), futureUpdate.getTimestamp(), futureUpdate.getValue()));

		}
		else {
			// This is an async future update, and it's timestamp may be vastly outdated
			// We need to first execute the async update and then local one, because if we're on the primary
			// owner the local future update would fail the async one.
			// TODO: There is some discrepancy with TombstoneUpdate handling which does not fail the update
			setFailed(command);
		}
		return null;
	}

	private Object handleTombstone(MVCCEntry e, Tombstone tombstone) {
		// Tombstones always come with lifespan in metadata
		Object storedValue = e.getValue();
		if (storedValue instanceof Tombstone) {
			setValue(e, ((Tombstone) storedValue).merge(tombstone));
		}
		else {
			setValue(e, tombstone);
		}
		return null;
	}

	protected Object handleTombstoneUpdate(MVCCEntry e, TombstoneUpdate tombstoneUpdate, PutKeyValueCommand command) {
		Object storedValue = e.getValue();
		Object value = tombstoneUpdate.getValue();

		if (value == null) {
			// eviction
			if (storedValue == null || storedValue instanceof Tombstone) {
				setFailed(command);
			}
			else {
				// We have to keep Tombstone, because otherwise putFromLoad could insert a stale entry
				// (after it has been already updated and *then* evicted)
				setValue(e, new Tombstone(ZERO, tombstoneUpdate.getTimestamp()));
			}
		}
		else if (storedValue instanceof Tombstone) {
			Tombstone tombstone = (Tombstone) storedValue;
			if (tombstone.getLastTimestamp() < tombstoneUpdate.getTimestamp()) {
				setValue(e, value);
			}
		}
		else if (storedValue == null) {
			// async putFromLoads shouldn't cross the invalidation timestamp
			if (region.getLastRegionInvalidation() < tombstoneUpdate.getTimestamp()) {
				setValue(e, value);
			}
		}
		else {
			// Don't do anything locally. This could be the async remote write, though, when local
			// value has been already updated: let it propagate to remote nodes, too
		}
		return null;
	}

	private Object setValue(MVCCEntry e, Object value) {
		if (e.isRemoved()) {
			e.setRemoved(false);
			e.setCreated(true);
			e.setValid(true);
		}
		else {
			e.setChanged(true);
		}
		if (value instanceof Tombstone) {
			e.setMetadata(expiringMetadata);
		}
		else {
			e.setMetadata(defaultMetadata);
		}
		return e.setValue(value);
	}

	private void setFailed(PutKeyValueCommand command) {
		// This sets command to be unsuccessful, since we don't want to replicate it to backup owner
		command.setValueMatcher(ValueMatcher.MATCH_NEVER);
		try {
			command.perform(null);
		}
		catch (Throwable ignored) {
		}
	}

	@Override
	public Object visitSizeCommand(InvocationContext ctx, SizeCommand command) throws Throwable {
		Set<Flag> flags = command.getFlags();
		int size = 0;
		Map<Object, CacheEntry> contextEntries = ctx.getLookedUpEntries();
		AdvancedCache decoratedCache = cache.getAdvancedCache().withFlags(flags != null ? flags.toArray(new Flag[flags.size()]) : null);
		// In non-transactional caches we don't care about context
		CloseableIterable<CacheEntry<Object, Object>> iterable = decoratedCache
				.filterEntries(Tombstone.EXCLUDE_TOMBSTONES);
		try {
			for (CacheEntry<Object, Object> entry : iterable) {
				if (entry.getValue() != null && size++ == Integer.MAX_VALUE) {
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
