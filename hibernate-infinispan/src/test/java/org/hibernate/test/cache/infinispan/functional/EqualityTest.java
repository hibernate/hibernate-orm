package org.hibernate.test.cache.infinispan.functional;

import java.util.concurrent.Callable;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Persons should be correctly indexed since we can use Type for comparison
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class EqualityTest extends SingleNodeTestCase {
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] { Person.class };
    }

    @Test
    public void testEqualityFromType() throws Exception {
        Person john = new Person("John", "Black", 26);
        Person peter = new Person("Peter", "White", 32);

        withTx(tm, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Session session = openSession();
                session.getTransaction().begin();
                session.persist(john);
                session.persist(peter);
                session.getTransaction().commit();
                session.close();
                return null;
            }
        });

        Statistics statistics = sessionFactory().getStatistics();
        statistics.clear();

        for (int i = 0; i < 5; ++i) {
            withTx(tm, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Session session = openSession();
                    session.getTransaction().begin();
                    Person p1 = session.get(Person.class, john.name);
                    assertPersonEquals(john, p1);
                    Person p2 = session.get(Person.class, peter.name);
                    assertPersonEquals(peter, p2);
                    Person p3 = session.get(Person.class, new Name("Foo", "Bar"));
                    assertNull(p3);
                    session.getTransaction().commit();
                    session.close();
                    return null;
                }
            });
        }

        assertTrue(statistics.getSecondLevelCacheHitCount() > 0);
        assertTrue(statistics.getSecondLevelCacheMissCount() > 0);
    }

    private static void assertPersonEquals(Person expected, Person person) {
        assertNotNull(person);
        assertNotNull(person.getName());
        assertEquals(expected.getName().getFirstName(), person.getName().getFirstName());
        assertEquals(expected.getName().getLastName(), person.getName().getLastName());
        assertEquals(expected.getAge(), person.getAge());
    }
}
