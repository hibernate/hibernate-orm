/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.guide.collection;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.jpa.test.util.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BasicTypeElementCollectionTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( BasicTypeCollectionTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	public void buildEntityManagerFactory() throws Exception {
		super.buildEntityManagerFactory();
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.phones.add( "027-123-4567" );
			person.phones.add( "028-234-9876" );
			entityManager.persist( person );
		} );
	}

	@Test
	public void testProxies() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			assertEquals( 2, person.getPhones().size() );
			try {
				ArrayList<String> phones = (ArrayList<String>) person.getPhones();
			}
			catch (Exception expected) {
				log.error( "Failure", expected );
			}
		} );
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			log.info( "Clear element collection and add element" );
			person.getPhones().clear();
			person.getPhones().add( "123-456-7890" );
			person.getPhones().add( "456-000-1234" );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			log.info( "Remove one element" );
			person.getPhones().remove( 0 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@ElementCollection
		private List<String> phones = new ArrayList<>();

		public List<String> getPhones() {
			return phones;
		}
	}
}
