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

package org.hibernate.shards.id;

import org.hibernate.shards.ShardId;
import org.hibernate.shards.session.ShardedSessionImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Properties;

/**
 * @author Tomislav Nad
 */
public class ShardedUUIDGeneratorTest {

    private ShardedUUIDGenerator gen;

    @Before
    protected void setUp() {
        gen = new ShardedUUIDGenerator();
    }

    @Test
    public void testHexShardEncoding() throws Exception {
        Properties prop = new Properties();
        prop.setProperty("sharded-uuid-type", "STRING");
        gen.configure(null, prop, null);
        ShardedSessionImpl.setCurrentSubgraphShardId(new ShardId(13));
        Serializable id = gen.generate(null, null);
        Assert.assertEquals(new ShardId(13), gen.extractShardId(id));
    }

    @Test
    public void testIntegerShardEncoding() throws Exception {
        Properties prop = new Properties();
        gen.configure(null, prop, null);
        ShardedSessionImpl.setCurrentSubgraphShardId(new ShardId(13));
        Serializable id = gen.generate(null, null);
        Assert.assertEquals(new ShardId(13), gen.extractShardId(id));
    }
}
