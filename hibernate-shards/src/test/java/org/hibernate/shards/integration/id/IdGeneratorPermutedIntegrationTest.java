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

package org.hibernate.shards.integration.id;

import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.model.Building;
import org.hibernate.shards.util.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Tomislav Nad
 */
public class IdGeneratorPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

    @Test
    public void testSingleShard() {
        session.beginTransaction();
        Building b = new Building();
        b.setName("foo");
        session.save(b);
        session.getTransaction().commit();
        resetSession();
        Assert.assertNotNull(b.getBuildingId());

        Building b2 = (Building) session.get(Building.class, b.getBuildingId());
        Assert.assertNotNull(b2);
        Assert.assertEquals(b.getName(), b2.getName());
    }

    @Test
    public void testMultipleShards() {
        session.beginTransaction();
        List<Building> buildings = Lists.newArrayList();
        for (int i = 0; i < getNumDatabases(); ++i) {
            Building b = new Building();
            b.setName("foo" + i);
            buildings.add(b);
            session.save(b);
        }
        session.getTransaction().commit();
        resetSession();
        for (Building b : buildings) {
            Assert.assertNotNull(b.getBuildingId());
            Building returnedBuilding = (Building) session.get(Building.class, b.getBuildingId());
            Assert.assertEquals(b.getName(), returnedBuilding.getName());
        }
    }
}
