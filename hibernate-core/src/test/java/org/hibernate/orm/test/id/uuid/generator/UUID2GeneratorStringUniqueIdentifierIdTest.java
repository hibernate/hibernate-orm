/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.generator;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( SQLServerDialect.class )
@JiraKey( value = "HHH-12943" )
public class UUID2GeneratorStringUniqueIdentifierIdTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { FooEntity.class };
	}

	@Test
	public void testPaginationQuery() {
		String id = doInJPA( this::entityManagerFactory, entityManager -> {
			FooEntity entity = new FooEntity();
			entity.getFooValues().add("one");
			entity.getFooValues().add("two");
			entity.getFooValues().add("three");

			entityManager.persist(entity);

			return entity.getId();
		} );

		assertNotNull(id);

		doInJPA( this::entityManagerFactory, entityManager -> {
			FooEntity entity = entityManager.find(FooEntity.class, id.toUpperCase());
			assertNotNull(entity);

			assertTrue(entity.getFooValues().size() == 3);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			FooEntity entity = entityManager.find(FooEntity.class, id);
			assertNotNull(entity);

			assertTrue(entity.getFooValues().size() == 3);
		} );
	}

	@Entity
	@Table(name = "foo")
	public static class FooEntity {

		@Id
		@GenericGenerator(name = "uuid", strategy = "uuid2")
		@GeneratedValue(generator = "uuid")
		@Column(columnDefinition = "UNIQUEIDENTIFIER")
		private String id;

		@ElementCollection
		@JoinTable(name = "foo_values")
		@Column(name = "foo_value")
		private final Set<String> fooValues = new HashSet<>();

		public String getId() {
			return id.toUpperCase();
		}

		public void setId(String id) {
			this.id = id;
		}

		public Set<String> getFooValues() {
			return fooValues;
		}

	}
}
