/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.TombstoneUpdate;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.SizeCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.MVCCEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
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
	private static final UUID ZERO = new UUID(0, 0);

	private AdvancedCache cache;
	private final Metadata expiringMetadata;

	public TombstoneCallInterceptor(long tombstoneExpiration) {
		expiringMetadata = new EmbeddedMetadata.Builder().lifespan(tombstoneExpiration, TimeUnit.MILLISECONDS).build();
	}

	@Inject
	public void injectDependencies(AdvancedCache cache) {
		this.cache = cache;
	}

	@Override
	public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
		MVCCEntry e = (MVCCEntry) ctx.lookupEntry(command.getKey());
		if (e == null) {
			return null;
		}
		Object value = command.getValue();
		if (value instanceof TombstoneUpdate) {
			return handleTombstoneUpdate(e, (TombstoneUpdate) value);
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

	private Object handleFutureUpdate(MVCCEntry e, FutureUpdate futureUpdate, PutKeyValueCommand command) {
		Object storedValue = e.getValue();
		if (storedValue instanceof FutureUpdate) {
			FutureUpdate storedFutureUpdate = (FutureUpdate) storedValue;
			if (futureUpdate.getUuid().equals(storedFutureUpdate.getUuid())) {
				if (futureUpdate.getValue() != null) {
					// transaction succeeded
					setValue(e, futureUpdate.getValue());
				}
				else {
					// transaction failed
					setValue(e, storedFutureUpdate.getValue());
				}
			}
			else {
				// two conflicting updates
				setValue(e, new FutureUpdate(ZERO, null));
				e.setMetadata(expiringMetadata);
				// Infinispan always commits the entry with data with the metadata provided to the command,
				// However, in non-conflicting case we want to keep the value not expired
				command.setMetadata(expiringMetadata);
			}
		}
		else if (storedValue instanceof Tombstone){
			return null;
		}
		else {
			if (futureUpdate.getValue() != null) {
				// The future update has disappeared (probably due to region invalidation) and
				// the currently stored value was putFromLoaded (or is null).
				// We cannot keep the possibly outdated value here but we cannot know that
				// this command's value is the most up-to-date. Therefore, we'll remove
				// the value and let future putFromLoad update it.
				removeValue(e);
			}
			else {
				// this is the pre-update
				// change in logic: we don't keep the old value around anymore (for read-write strategy)
				setValue(e, new FutureUpdate(futureUpdate.getUuid(), null));
			}
		}
		return null;
	}

	private Object handleTombstone(MVCCEntry e, Tombstone tombstone) {
		Object storedValue = e.getValue();
		if (storedValue instanceof Tombstone) {
			e.setChanged(true);
			e.setValue(tombstone.merge((Tombstone) storedValue));
		}
		else {
			setValue(e, tombstone);
		}
		return null;
	}

	protected Object handleTombstoneUpdate(MVCCEntry e, TombstoneUpdate tombstoneUpdate) {
		Object storedValue = e.getValue();
		Object value = tombstoneUpdate.getValue();

		if (storedValue instanceof Tombstone) {
			Tombstone tombstone = (Tombstone) storedValue;
			if (tombstone.getLastTimestamp() < tombstoneUpdate.getTimestamp()) {
				e.setChanged(true);
				e.setValue(value);
			}
		}
		else if (storedValue == null) {
			// putFromLoad (putIfAbsent)
			setValue(e, value);
		}
		else if (value == null) {
			// evict
			removeValue(e);
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
		return e.setValue(value);
	}

	private void removeValue(MVCCEntry e) {
		e.setRemoved(true);
		e.setChanged(true);
		e.setCreated(false);
		e.setValid(false);
		e.setValue(null);
	}

	@Override
	public Object visitSizeCommand(InvocationContext ctx, SizeCommand command) throws Throwable {
		Set<Flag> flags = command.getFlags();
		int size = 0;
		Map<Object, CacheEntry> contextEntries = ctx.getLookedUpEntries();
		AdvancedCache decoratedCache = cache.getAdvancedCache().withFlags(flags != null ? flags.toArray(new Flag[flags.size()]) : null);
		// In non-transactional caches we don't care about context
		CloseableIterable<CacheEntry<Object, Object>> iterable = decoratedCache
				.filterEntries(Tombstone.EXCLUDE_TOMBSTONES)
				.converter(FutureUpdate.VALUE_EXTRACTOR);
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
