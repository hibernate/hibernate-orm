/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

final class GlobalMemoryUsageSnapshotter implements MemoryAllocationSnapshotter {

	private static final GlobalMemoryUsageSnapshotter INSTANCE = new GlobalMemoryUsageSnapshotter(
			ManagementFactory.getMemoryPoolMXBeans()
	);

	private final List<MemoryPoolMXBean> heapPoolBeans;
	private final Runnable gcAndWait;

	private GlobalMemoryUsageSnapshotter(List<MemoryPoolMXBean> heapPoolBeans) {
		this.heapPoolBeans = heapPoolBeans;
		this.gcAndWait = () -> {
			for (int i = 0; i < 3; i++) {
				System.gc();
				try { Thread.sleep(50); } catch (InterruptedException ignored) {}
			}
		};
	}

	public static GlobalMemoryUsageSnapshotter getInstance() {
		return INSTANCE;
	}

	@Override
	public MemoryAllocationSnapshot snapshot() {
		final long peakUsage = heapPoolBeans.stream().mapToLong(p -> p.getPeakUsage().getUsed()).sum();
		gcAndWait.run();
		final long retainedUsage = heapPoolBeans.stream().mapToLong(p -> p.getUsage().getUsed()).sum();
		heapPoolBeans.forEach(MemoryPoolMXBean::resetPeakUsage);
		return new GlobalMemoryAllocationSnapshot( peakUsage, retainedUsage );
	}

	record GlobalMemoryAllocationSnapshot(long peakUsage, long retainedUsage) implements MemoryAllocationSnapshot {

		@Override
		public long difference(MemoryAllocationSnapshot before) {
			// When doing the "before" snapshot, the peak usage is reset.
			// Since this object is the "after" snapshot, we can simply estimate the memory usage of an operation
			// to be the peak usage of that operation minus the usage after GC
			return peakUsage - retainedUsage;
		}
	}
}
