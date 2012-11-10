/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.query;

import org.hibernate.Query;
import org.hibernate.shards.defaultmock.QueryDefaultMock;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * @author Maulik Shah
 */
public class SetBigIntegerEventTest {

    @Test
    public void testSetBigIntegerEventPositionVal() {
        SetBigIntegerEvent event = new SetBigIntegerEvent(-1, null);
        final boolean[] called = {false};
        Query query = new QueryDefaultMock() {
            @Override
            public Query setBigInteger(int position, BigInteger val) {
                called[0] = true;
                return null;
            }
        };
        event.onEvent(query);
        Assert.assertTrue(called[0]);
    }

    @Test
    public void testSetBigIntegerEventNameVal() {
        SetBigIntegerEvent event = new SetBigIntegerEvent(null, null);
        final boolean[] called = {false};
        Query query = new QueryDefaultMock() {
            @Override
            public Query setBigInteger(String name, BigInteger val) {
                called[0] = true;
                return null;
            }
        };
        event.onEvent(query);
        Assert.assertTrue(called[0]);
    }
}