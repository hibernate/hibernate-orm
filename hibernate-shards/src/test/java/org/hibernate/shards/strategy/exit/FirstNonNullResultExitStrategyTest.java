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
package org.hibernate.shards.strategy.exit;

import org.hibernate.shards.Shard;
import org.hibernate.shards.defaultmock.ShardDefaultMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author maxr@google.com (Max Ross)
 */
public class FirstNonNullResultExitStrategyTest {

    @Test
    public void testNullShard() {
        FirstNonNullResultExitStrategy<Object> fnnres = new FirstNonNullResultExitStrategy<Object>();
        try {
            fnnres.addResult(null, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }
    }

    @Test
    public void testAddResult() {
        FirstNonNullResultExitStrategy<Object> fnnres = new FirstNonNullResultExitStrategy<Object>();
        Shard shard1 = new ShardDefaultMock();
        fnnres.addResult(null, shard1);
        Assert.assertNull(fnnres.compileResults(null));
        Assert.assertNull(fnnres.getShardOfResult());

        Object result = new Object();
        Shard shard2 = new ShardDefaultMock();
        fnnres.addResult(result, shard2);
        Assert.assertSame(result, fnnres.compileResults(null));
        Assert.assertSame(shard2, fnnres.getShardOfResult());

        Object anotherResult = new Object();
        Shard shard3 = new ShardDefaultMock();
        fnnres.addResult(anotherResult, shard3);
        Assert.assertSame(result, fnnres.compileResults(null));
        Assert.assertSame(shard2, fnnres.getShardOfResult());
    }
}
