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

package org.hibernate.shards;

import org.hibernate.shards.strategy.ShardStrategyImpl;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.access.ShardAccessStrategyDefaultMock;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategyDefaultMock;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategyDefaultMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author maxr@google.com (Max Ross)
 */
public class InstanceShardStrategyImplTest {

    @Test
    public void testCtor() {
        try {
            new ShardStrategyImpl(null, null, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        ShardSelectionStrategy sss = new ShardSelectionStrategyDefaultMock();
        ShardResolutionStrategy srs = new ShardResolutionStrategyDefaultMock();
        ShardAccessStrategy sas = new ShardAccessStrategyDefaultMock();
        try {
            new ShardStrategyImpl(sss, null, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardStrategyImpl(null, srs, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardStrategyImpl(null, null, sas);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardStrategyImpl(sss, srs, null);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardStrategyImpl(null, srs, sas);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        new ShardStrategyImpl(sss, srs, sas);
    }
}
