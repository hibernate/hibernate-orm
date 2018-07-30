/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.sql;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.hibernate.Session;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Where;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQL82Dialect.class)
public class CustomSQLTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try(Statement statement = connection.createStatement(); ) {
					statement.executeUpdate( "ALTER TABLE person ADD COLUMN valid boolean" );
					statement.executeUpdate( "ALTER TABLE Person_phones ADD COLUMN valid boolean" );
				}
			} );
		});
	}

	@Test
	public void test_sql_custom_crud() {

		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setName( "John Doe" );
			entityManager.persist( person );
			person.getPhones().add( "123-456-7890" );
			person.getPhones().add( "123-456-0987" );
			return person;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertEquals( 2, person.getPhones().size() );
			person.getPhones().remove( 0 );
			person.setName( "Mr. John Doe" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertEquals( 1, person.getPhones().size() );
			entityManager.remove( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNull(person);
		} );
	}

	//tag::sql-custom-crud-example[]
	@Entity(name = "Person")
	@SQLInsert(
		sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, true) ",
		check = ResultCheckStyle.COUNT
	)
	@SQLUpdate(
		sql = "UPDATE person SET name = ? where id = ? "
	)
	@SQLDelete(
		sql = "UPDATE person SET valid = false WHERE id = ? "
	)
	@Loader(namedQuery = "find_valid_person")
	@NamedNativeQueries({
		@NamedNativeQuery(
			name = "find_valid_person",
			query = "SELECT id, name " +
					"FROM person " +
					"WHERE id = ? and valid = true",
			resultClass = Person.class
		)
	})
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ElementCollection
		@SQLInsert(
			sql = "INSERT INTO person_phones (person_id, phones, valid) VALUES (?, ?, true) ")
		@SQLDeleteAll(
			sql = "UPDATE person_phones SET valid = false WHERE person_id = ?")
		@Where( clause = "valid = true" )
		private List<String> phones = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::sql-custom-crud-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<String> getPhones() {
			return phones;
		}
	//tag::sql-custom-crud-example[]
	}
	//end::sql-custom-crud-example[]
}
