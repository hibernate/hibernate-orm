/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.userguide.collections.type.CommaDelimitedStringsJavaTypeDescriptor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
public class BasicTypeCollectionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void testLifecycle() {
		doInHibernate(this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			session.persist(person);

			//tag::collections-comma-delimited-collection-lifecycle-example[]
			person.phones.add("027-123-4567");
			person.phones.add("028-234-9876");
			session.flush();
			person.getPhones().remove(0);
			//end::collections-comma-delimited-collection-lifecycle-example[]
		});
	}

	//tag::collections-comma-delimited-collection-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@JavaType(CommaDelimitedStringsJavaTypeDescriptor.class)
		@JdbcTypeCode(Types.VARCHAR)
		private List<String> phones = new ArrayList<>();

		public List<String> getPhones() {
			return phones;
		}
	}
	//end::collections-comma-delimited-collection-example[]
}
