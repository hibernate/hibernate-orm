/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.sql;

import java.sql.Statement;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;

import org.hibernate.Session;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Before;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle8iDialect.class)
public class OracleCustomSQLWithStoredProcedureTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( OracleCustomSQLWithStoredProcedureTest.class );

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
					statement.executeUpdate( "ALTER TABLE person ADD valid NUMBER(1) DEFAULT 0 NOT NULL" );
					//tag::sql-sp-soft-delete-example[]
					statement.executeUpdate(
						"CREATE OR REPLACE PROCEDURE sp_delete_person ( " +
						"   personId IN NUMBER ) " +
						"AS  " +
						"BEGIN " +
						"    UPDATE person SET valid = 0 WHERE id = personId; " +
						"END;"
					);}
				//end::sql-sp-soft-delete-example[]
			} );
		});
	}

	@Test
	public void test_sql_custom_crud() {

		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setName( "John Doe" );
			entityManager.persist( person );
			return person;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNotNull(person);
			entityManager.remove( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNull(person);
		} );
	}


	@Entity(name = "Person")
	@SQLInsert(
		sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, 1) ",
		check = ResultCheckStyle.COUNT
	)
	//tag::sql-sp-custom-crud-example[]
	@SQLDelete(
		sql =   "{ call sp_delete_person( ? ) } ",
		callable = true
	)
	//end::sql-sp-custom-crud-example[]
	@Loader(namedQuery = "find_valid_person")
	@NamedNativeQueries({
		@NamedNativeQuery(
			name = "find_valid_person",
			query = "SELECT id, name " +
					"FROM person " +
					"WHERE id = ? and valid = 1",
			resultClass = Person.class
		)
	})
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

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
	}

}
