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

import static org.junit.Assert.*;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests the "treat" keyword in HQL.
 *
 * @author Etienne Miret
 */
public class TreatTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "hql/Animal.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9342" )
	public void memberOfTreatTest() {
		final Session s = openSession();

		s.getTransaction().begin();

		final Human owner = new Human();
		s.persist( owner );

		final Dog wildDog = new Dog();
		s.persist( wildDog );

		final Dog petDog = new Dog();
		petDog.setOwner( owner );
		s.persist( petDog );

		final Cat petCat = new Cat();
		petCat.setOwner( owner );
		s.persist( petCat );

		s.getTransaction().commit();

		final Query q = s.createQuery( "select pet"
				+ " from Animal pet, Animal owner"
				+ " where pet member of treat (owner as Human).pets"
		);
		@SuppressWarnings("unchecked")
		final List<Animal> results = q.list();

		assertEquals( 2, results.size() );

		s.close();
	}

}
