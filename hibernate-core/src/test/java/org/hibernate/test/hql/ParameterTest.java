/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Isolated test for various usages of parameters
 *
 * @author Steve Ebersole
 */
public class ParameterTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9154" )
	public void testClassAsParameter() {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "from Human h where h.name = :class" ).setParameter( "class", new Name() ).list();
		s.createQuery( "from Human where name = :class" ).setParameter( "class", new Name() ).list();
		s.createQuery( "from Human h where :class = h.name" ).setParameter( "class", new Name() ).list();
		s.createQuery( "from Human h where :class <> h.name" ).setParameter( "class", new Name() ).list();

		s.getTransaction().commit();
		s.close();
	}


	@Test
	@TestForIssue( jiraKey = "HHH-9154" )
	public void testObjectAsParameter() {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "from Human h where h.name = :OBJECT" ).setParameter( "OBJECT", new Name() ).list();
		s.createQuery( "from Human where name = :OBJECT" ).setParameter( "OBJECT", new Name() ).list();
		s.createQuery( "from Human h where :OBJECT = h.name" ).setParameter( "OBJECT", new Name() ).list();
		s.createQuery( "from Human h where :OBJECT <> h.name" ).setParameter( "OBJECT", new Name() ).list();

		s.getTransaction().commit();
		s.close();
	}
}
