/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.pc;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadeReplicateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class
		};
	}

	@Test
	public void refreshTest() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");

			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("123-456-7890");

			person.addPhone(phone);
			entityManager.persist(person);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {

			//tag::pc-cascade-replicate-example[]
			Person person = new Person();
			person.setId(1L);
			person.setName("John Doe Sr.");

			Phone phone = new Phone();
			phone.setId(1L);
			phone.setNumber("(01) 123-456-7890");
			person.addPhone(phone);

			entityManager.unwrap(Session.class).replicate(person, ReplicationMode.OVERWRITE);
			//end::pc-cascade-replicate-example[]
		});
	}
}
