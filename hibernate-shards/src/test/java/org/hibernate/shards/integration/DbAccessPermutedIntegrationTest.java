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

package org.hibernate.shards.integration;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class DbAccessPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

  public void testAccess() throws SQLException {
    Set<? extends SessionFactory> sfSet = ((ShardedSessionFactoryImplementor)sf).getSessionFactoryShardIdMap().keySet();
    for(SessionFactory sf : sfSet) {
      testShard(sf);
      testShard(sf);
      testShard(sf);
    }
  }

  private void testShard(SessionFactory sf) throws SQLException {
    Session session = sf.openSession();
    try {
      insertRecord(session);
      updateRecord(session);
      selectRecord(session);
      deleteRecord(session);
    } finally {
      session.close();
    }
  }

  private void insertRecord(Session session) throws SQLException {
    assertEquals(1, session.createSQLQuery("INSERT INTO sample_table(id, str_col) values (0, 'yam')").executeUpdate());
  }

  private void updateRecord(Session session) throws SQLException {
    assertEquals(1, session.createSQLQuery("UPDATE sample_table set str_col = 'max' where id = 0").executeUpdate());
  }

  private void selectRecord(Session session) throws SQLException {
    SQLQuery query = session.createSQLQuery("select id, str_col from sample_table where id = 0");
    List results = query.list();
    assertEquals(1, results.size());
    Object[] result = (Object[]) results.get(0);
    assertEquals(new BigDecimal(0), result[0]);
    assertEquals("max", result[1]);
  }

  private void deleteRecord(Session session) throws SQLException {
    assertEquals(1, session.createSQLQuery("DELETE from sample_table where id = 0").executeUpdate());
  }
}
