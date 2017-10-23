/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.tracker;

import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.engine.spi.CompositeOwner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompositeOwnerTrackerTest {

    private int counter = 0;

    @Test
    public void testCompositeOwnerTracker() {

        CompositeOwnerTracker tracker = new CompositeOwnerTracker();
        tracker.add("foo", new TestCompositeOwner());

        tracker.callOwner(".street1");
        assertEquals(1, counter);
        tracker.add("bar", new TestCompositeOwner());
        tracker.callOwner(".city");
        assertEquals(3, counter);

        tracker.removeOwner("foo");

        tracker.callOwner(".country");
        assertEquals(4, counter);
        tracker.removeOwner("bar");

        tracker.callOwner(".country");

        tracker.add("moo", new TestCompositeOwner());
        tracker.callOwner(".country");
        assertEquals(5, counter);
    }

    class TestCompositeOwner implements CompositeOwner {

        @Override
        public void $$_hibernate_trackChange(String attributeName) {
            if(counter == 0)
                assertEquals("foo.street1", attributeName);
            if(counter == 1)
                assertEquals("foo.city", attributeName);
            if(counter == 2)
                assertEquals("bar.city", attributeName);
            if(counter == 3)
                assertEquals("bar.country", attributeName);
            if(counter == 4)
                assertEquals("moo.country", attributeName);
            counter++;
        }
    }
}


