//$Id$
package org.hibernate.test.annotations.immutable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.test.annotations.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for <code>Immutable</code> annotation.
 * 
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class ImmutableTest extends TestCase {

	private Logger log = LoggerFactory.getLogger(ImmutableTest.class);

	public ImmutableTest(String x) {
		super(x);
	}

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
		
//		// try deletion
//		s = openSession();
//		tx = s.beginTransaction();
//		s.delete(germany);
//		tx.commit();
//		s.close();
//		
//		s = openSession();
//		tx = s.beginTransaction();
//		germany = (Country) s.get(Country.class, country.getId());
//		assertNotNull(germany);
//		assertEquals("Name should not have changed", "Germany", germany.getName());
//		s.close();
	}
	
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
		} catch (HibernateException e) {
			assertTrue(e.getMessage().contains("changed an immutable collection instance"));
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
			assertTrue(e.getMessage().contains("changed an immutable collection instance"));
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
	
	public void testMiscplacedImmutableAnnotation() {
		try {
			AnnotationConfiguration config = new AnnotationConfiguration();
			config.addAnnotatedClass(Foobar.class);
			config.buildSessionFactory();
			fail();
		} catch (AnnotationException ae) {
			log.debug("succes");
		}
	}
	
	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Country.class, State.class};
	}
}
