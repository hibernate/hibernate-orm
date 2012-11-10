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

import org.hibernate.criterion.AvgProjection;
import org.hibernate.shards.util.Lists;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class AvgResultsExitOperationTest {

    @Test
    public void testAvgProjectionComesBackAsDouble() {
        // sharded avg calculation assumes that the avg projection implementation
        // returns a Double, so let's make sure that assumption is valid
        final AvgProjection ap = new AvgProjection("yam");
        Type[] types = ap.getTypes(null, null);
        Assert.assertNotNull(types);
        Assert.assertEquals(1, types.length);
        Assert.assertEquals(StandardBasicTypes.DOUBLE, types[0]);
    }

    @Test
    public void testEmptyList() {
        AvgResultsExitOperation op = new AvgResultsExitOperation();

        List<Object> result = op.apply(Collections.emptyList());
        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0));
    }

    @Test
    public void testSingleResult() {
        AvgResultsExitOperation op = new AvgResultsExitOperation();

        Object[] objArr = {null, 3};
        List<Object> result = op.apply(Collections.singletonList((Object) objArr));
        Assert.assertEquals(1, result.size());
        Assert.assertNull(result.get(0));

        objArr[0] = 9.0;
        result = op.apply(Collections.singletonList((Object) objArr));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(9.0, result.get(0));
    }

    @Test
    public void testMultipleResults() {
        AvgResultsExitOperation op = new AvgResultsExitOperation();

        Object[] objArr1 = {null, 3};
        Object[] objArr2 = {2.5, 2};
        List<Object> result = op.apply(Lists.<Object>newArrayList(objArr1, objArr2));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2.5, result.get(0));

        objArr1[0] = 2.0;
        result = op.apply(Lists.<Object>newArrayList(objArr1, objArr2));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2.2, result.get(0));
    }

    @Test
    public void testBadInput() {
        AvgResultsExitOperation op = new AvgResultsExitOperation();

        Object[] objArr = {null};
        try {
            op.apply(Collections.singletonList((Object) objArr));
            Assert.fail("expected rte");
        } catch (IllegalStateException rte) {
            // good
        }

        Object obj = new Object();
        try {
            op.apply(Collections.singletonList(obj));
            Assert.fail("expected rte");
        } catch (IllegalStateException rte) {
            // good
        }
    }
}
