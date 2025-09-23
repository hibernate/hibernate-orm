/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.memory;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

record HotspotTotalThreadBytesSnapshotter(ThreadMXBean threadMXBean) implements MemoryAllocationSnapshotter {

	private static final @Nullable HotspotTotalThreadBytesSnapshotter INSTANCE;
	private static final Method GET_TOTAL_THREAD_ALLOCATED_BYTES;

	static {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		Method method = null;
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ThreadMXBean> hotspotInterface =
					(Class<? extends ThreadMXBean>) Class.forName( "com.sun.management.ThreadMXBean" );
			try {
				method = hotspotInterface.getMethod( "getTotalThreadAllocatedBytes" );
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

		GET_TOTAL_THREAD_ALLOCATED_BYTES = method;

		HotspotTotalThreadBytesSnapshotter instance = null;
		if ( method != null && threadMXBean != null ) {
			try {
				instance = new HotspotTotalThreadBytesSnapshotter( threadMXBean );
				instance.snapshot();
			}
			catch (Exception e) {
				instance = null;
			}
		}
		INSTANCE = instance;
	}

	public static @Nullable HotspotTotalThreadBytesSnapshotter getInstance() {
		return INSTANCE;
	}

	@Override
	public MemoryAllocationSnapshot snapshot() {
		try {
			return new GlobalMemoryAllocationSnapshot( (long) GET_TOTAL_THREAD_ALLOCATED_BYTES.invoke( threadMXBean ) );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	record GlobalMemoryAllocationSnapshot(long allocatedBytes) implements MemoryAllocationSnapshot {

		GlobalMemoryAllocationSnapshot {
			if ( allocatedBytes == -1L ) {
				throw new IllegalArgumentException( "getTotalThreadAllocatedBytes is disabled" );
			}
		}

		@Override
		public long difference(MemoryAllocationSnapshot before) {
			final GlobalMemoryAllocationSnapshot other = (GlobalMemoryAllocationSnapshot) before;
			return Math.max( allocatedBytes - other.allocatedBytes, 0L );
		}
	}
}
