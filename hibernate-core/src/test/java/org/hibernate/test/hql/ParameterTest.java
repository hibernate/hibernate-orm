/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
