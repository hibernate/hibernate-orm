/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests the "treat" keyword in HQL.
 *
 * @author Etienne Miret
 * @author Steve Ebersole
 *
 * @see org.hibernate.test.jpa.ql.TreatKeywordTest
 */
public class TreatKeywordTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9342" )
	public void memberOfTreatTest() {
		// prepare test data
		Session s = openSession();
		s.getTransaction().begin();

		Human owner = new Human();
		s.persist( owner );

		Dog wildDog = new Dog();
		s.persist( wildDog );

		Dog petDog = new Dog();
		petDog.setOwner( owner );
		s.persist( petDog );

		Cat petCat = new Cat();
		petCat.setOwner( owner );
		s.persist( petCat );

		s.getTransaction().commit();
		s.close();


		// perform test
		s = openSession();
		s.getTransaction().begin();
		Query q = s.createQuery(
				"select pet" +
						" from Animal pet, Animal owner" +
						" where pet member of treat (owner as Human).pets"
		);
		@SuppressWarnings("unchecked")
		List<Animal> results = q.list();
		assertEquals( 2, results.size() );
		s.getTransaction().commit();
		s.close();


		// clean up test data
		s = openSession();
		s.getTransaction().begin();
		s.delete( petCat );
		s.delete( petDog );
		s.delete( wildDog );
		s.delete( owner );
		s.getTransaction().commit();
		s.close();
	}
}
