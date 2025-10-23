/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.memory;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;

record HotspotPerThreadAllocationSnapshotter(ThreadMXBean threadMXBean) implements MemoryAllocationSnapshotter {

	private static final @Nullable HotspotPerThreadAllocationSnapshotter INSTANCE;
	private static final Method GET_THREAD_ALLOCATED_BYTES;

	static {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		Method method = null;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ThreadMXBean> hotspotInterface =
					(Class<? extends ThreadMXBean>) Class.forName( "com.sun.management.ThreadMXBean" );
			try {
				method = hotspotInterface.getMethod( "getThreadAllocatedBytes", long[].class );
			}
			catch (Exception e) {
				// Ignore
			}

			if ( !hotspotInterface.isInstance( threadMXBean ) ) {
				threadMXBean = ManagementFactory.getPlatformMXBean( hotspotInterface );
			}
		}
		catch (Throwable e) {
			// Ignore
		}

		GET_THREAD_ALLOCATED_BYTES = method;

		HotspotPerThreadAllocationSnapshotter instance = null;
		if ( method != null && threadMXBean != null ) {
			try {
				instance = new HotspotPerThreadAllocationSnapshotter( threadMXBean );
				instance.snapshot();
			}
			catch (Exception e) {
				instance = null;
			}
		}
		INSTANCE = instance;
	}

	public static @Nullable HotspotPerThreadAllocationSnapshotter getInstance() {
		return INSTANCE;
	}

	@Override
	public MemoryAllocationSnapshot snapshot() {
		long[] threadIds = threadMXBean.getAllThreadIds();
		try {
			return new PerThreadMemoryAllocationSnapshot(
					threadIds,
					(long[]) GET_THREAD_ALLOCATED_BYTES.invoke( threadMXBean, (Object) threadIds )
			);
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	record PerThreadMemoryAllocationSnapshot(long[] threadIds, long[] threadAllocatedBytes)
			implements MemoryAllocationSnapshot {

		@Override
		public long difference(MemoryAllocationSnapshot before) {
			final PerThreadMemoryAllocationSnapshot other = (PerThreadMemoryAllocationSnapshot) before;
			final HashMap<Long, Integer> previousThreadIdToIndexMap = new HashMap<>();
			for ( int i = 0; i < other.threadIds.length; i++ ) {
				previousThreadIdToIndexMap.put( other.threadIds[i], i );
			}
			long allocatedBytes = 0;
			for ( int i = 0; i < threadIds.length; i++ ) {
				allocatedBytes += threadAllocatedBytes[i];
				final Integer previousThreadIndex = previousThreadIdToIndexMap.get( threadIds[i] );
				if ( previousThreadIndex != null ) {
					allocatedBytes -= other.threadAllocatedBytes[previousThreadIndex];
				}
			}
			return allocatedBytes;
		}
	}
}
