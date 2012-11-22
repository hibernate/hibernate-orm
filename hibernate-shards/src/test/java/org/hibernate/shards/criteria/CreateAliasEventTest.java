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

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.shards.defaultmock.CriteriaDefaultMock;
import org.hibernate.sql.JoinType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author maxr@google.com (Max Ross)
 */
public class CreateAliasEventTest {

    @Test
    public void testOnOpenSession() {
        final CreateAliasEvent event = new CreateAliasEvent(null, null);
        final boolean[] called = {false};
        final Criteria crit = new CriteriaDefaultMock() {
            @Override
            public Criteria createAlias(String associationPath, String alias) throws HibernateException {
                called[0] = true;
                return null;
            }
        };
        event.onEvent(crit);
        Assert.assertTrue(called[0]);
    }

    @Test
    public void testOnOpenSessionWithJoinType() {
        final CreateAliasEvent event = new CreateAliasEvent(null, null, JoinType.INNER_JOIN);
        final boolean[] called = {false};
        final Criteria crit = new CriteriaDefaultMock() {
            @Override
            public Criteria createAlias(String associationPath, String alias,
                                        JoinType joinType) throws HibernateException {
                called[0] = true;
                return null;
            }
        };
        event.onEvent(crit);
        Assert.assertTrue(called[0]);
    }
}
