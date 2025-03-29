/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import java.sql.Statement;

import org.hibernate.Session;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = OracleDialect.class)
@Jpa(
		annotatedClasses = OracleCustomSQLWithStoredProcedureTest.Person.class
)
public class OracleCustomSQLWithStoredProcedureTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement();) {
					statement.executeUpdate( "ALTER TABLE person ADD valid NUMBER(1) DEFAULT 0 NOT NULL" );
					//tag::sql-sp-soft-delete-example[]
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_delete_person (" +
									"   personId IN NUMBER) " +
									"AS  " +
									"BEGIN " +
									"    UPDATE person SET valid = 0 WHERE id = personId; " +
									"END;"
					);
				}
				//end::sql-sp-soft-delete-example[]
			} );
		} );
	}

	@Test
	public void test_sql_custom_crud(EntityManagerFactoryScope scope) {

		Person _person = scope.fromTransaction( entityManager -> {
			Person person = new Person();
			person.setName( "John Doe" );
			entityManager.persist( person );
			return person;
		} );

		scope.inTransaction(  entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNotNull( person );
			entityManager.remove( person );
		} );

		scope.inTransaction(  entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertNull( person );
		} );
	}


	@Entity(name = "Person")
	@SQLInsert(
			sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, 1) ",
			check = ResultCheckStyle.COUNT
	)
	//tag::sql-sp-custom-crud-example[]
	@SQLDelete(
			sql = "{ call sp_delete_person(?) } ",
			callable = true
	)
	//end::sql-sp-custom-crud-example[]
	@SQLSelect(sql = "SELECT id, name  FROM person WHERE id = ? and valid = 1")
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
