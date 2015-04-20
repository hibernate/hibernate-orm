/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.immutable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for <code>Immutable</code> annotation.
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class ImmutableTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( ImmutableTest.class );

	@Test
	public void testImmutableEntity() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Country country = new Country();
		country.setName("Germany");
		s.persist(country);
		tx.commit();
		s.close();

		// try changing the entity
		s = openSession();
		tx = s.beginTransaction();
		Country germany = (Country) s.get(Country.class, country.getId());
		assertNotNull(germany);
		germany.setName("France");
		assertEquals("Local name can be changed", "France", germany.getName());
		s.save(germany);
		tx.commit();
		s.close();

		// retrieving the country again - it should be unmodified
		s = openSession();
		tx = s.beginTransaction();
		germany = (Country) s.get(Country.class, country.getId());
		assertNotNull(germany);
		assertEquals("Name should not have changed", "Germany", germany.getName());
		tx.commit();
		s.close();
	}

	@Test
	public void testImmutableCollection() {
		Country country = new Country();
		country.setName("Germany");
		List states = new ArrayList<State>();
		State bayern = new State();
		bayern.setName("Bayern");
		State hessen = new State();
		hessen.setName("Hessen");
		State sachsen = new State();
		sachsen.setName("Sachsen");
		states.add(bayern);
		states.add(hessen);
		states.add(sachsen);
		country.setStates(states);

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist(country);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Country germany = (Country) s.get(Country.class, country.getId());
		assertNotNull(germany);
		assertEquals("Wrong number of states", 3, germany.getStates().size());

		// try adding a state
		State foobar = new State();
		foobar.setName("foobar");
		s.save(foobar);
		germany.getStates().add(foobar);
		try {
			tx.commit();
			fail();
		}
		catch (HibernateException e) {
			assertTrue(e.getCause().getMessage().contains("changed an immutable collection instance"));
            log.debug("success");
		}
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		germany = (Country) s.get(Country.class, country.getId());
		assertNotNull(germany);
		assertEquals("Wrong number of states", 3, germany.getStates().size());

		// try deleting a state
		germany.getStates().remove(0);
		try {
			tx.commit();
			fail();
		} catch (HibernateException e) {
			assertTrue(e.getCause().getMessage().contains("changed an immutable collection instance"));
            log.debug("success");
		}
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		germany = (Country) s.get(Country.class, country.getId());
		assertNotNull(germany);
		assertEquals("Wrong number of states", 3, germany.getStates().size());
		tx.commit();
		s.close();
	}

	@Test
	public void testMisplacedImmutableAnnotation() {
		try {
			new MetadataSources().addAnnotatedClass( Foobar.class ).buildMetadata();
			fail( "Expecting exception due to misplaced @Immutable annotation");
		}
		catch (AnnotationException ignore) {
		}
	}

	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[] { Country.class, State.class};
	}
}
