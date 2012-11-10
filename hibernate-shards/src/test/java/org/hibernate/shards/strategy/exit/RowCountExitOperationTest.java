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

import org.hibernate.criterion.Projections;
import org.hibernate.shards.util.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Maulik Shah
 */
public class RowCountExitOperationTest {

    @Test(expected = IllegalAccessException.class)
    public void testCtor() throws Exception {
        new RowCountExitOperation(Projections.avg("foo"));
        Assert.fail();
    }

    @Test
    public void testApplyCount() throws Exception {
        RowCountExitOperation exitOp = new RowCountExitOperation(Projections.rowCount());
        List<Object> list = Lists.<Object>newArrayList(1, 2, null, 3);
        Assert.assertEquals(3, (int) (Integer) exitOp.apply(list).get(0));
    }
}
