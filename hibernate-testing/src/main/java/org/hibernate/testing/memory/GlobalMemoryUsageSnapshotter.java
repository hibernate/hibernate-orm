/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Objects;

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
				try {
					Thread.sleep( 50 );
				}
				catch (InterruptedException ignored) {
				}
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

	final static class GlobalMemoryAllocationSnapshot implements MemoryAllocationSnapshot {
		private final long peakUsage;
		private final long retainedUsage;

		GlobalMemoryAllocationSnapshot(long peakUsage, long retainedUsage) {
			this.peakUsage = peakUsage;
			this.retainedUsage = retainedUsage;
		}

		public long peakUsage() {
			return peakUsage;
		}

		public long retainedUsage() {
			return retainedUsage;
		}

		@Override
		public long difference(MemoryAllocationSnapshot before) {
			// When doing the "before" snapshot, the peak usage is reset.
			// Since this object is the "after" snapshot, we can simply estimate the memory usage of an operation
			// to be the peak usage of that operation minus the usage after GC
			return peakUsage - retainedUsage;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || obj.getClass() != this.getClass() ) {
				return false;
			}
			var that = (GlobalMemoryAllocationSnapshot) obj;
			return this.peakUsage == that.peakUsage &&
					this.retainedUsage == that.retainedUsage;
		}

		@Override
		public int hashCode() {
			return Objects.hash( peakUsage, retainedUsage );
		}

		@Override
		public String toString() {
			return "GlobalMemoryAllocationSnapshot[" +
					"peakUsage=" + peakUsage + ", " +
					"retainedUsage=" + retainedUsage + ']';
		}
	}
}
