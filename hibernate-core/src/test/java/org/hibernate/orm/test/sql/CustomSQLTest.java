/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@Jpa(
		annotatedClasses = {
				CustomSQLTest.Person.class
		},
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = CustomSQLTest.ListSemanticsProvider.class
		)
)
public class CustomSQLTest {

	public static class ListSemanticsProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return CollectionClassification.BAG.name();
		}
	}

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "ALTER TABLE person ADD COLUMN valid boolean" );
					statement.executeUpdate( "ALTER TABLE Person_phones ADD COLUMN valid boolean" );
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
			person.getPhones().add( "123-456-7890" );
			person.getPhones().add( "123-456-0987" );
			return person;
		} );

		scope.inTransaction( entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertThat( person.getPhones() ).hasSize( 2 );
			person.getPhones().remove( 0 );
			person.setName( "Mr. John Doe" );
		} );

		scope.inTransaction( entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertThat( person.getPhones() ).hasSize( 1 );
			entityManager.remove( person );
		} );

		scope.inTransaction( entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find( Person.class, postId );
			assertThat( person ).isNull();
		} );
	}

	//tag::sql-custom-crud-example[]
	@Entity(name = "Person")
	@SQLInsert(
			sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, true) ",
			verify = Expectation.RowCount.class
	)
	@SQLUpdate(
			sql = "UPDATE person SET name = ? where id = ? "
	)
	@SQLDelete(
			sql = "UPDATE person SET valid = false WHERE id = ? "
	)
	@SQLSelect(
			sql = "SELECT id, name FROM person WHERE id = ? and valid = true"
	)
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
		@SQLRestriction("valid = true")
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
