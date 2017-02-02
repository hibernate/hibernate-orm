/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.PersistenceException;

import org.hibernate.AnnotationException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

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
		catch ( PersistenceException ex ) {
			// expected
			assertTrue(ex.getMessage().contains("changed an immutable collection instance"));
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
		} catch (PersistenceException e) {
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
	
	@Test
	public void testImmutableAttribute(){
		configuration().addAttributeConverter( ExifConverter.class);
		configuration().addAttributeConverter( CaptionConverter.class);
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Photo photo = new Photo();
		photo.setName( "cat.jpg");
		photo.setMetadata( new Exif(Collections.singletonMap( "fake", "first value")));
		photo.setCaption( new Caption( "Cat.jpg caption" ) );
		s.persist(photo);
		tx.commit();
		s.close();

		// try changing the attribute
		s = openSession();
		tx = s.beginTransaction();

		Photo cat = s.get(Photo.class, photo.getId());
		assertNotNull(cat);
		cat.getMetadata().getAttributes().put( "fake", "second value");
		cat.getCaption().setText( "new caption" );

		tx.commit();
		s.close();

		// retrieving the attribute again - it should be unmodified since object identity is the same
		s = openSession();
		tx = s.beginTransaction();

		cat = s.get(Photo.class, photo.getId());
		assertNotNull(cat);
		assertEquals("Metadata should not have changed", "first value", cat.getMetadata().getAttribute( "fake"));
		assertEquals("Caption should not have changed", "Cat.jpg caption", cat.getCaption().getText());

		tx.commit();
		s.close();
	}

	@Test
	public void testChangeImmutableAttribute(){
		configuration().addAttributeConverter( ExifConverter.class);
		configuration().addAttributeConverter( CaptionConverter.class);

		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Photo photo = new Photo();
		photo.setName( "cat.jpg");
		photo.setMetadata( new Exif(Collections.singletonMap( "fake", "first value")));
		photo.setCaption( new Caption( "Cat.jpg caption" ) );
		s.persist(photo);

		tx.commit();
		s.close();

		// replacing the attribute
		s = openSession();
		tx = s.beginTransaction();

		Photo cat = s.get(Photo.class, photo.getId());
		assertNotNull(cat);
		cat.setMetadata( new Exif(Collections.singletonMap( "fake", "second value")));
		cat.setCaption( new Caption( "new caption" ) );

		tx.commit();
		s.close();

		// retrieving the attribute again - it should be modified since the holder object has changed as well
		s = openSession();
		tx = s.beginTransaction();

		cat = s.get(Photo.class, photo.getId());
		assertNotNull(cat);

		assertEquals("Metadata should have changed", "second value", cat.getMetadata().getAttribute( "fake"));
		assertEquals("Caption should have changed", "new caption", cat.getCaption().getText());

		tx.commit();
		s.close();
	}
	
	@Test
	public void testMisplacedImmutableAnnotation() {
		MetadataSources metadataSources = new MetadataSources().addAnnotatedClass( Foobar.class );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting exception due to misplaced @Immutable annotation");
		}
		catch (AnnotationException ignore) {
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}

	@Override
    protected Class[] getAnnotatedClasses() {
		return new Class[] { Country.class, State.class, Photo.class };
	}
}
