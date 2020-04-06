/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bugs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bugs.sql.ConnectionWrapper;
import org.hibernate.bugs.sql.DataSourceWrapper;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

//import com.arjuna.ats.internal.jdbc.ConnectionManager;

/**
 * 
 * 
 * @author Selaron
 */
@TestForIssue(jiraKey = "HHH-4808")
public class LazyLoadingConnectionCloseTest
    extends BaseCoreFunctionalTestCase
{

  @Override
  protected Class<?>[] getAnnotatedClasses()
  {
    return new Class[]
    {
        SimpleEntity.class, ChildEntity.class
    };
  }

  @Override
  protected void configure(final Configuration configuration)
  {

    // print SQL
    configuration.setProperty(AvailableSettings.SHOW_SQL,
                              Boolean.TRUE.toString());
    configuration.setProperty(AvailableSettings.FORMAT_SQL,
                              Boolean.TRUE.toString());

    // enable LL without TX.
    configuration.setProperty(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS,
                              Boolean.TRUE.toString());

    configuration.setProperty(AvailableSettings.CONNECTION_HANDLING,
                              PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION.name());

    configuration.setProperty(AvailableSettings.AUTOCOMMIT,
                              Boolean.FALSE.toString());

    configuration.setProperty(AvailableSettings.POOL_SIZE,
                              "5");

  }

  @Override
  protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder)
  {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setUser("sa");
    dataSource.setPassword("pw");
    dataSource.setURL("jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1");
    DataSource dataSourceWrapper = new DataSourceWrapper(dataSource);
    serviceRegistryBuilder.applySetting(AvailableSettings.DATASOURCE,
                                        dataSourceWrapper);

  }

  /**
   * Prepare and persist a {@link SimpleEntity} with two {@link ChildEntity}.
   */
  @Before
  public void prepare()
  {
    adjustLogger();
    ConnectionWrapper.currentOpenConnections.set(0);

    final Session s = openSession();

    final Transaction t = s.beginTransaction();

    try
    {
      final Number count = (Number) s.createQuery("SELECT count(ID) FROM SimpleEntity").list().stream()
          .findFirst().get();
      if (count.longValue() > 0L)
      {
        // entity already added previously
        return;
      }

      final SimpleEntity entity = new SimpleEntity();
      entity.setId(1L);
      entity.setName("TheParent");

      final ChildEntity c1 = new ChildEntity();
      c1.setId(1L);
      c1.setParent(entity);
      c1.setName("child1");

      final ChildEntity c2 = new ChildEntity();
      c2.setId(2L);
      c2.setParent(entity);
      c2.setName("child2");

      s.save(entity);
      s.save(c1);
      s.save(c2);

    }
    finally
    {
      t.commit();
      s.close();
    }
  }

  /**
   * Tests connections get closed after transaction commit.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testConnectionCloseAfterTx()
  {
    final Session s = openSession();
    final int oldOpenedConnections = ConnectionWrapper.openedConnections.get();

    final Transaction t = s.beginTransaction();
    try
    {
      final Query qry = s.createQuery("FROM SimpleEntity");
      final List<SimpleEntity> entities = qry.list();
      final SimpleEntity entity = entities.get(0);

      // assert one connection is open
      assertEquals(1,
                   ConnectionWrapper.currentOpenConnections.get());

      t.commit();

      // assert a connection had been opened
      assertTrue(oldOpenedConnections < ConnectionWrapper.openedConnections.get());
      // assert no connection is open
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

    }
    finally
    {
      s.close();
    }
  }

  /**
   * Tests connections get closed after lazy collection initialization.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testConnectionCloseAfterLazyCollectionInit()
  {

    final Session s = openSession();

    try
    {
      final Query qry = s.createQuery("FROM SimpleEntity");
      final List<SimpleEntity> entities = qry.list();
      final SimpleEntity entity = entities.get(0);

      // assert no connection is open
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

      /*
       * Test connection gets closed after lazy collection loading:
       */
      final int oldOpenedConnections = ConnectionWrapper.openedConnections.get();
      final Set<ChildEntity> lazyChildren = entity.getChildren();

      // this will initialize the collection and such trigger a query
      lazyChildren.stream().findAny();

      // assert a connection had been opened
      assertTrue(oldOpenedConnections < ConnectionWrapper.openedConnections.get());
      // assert there's no remaining connection left.
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

    }
    finally
    {
      s.close();
    }
  }

  /**
   * Tests connections get closed after transaction commit.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testConnectionCloseAfterLazyPojoPropertyInit()
  {

    final Session s = openSession();

    try
    {
      final Query qry = s.createQuery("FROM ChildEntity");
      final List<ChildEntity> entities = qry.list();
      final ChildEntity entity = entities.get(0);

      // assert no connection is open
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

      /*
       * Test connection gets closed after lazy collection loading:
       */
      final int oldOpenedConnections = ConnectionWrapper.openedConnections.get();
      final SimpleEntity parent = entity.getParent();

      // this will initialize the collection and such trigger a query
      parent.getName();

      // assert a connection had been opened
      assertTrue(oldOpenedConnections < ConnectionWrapper.openedConnections.get());
      // assert there's no remaining connection left.
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

    }
    finally
    {
      s.close();
    }
  }

  /**
   * Tests connections get closed after transaction commit.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testConnectionCloseAfterQueryWithoutTx()
  {

    final Session s = openSession();

    try
    {

      final int oldOpenedConnections = ConnectionWrapper.openedConnections.get();
      final List<ChildEntity> childrenByQuery = s.createQuery("FROM ChildEntity").list();
      assertTrue(childrenByQuery.size() > 0);

      // assert a connection had been opened
      assertTrue(oldOpenedConnections < ConnectionWrapper.openedConnections.get());
      // assert there's no remaining connection left.
      assertEquals(0,
                   ConnectionWrapper.currentOpenConnections.get());

    }
    finally
    {
      s.close();
    }
  }

  private void adjustLogger()
  {
    Logger.getRootLogger().setLevel(Level.WARN);
    Logger.getLogger("test").setLevel(Level.INFO);
    Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%-5p [%-20.20c{1}] %m%n")));
    //		Logger.getLogger(ConnectionManager.class).setLevel(Level.DEBUG);
    Logger.getLogger("org.hibernate.engine.transaction").setLevel(Level.TRACE);
  }

}