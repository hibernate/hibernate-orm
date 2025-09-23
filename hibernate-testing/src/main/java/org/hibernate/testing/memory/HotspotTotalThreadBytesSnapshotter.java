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
import java.util.Objects;

final class HotspotTotalThreadBytesSnapshotter implements MemoryAllocationSnapshotter {

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

	final static class GlobalMemoryAllocationSnapshot implements MemoryAllocationSnapshot {
		private final long allocatedBytes;

		GlobalMemoryAllocationSnapshot(long allocatedBytes) {
			if ( allocatedBytes == -1L ) {
				throw new IllegalArgumentException( "getTotalThreadAllocatedBytes is disabled" );
			}
			this.allocatedBytes = allocatedBytes;
		}

		@Override
		public long difference(MemoryAllocationSnapshot before) {
			final GlobalMemoryAllocationSnapshot other = (GlobalMemoryAllocationSnapshot) before;
			return Math.max( allocatedBytes - other.allocatedBytes, 0L );
		}

		public long allocatedBytes() {
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
			var that = (GlobalMemoryAllocationSnapshot) obj;
			return this.allocatedBytes == that.allocatedBytes;
		}

		@Override
		public int hashCode() {
			return Objects.hash( allocatedBytes );
		}

		@Override
		public String toString() {
			return "GlobalMemoryAllocationSnapshot[" +
					"allocatedBytes=" + allocatedBytes + ']';
		}
	}

	private final ThreadMXBean threadMXBean;

	HotspotTotalThreadBytesSnapshotter(ThreadMXBean threadMXBean) {
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
		var that = (HotspotTotalThreadBytesSnapshotter) obj;
		return Objects.equals( this.threadMXBean, that.threadMXBean );
	}

	@Override
	public int hashCode() {
		return Objects.hash( threadMXBean );
	}

	@Override
	public String toString() {
		return "HotspotTotalThreadBytesSnapshotter[" +
				"threadMXBean=" + threadMXBean + ']';
	}
}
