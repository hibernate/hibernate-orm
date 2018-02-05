/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass3;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author pholvs
 */
public class UnionSubclassTest extends BaseCoreFunctionalTestCase {
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ClassOfInterest.class, OtherClassIface.class, OtherClassA.class, OtherClassB.class};
	}

	/** Core problem: when generating a query with multiple TYPE() or .class clauses, an ambiguous column error is generated.
	 * This is because there are multiple columns named clazz_ - one for each union-select the query generates.
	 * eg: WHERE clazz_ = 1 AND clazz_ = 2
	 * Fix: Add the alias for the union select before the clazz_ clause in generated SQL query
	 * eg: WHERE myAlias1.clazz_ = 1 AND myAlias2.clazz_ = 2
	 */
	@Test
	public void testUnionSubclassClassQueryGeneration() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		try{
			Query q = s.createQuery("from ClassOfInterest c"
					+ " left join c.otherEntity myAlias1"
					+ " left join c.otherEntity myAlias2"
					+ " where TYPE(myAlias1) = OtherClassA"
					+ " and TYPE(myAlias2) = OtherClassB"
					+ " and (myAlias1.fieldA = 'foo' or myAlias2.fieldB = 'bar')");
			List l = q.getResultList();
		}
		finally
		{
			t.commit();
			s.close();
		}
	}

	/** If correct query can be generated, double check that results are correct
	 *
	 */
	@Test
	public void testUnionSubclassClassResults() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		OtherClassA childA = new OtherClassA();
		childA.id = 1L;
		childA.fieldA = "foo";

		ClassOfInterest objA = new ClassOfInterest();
		objA.id = 2L;
		objA.otherEntity = childA;

		OtherClassB childB = new OtherClassB();
		childB.id = 3L;
		childB.fieldB = "bar";

		ClassOfInterest objB = new ClassOfInterest();
		objB.id = 4L;
		objB.otherEntity = childB;

		OtherClassB childC = new OtherClassB();
		childC.id = 5L;
		childC.fieldB = "baz";

		ClassOfInterest objC = new ClassOfInterest();
		objC.id = 6L;
		objC.otherEntity = childC;

		OtherClassB childD = new OtherClassB();
		childD.id = 7L;
		childD.fieldB = "dog";

		ClassOfInterest objD = new ClassOfInterest();
		objD.id = 8L;
		objD.otherEntity = childD;

		s.save(childA);
		s.save(childB);
		s.save(childC);
		s.save(childD);

		s.save(objA);
		s.save(objB);
		s.save(objC);
		s.save(objD);

		try{
			Query q = s.createQuery("from ClassOfInterest c"
					+ " left join c.otherEntity myAlias1"
					+ " left join c.otherEntity myAlias2"
					+ " where TYPE(myAlias1) = OtherClassA"
					+ " and TYPE(myAlias2) = OtherClassB"
					+ " and (myAlias1.fieldA = 'foo' or myAlias2.fieldB = 'bar')");
			List l = q.getResultList();
			assertEquals(l.size(), 2);
		}
		finally
		{
			s.delete(objA);
			s.delete(objB);
			s.delete(objC);
			s.delete(objD);

			s.delete(childA);
			s.delete(childB);
			s.delete(childC);
			s.delete(childD);

			t.commit();
			s.close();
		}
	}
	
}

