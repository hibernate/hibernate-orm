/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.bytecode.enhancement.tracker;

import org.hibernate.bytecode.enhance.internal.tracker.SortedDirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleDirtyTracker;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class DirtyTrackerTest {

    @Test
    public void testSimpleTracker() {
        SimpleDirtyTracker tracker = new SimpleDirtyTracker();
        assertTrue(tracker.isEmpty());
        assertTrue(tracker.asSet().isEmpty());

        tracker.add("foo");
        assertFalse(tracker.isEmpty());
        assertArrayEquals(tracker.asSet().toArray(), new String[]{"foo"});

        tracker.clear();
        assertTrue(tracker.isEmpty());
        assertTrue(tracker.asSet().isEmpty());

        tracker.add("foo");
        tracker.add("bar");
        tracker.add("another.bar");
        tracker.add("foo");
        tracker.add("another.foo");
        tracker.add("another.bar");
        assertTrue(tracker.asSet().size() == 4);

    }

    @Test
    public void testSortedTracker() {
        SortedDirtyTracker tracker = new SortedDirtyTracker();
        assertTrue(tracker.isEmpty());
        assertTrue(tracker.asSet().isEmpty());

        tracker.add("foo");
        assertFalse(tracker.isEmpty());
        assertArrayEquals(tracker.asSet().toArray(), new String[]{"foo"});

        tracker.clear();
        assertTrue(tracker.isEmpty());
        assertTrue(tracker.asSet().isEmpty());

        tracker.add("foo");
        tracker.add("bar");
        tracker.add("another.bar");
        tracker.add("foo");
        tracker.add("another.foo");
        tracker.add("another.bar");
        assertTrue(tracker.asSet().size() == 4);

        // we the algorithm for this implementation relies on the fact that the array is kept sorted, so let's check it really is
        assertTrue(isSorted(tracker.asSet()));
    }

    private boolean isSorted(Set<String> set) {
        String[] arr = new String[set.size()];
        arr = set.toArray(arr);
        for (int i = 1; i < arr.length; i++) {
            if (arr[i - 1].compareTo(arr[i]) > 0) {
                return false;
            }
        }
        return true;
    }

}


