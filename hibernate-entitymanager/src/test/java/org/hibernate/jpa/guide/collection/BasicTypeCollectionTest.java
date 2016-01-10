/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.guide.collection;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.guide.collection.type.CommaDelimitedStringsType;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.jpa.test.util.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
public class BasicTypeCollectionTest extends BaseCoreFunctionalTestCase {

	private static final Logger log = Logger.getLogger( BasicTypeCollectionTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void testLifecycle() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			person.phones.add( "027-123-4567" );
			person.phones.add( "028-234-9876" );
			session.persist( person );
		} );
		doInHibernate( this::sessionFactory, session -> {
			Person person = session.get( Person.class, 1L );
			log.infov( "Remove one element" );
			person.getPhones().remove( 0 );
		} );
	}

	@Override
	protected Configuration constructAndConfigureConfiguration() {
		Configuration configuration = super.constructAndConfigureConfiguration();
		configuration.registerTypeContributor( (typeContributions, serviceRegistry) -> {
			typeContributions.contributeType( new CommaDelimitedStringsType() );
		} );
		return configuration;
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@Type(type = "comma_delimited_strings")
		private List<String> phones = new ArrayList<>();

		public List<String> getPhones() {
			return phones;
		}
	}
}
