/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.memory;

public class MemoryUsageUtil {

	private static final MemoryAllocationSnapshotter SNAPSHOTTER;

	static {
		MemoryAllocationSnapshotter snapshotter = HotspotTotalThreadBytesSnapshotter.getInstance();
		if ( snapshotter == null ) {
			snapshotter = HotspotPerThreadAllocationSnapshotter.getInstance();
		}
		if ( snapshotter == null ) {
			snapshotter = GlobalMemoryUsageSnapshotter.getInstance();
		}
		SNAPSHOTTER = snapshotter;
	}

	public static long estimateMemoryUsage(Runnable runnable) {
		final MemoryAllocationSnapshot beforeSnapshot = SNAPSHOTTER.snapshot();
		runnable.run();
		return SNAPSHOTTER.snapshot().difference( beforeSnapshot );
	}
}
