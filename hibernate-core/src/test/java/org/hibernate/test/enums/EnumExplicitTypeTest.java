/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumExplicitTypeTest extends BaseCoreFunctionalTestCase {

	protected String[] getMappings() {
		return new String[] { "enums/Person.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10766")
	public void hbmEnumWithExplicitTypeTest() {
		Session s = openSession();
		s.getTransaction().begin();
		Person painted = Person.person( Gender.MALE, HairColor.BROWN );
		painted.setOriginalHairColor( HairColor.BLONDE );
		s.persist( painted );
		s.getTransaction().commit();
		s.clear();

		s.getTransaction().begin();
		Number id = (Number) session.createNativeQuery(
				"select id from Person where originalHairColor = :color" )
				.setParameter( "color", HairColor.BLONDE.name() )
				.getSingleResult();
		assertEquals( 1L, id.longValue() );
		s.getTransaction().commit();
		s.close();
	}
}
