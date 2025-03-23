/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.junit.Before;
import org.junit.Test;

import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hibernate.annotations.ResultCheckStyle.COUNT;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * This test is for replicating the HHH-10557 issue.
 *
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
public class SQLSelectTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG.name() );
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class);
			DdlTypeRegistry ddlTypeRegistry = session.getTypeConfiguration().getDdlTypeRegistry();
			session.doWork(connection -> {
				try(Statement statement = connection.createStatement();) {
					statement.executeUpdate(String.format( "ALTER TABLE person %s valid %s",
														getDialect().getAddColumnString(),
							ddlTypeRegistry.getTypeName( Types.BOOLEAN, getDialect())));
					statement.executeUpdate(String.format( "ALTER TABLE Person_phones %s valid %s",
														getDialect().getAddColumnString(),
							ddlTypeRegistry.getTypeName( Types.BOOLEAN, getDialect())));
				}
			});
		});
	}

	@Test @JiraKey(value = "HHH-10557")
	public void test_HHH10557() {

		Person _person = doInJPA(this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setName("John Doe");
			entityManager.persist(person);
			person.getPhones().add("123-456-7890");
			person.getPhones().add("123-456-0987");
			return person;
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find(Person.class, postId);
			assertEquals(2, person.getPhones().size());
			person.getPhones().remove(0);
			person.setName("Mr. John Doe");
		});


		doInJPA(this::entityManagerFactory, entityManager -> {
			Long postId = _person.getId();
			Person person = entityManager.find(Person.class, postId);
			assertEquals(1, person.getPhones().size());
		});
	}


	//tag::sql-custom-crud-example[]
	@Entity(name = "Person")
	@SQLInsert(sql = "INSERT INTO person (name, id, valid) VALUES (?, ?, true) ", check = COUNT)
	@SQLUpdate(sql = "UPDATE person SET name = ? where id = ? ")
	@SQLDelete(sql = "UPDATE person SET valid = false WHERE id = ? ")
	@SQLSelect(sql = "SELECT id, name FROM person WHERE id = ? and valid = true")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ElementCollection
		@SQLInsert(sql = "INSERT INTO person_phones (person_id, phones, valid) VALUES (?, ?, true) ")
		@SQLDeleteAll(sql = "UPDATE person_phones SET valid = false WHERE person_id = ?")
		@SQLSelect(sql = "SELECT phones FROM Person_phones WHERE person_id = ? and valid = true ")
		private List<String> phones = new ArrayList<>();

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
	}
	//end::sql-custom-crud-example[]

}
