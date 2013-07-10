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
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.CompositeOwnerTracker;
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


