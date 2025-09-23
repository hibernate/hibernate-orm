/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.memory;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

final class HotspotPerThreadAllocationSnapshotter implements MemoryAllocationSnapshotter {

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

	final static class PerThreadMemoryAllocationSnapshot implements MemoryAllocationSnapshot {
		private final long[] threadIds;
		private final long[] threadAllocatedBytes;

		PerThreadMemoryAllocationSnapshot(long[] threadIds, long[] threadAllocatedBytes) {
			this.threadIds = threadIds;
			this.threadAllocatedBytes = threadAllocatedBytes;
		}

		public long[] threadIds() {
			return threadIds;
		}

		public long[] threadAllocatedBytes() {
			return threadAllocatedBytes;
		}

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

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || obj.getClass() != this.getClass() ) {
				return false;
			}
			var that = (PerThreadMemoryAllocationSnapshot) obj;
			return Objects.equals( this.threadIds, that.threadIds ) &&
					Objects.equals( this.threadAllocatedBytes, that.threadAllocatedBytes );
		}

		@Override
		public int hashCode() {
			return Objects.hash( threadIds, threadAllocatedBytes );
		}

		@Override
		public String toString() {
			return "PerThreadMemoryAllocationSnapshot[" +
					"threadIds=" + threadIds + ", " +
					"threadAllocatedBytes=" + threadAllocatedBytes + ']';
		}
	}
	private final ThreadMXBean threadMXBean;

	HotspotPerThreadAllocationSnapshotter(ThreadMXBean threadMXBean) {
		this.threadMXBean = threadMXBean;
	}

	public ThreadMXBean threadMXBean() {
		return threadMXBean;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || obj.getClass() != this.getClass() ) {
			return false;
		}
		var that = (HotspotPerThreadAllocationSnapshotter) obj;
		return Objects.equals( this.threadMXBean, that.threadMXBean );
	}

	@Override
	public int hashCode() {
		return Objects.hash( threadMXBean );
	}

	@Override
	public String toString() {
		return "HotspotPerThreadAllocationSnapshotter[" +
				"threadMXBean=" + threadMXBean + ']';
	}
}
