package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.stat.Statistics;
import org.hibernate.test.cache.infinispan.functional.entities.Name;
import org.hibernate.test.cache.infinispan.functional.entities.Person;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Persons should be correctly indexed since we can use Type for comparison
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class EqualityTest extends SingleNodeTest {
	 @Override
	 public List<Object[]> getParameters() {
		  return getParameters(true, true, true, true);
	 }

	 @Override
	 protected Class[] getAnnotatedClasses() {
		  return new Class[] { Person.class };
	 }

	 @Test
	 public void testEqualityFromType() throws Exception {
		  Person john = new Person("John", "Black", 26);
		  Person peter = new Person("Peter", "White", 32);

		  withTxSession(s -> {
				s.persist(john);
				s.persist(peter);
		  });

		  Statistics statistics = sessionFactory().getStatistics();
		  statistics.clear();

		  for (int i = 0; i < 5; ++i) {
				withTxSession(s -> {
					 Person p1 = s.get(Person.class, john.getName());
					 assertPersonEquals(john, p1);
					 Person p2 = s.get(Person.class, peter.getName());
					 assertPersonEquals(peter, p2);
					 Person p3 = s.get(Person.class, new Name("Foo", "Bar"));
					 assertNull(p3);
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
