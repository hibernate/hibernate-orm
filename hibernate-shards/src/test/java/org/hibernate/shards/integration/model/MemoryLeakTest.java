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

package org.hibernate.shards.integration.model;

import junit.framework.TestCase;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.StatefulPersistenceContext;
import org.hibernate.impl.SessionImpl;
import org.hibernate.shards.integration.IdGenType;
import org.hibernate.shards.integration.MemoryLeakPlugger;
import static org.hibernate.shards.integration.model.ModelDataFactory.person;
import static org.hibernate.shards.integration.model.ModelDataFactory.tenant;
import org.hibernate.shards.integration.platform.DatabasePlatform;
import org.hibernate.shards.integration.platform.DatabasePlatformFactory;
import org.hibernate.shards.model.Person;
import org.hibernate.shards.model.Tenant;
import org.hibernate.shards.util.JdbcUtil;
import org.hibernate.shards.util.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author maxr@google.com (Max Ross)
 */
public class MemoryLeakTest extends TestCase {

  private SessionFactory sf;
  private Session session;

  private Connection getConnection(DatabasePlatform dbPlatform) throws
      SQLException {
    return
      DriverManager.getConnection(
          dbPlatform.getUrl(0),
          dbPlatform.getUser(),
          dbPlatform.getPassword());
  }

  private void deleteDatabase(DatabasePlatform dbPlatform) throws SQLException {
    Connection conn = getConnection(dbPlatform);
    try {
      for(String statement : dbPlatform.getDropTableStatements(IdGenType.SIMPLE)) {
        try {
          JdbcUtil.executeUpdate(conn, statement, false);
        } catch (SQLException sqle) {
          // not interested, keep moving
        }
      }
    } finally {
      conn.close();
    }
  }

  private void createDatabase(DatabasePlatform dbPlatform) throws SQLException {
    Connection conn = getConnection(dbPlatform);
    try {
      for(String statement : dbPlatform.getCreateTableStatements(IdGenType.SIMPLE)) {
        JdbcUtil.executeUpdate(conn, statement, false);
      }
    } finally {
      conn.close();
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    deleteDatabase(dbPlatform);
    createDatabase(dbPlatform);

    String dbPlatformConfigDirectory = "../platform/" + dbPlatform.getName().toLowerCase() + "/" + "config/";
    Configuration configuration = new Configuration();
    configuration.configure(getClass().getResource(dbPlatformConfigDirectory + "shard0.hibernate.cfg.xml"));
    configuration.addURL(getClass().getResource(dbPlatformConfigDirectory + IdGenType.SIMPLE.getMappingFile()));

    sf = configuration.buildSessionFactory();
    session = sf.openSession();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      MemoryLeakPlugger.plug((SessionImpl)session);
      session.close();
    } finally {
      sf.close();
    }
    super.tearDown();
  }

  public void testLeak() throws SQLException, ClassNotFoundException,
      NoSuchFieldException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException {
    Person p;
    try {
      session.beginTransaction();
      p = person("max", null);
      Tenant t = tenant("tenant", null, Lists.newArrayList(p));
      session.save(t);
      session.getTransaction().commit();
    } finally {
      session.close();
    }
    session = sf.openSession();
    session.get(Person.class, p.getPersonId());
    Field f = StatefulPersistenceContext.class.getDeclaredField("proxiesByKey");
    boolean isAccessible = f.isAccessible();
    f.setAccessible(true);
    try {
      SessionImpl sessionImpl = (SessionImpl)session;
      Map map = (Map) f.get(sessionImpl.getPersistenceContext());
      assertEquals(1, map.size());
    } finally {
      f.setAccessible(isAccessible);
    }

    MemoryLeakPlugger.plug((SessionImpl)session);
  }
}
