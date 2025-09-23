/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
