/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@Jpa(
		annotatedClasses = {
				CustomSQLSecondaryTableTest.Person.class
		}
)
public class CustomSQLSecondaryTableTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "ALTER TABLE person ADD COLUMN valid boolean" );
					statement.executeUpdate( "ALTER TABLE person_details ADD COLUMN valid boolean" );
				}
			} );
		} );
	}

	@Test
	public void test_sql_custom_crud(EntityManagerFactoryScope scope) {

		Person _person = scope.fromTransaction( entityManager -> {
			Person person = new Person();
			person.setName( "John Doe" );
			entityManager.persist( person );
			person.setImage( new byte[] {1, 2, 3} );
			return person;
		} );

		scope.inTransaction( entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertThat( person.getImage() ).isEqualTo( new byte[] {1, 2, 3} );
			entityManager.remove( person );
		} );

		scope.inTransaction( entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertThat( person ).isNull();
		} );
	}


	//tag::sql-custom-crud-secondary-table-example[]
	@Entity(name = "Person")
	@Table(name = "person")
	@SecondaryTable(name = "person_details",
			pkJoinColumns = @PrimaryKeyJoinColumn(name = "person_id"))
	@SQLInsert(
			sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, true) "
	)
	@SQLDelete(
			sql = "UPDATE person SET valid = false WHERE id = ? "
	)
	@SQLInsert(
			table = "person_details",
			sql = "INSERT INTO person_details (image, person_id, valid) VALUES (?, ?, true) ",
			check = ResultCheckStyle.COUNT
	)
	@SQLDelete(
			table = "person_details",
			sql = "UPDATE person_details SET valid = false WHERE person_id = ? "
	)

	@SQLSelect(
			sql = "SELECT " +
				"    p.id, " +
				"    p.name, " +
				"    pd.image  " +
				"FROM person p  " +
				"LEFT OUTER JOIN person_details pd ON p.id = pd.person_id  " +
				"WHERE p.id = ? AND p.valid = true AND pd.valid = true"
	)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Column(name = "image", table = "person_details")
		private byte[] image;

		//Getters and setters are omitted for brevity

		//end::sql-custom-crud-secondary-table-example[]

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

		public byte[] getImage() {
			return image;
		}

		public void setImage(byte[] image) {
			this.image = image;
		}
		//tag::sql-custom-crud-secondary-table-example[]
	}
	//end::sql-custom-crud-secondary-table-example[]

}
