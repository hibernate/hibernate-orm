/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.vibur;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.vibur.internal.ViburDBCPConnectionProvider;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;
import org.vibur.dbcp.ViburDBCPDataSource;
import org.vibur.dbcp.stcache.StatementHolder;
import org.vibur.dbcp.stcache.StatementMethod;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.inOrder;
import static org.vibur.dbcp.AbstractDataSourceTest.mockStatementCache;
import static org.vibur.dbcp.stcache.StatementHolder.State.AVAILABLE;

/**
 * Hibernate unit/integration test for {@link ViburDBCPConnectionProvider}.
 *
 * @author Simeon Malchev
 */
@RunWith(MockitoJUnitRunner.class)
public class ViburDBCPConnectionProviderTest extends BaseCoreFunctionalTestCase {

    private int poolMaxSize;
    private int statementCacheMaxSize;

    @Override
    protected void configure(Configuration configuration) {
        Properties properties = configuration.getProperties();
        properties.put(CONNECTION_PROVIDER, ViburDBCPConnectionProvider.class);
        properties.put(SHOW_SQL, Boolean.TRUE);
        properties.put(FORMAT_SQL, Boolean.TRUE);

        properties.put("hibernate.vibur.poolInitialSize", "1");
        properties.put("hibernate.vibur.poolMaxSize", Integer.toString(poolMaxSize));
        properties.put("hibernate.vibur.logQueryExecutionLongerThanMs", "100");
        properties.put("hibernate.vibur.statementCacheMaxSize", Integer.toString(statementCacheMaxSize));
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Actor.class };
    }

    public void setUpPoolAndDatabase(int poolMaxSize, int statementCacheMaxSize) {
        this.poolMaxSize = poolMaxSize;
        this.statementCacheMaxSize = statementCacheMaxSize;
        buildSessionFactory();

        doInHibernate(this::sessionFactory, session -> {
            addDbRecord(session, "CHRISTIAN", "GABLE");
            addDbRecord(session, "CHRISTIAN", "AKROYD");
            addDbRecord(session, "CHRISTIAN", "NEESON");
            addDbRecord(session, "CAMERON", "NEESON");
            addDbRecord(session, "RAY", "JOHANSSON");
        });
    }

    private static void addDbRecord(Session session, String firstName, String lastName) {
        Actor actor = new Actor();
        actor.setFirstName(firstName);
        actor.setLastName(lastName);
        session.persist(actor);
    }

    @After
    public void tearDown() {
        releaseSessionFactory();
    }

    @Captor
    private ArgumentCaptor<StatementMethod> key1, key2;
    @Captor
    private ArgumentCaptor<StatementHolder> val1;

    @Test
    public void testSelectStatementNoStatementsCache() {
        setUpPoolAndDatabase(2, 0 /* disables the statements cache */ );

        doInHibernate(this::sessionFactory, ViburDBCPConnectionProviderTest::executeAndVerifySelect);
    }

    @Test
    public void testSelectStatementWithStatementsCache() {
        setUpPoolAndDatabase(1, 10 /* statement cache is enabled */ );

        ConnectionProvider cp = sessionFactory().getServiceRegistry().getService(ConnectionProvider.class);
        ViburDBCPDataSource ds = ((ViburDBCPConnectionProvider) cp).getDataSource();

        ConcurrentMap<StatementMethod, StatementHolder> mockedStatementCache = mockStatementCache(ds);

        doInHibernate(this::sessionFactory, ViburDBCPConnectionProviderTest::executeAndVerifySelect);

        // We set above the poolMaxSize = 1, that's why the second session will get and use the same underlying connection.
        doInHibernate(this::sessionFactory, ViburDBCPConnectionProviderTest::executeAndVerifySelect);

        InOrder inOrder = inOrder(mockedStatementCache);
        inOrder.verify(mockedStatementCache).get(key1.capture());
        inOrder.verify(mockedStatementCache).putIfAbsent(same(key1.getValue()), val1.capture());
        inOrder.verify(mockedStatementCache).get(key2.capture());

        assertEquals(1, mockedStatementCache.size());
        assertTrue(mockedStatementCache.containsKey(key1.getValue()));
        assertEquals(key1.getValue(), key2.getValue());
        assertEquals(AVAILABLE, val1.getValue().state().get());
    }

    @SuppressWarnings("unchecked")
    private static void executeAndVerifySelect(Session session) {
        List<Actor> list = session.createQuery("from Actor where firstName = ?0")
                .setParameter(0, "CHRISTIAN").list();

        Set<String> expectedLastNames = new HashSet<>(Arrays.asList("GABLE", "AKROYD", "NEESON"));
        assertEquals(expectedLastNames.size(), list.size());
        for (Actor actor : list) {
            assertTrue(expectedLastNames.remove(actor.getLastName()));
        }
    }

    @Entity(name="Actor")
    public static class Actor {
        @Id
        @GeneratedValue
        private Long id;

        private String firstName;
        private String lastName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
