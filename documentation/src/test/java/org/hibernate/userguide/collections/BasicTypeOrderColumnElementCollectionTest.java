/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class BasicTypeOrderColumnElementCollectionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.id = 1L;
			person.getPhones().add( "123-456-7890" );
			person.getPhones().add( "456-000-1234" );
			entityManager.persist( person );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			log.info( "Remove one element" );
			//tag::collections-value-type-collection-order-column-remove-example[]
			person.getPhones().remove( 0 );
			//end::collections-value-type-collection-order-column-remove-example[]
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		//tag::collections-value-type-collection-order-column-remove-entity-example[]
		@ElementCollection
		@OrderColumn(name = "order_id")
		private List<String> phones = new ArrayList<>();
		//end::collections-value-type-collection-order-column-remove-entity-example[]

		public List<String> getPhones() {
			return phones;
		}
	}
}
