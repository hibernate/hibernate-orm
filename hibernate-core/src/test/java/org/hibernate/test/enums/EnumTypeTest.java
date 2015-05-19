/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Brett Meyer
 */
public class EnumTypeTest extends BaseCoreFunctionalTestCase {

	protected String[] getMappings() {
		return new String[] { "enums/Person.hbm.xml" };
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8153")
	public void hbmEnumTypeTest() {
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( Person.person( Gender.MALE, HairColor.BROWN ) );
		s.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
		s.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
		s.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
		s.getTransaction().commit();
		s.clear();
		
		s.getTransaction().begin();
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.MALE ) )
				.list().size(), 2);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.MALE ) )
				.add( Restrictions.eq( "hairColor", HairColor.BROWN ) )
				.list().size(), 1);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.FEMALE ) )
				.list().size(), 2);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.FEMALE ) )
				.add( Restrictions.eq( "hairColor", HairColor.BROWN ) )
				.list().size(), 1);
		s.getTransaction().commit();
		s.close();
	}
}
