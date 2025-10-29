/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author El Mehdi KZADRI
 */
@JiraKey("HHH-19850")
public class AbstractEntityPersisterTest extends BaseCoreFunctionalTestCase {

	private AbstractEntityPersister persister;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Before
	@SuppressWarnings("resource")
	public void setUp() {
		persister = (AbstractEntityPersister) sessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(TestEntity.class.getName());
	}

	@Test
	public void testResolveDirtyAttributeIndexes_EmptyAttributeNames() {
		inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

			Object[] currentState = new Object[5];
			Object[] previousState = new Object[5];
			String[] attributeNames = new String[0];

			int[] result = persister.resolveDirtyAttributeIndexes(
					currentState, previousState, attributeNames, session
			);

			assertThat(result).isNotNull();
			assertThat(result.length).isEqualTo(0);
		});
	}

	@Test
	public void testResolveDirtyAttributeIndexes_WithAttributes() {
		inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

			Object[] currentState = new Object[] { 1L, "name1", "value1" };
			Object[] previousState = new Object[] { 1L, "name2", "value2" };
			String[] attributeNames = {"name", "value"};

			int[] result = persister.resolveDirtyAttributeIndexes(
					currentState, previousState, attributeNames, session
			);

			assertThat(result).isNotNull();
			// Verify no duplicates
			assertThat(result.length).isEqualTo(Arrays.stream(result).distinct().count());
			// Verify sorted order
			int[] sorted = result.clone();
			Arrays.sort(sorted);
			assertThat(sorted).isEqualTo(result);
		});
	}

	@Test
	public void testResolveDirtyAttributeIndexes_DuplicateAttributeNames() {
		inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

			Object[] currentState = new Object[] { 1L, "name1", "value1" };
			Object[] previousState = new Object[] { 1L, "name2", "value2" };
			String[] attributeNames = {"name", "name", "value", "value"};

			int[] result = persister.resolveDirtyAttributeIndexes(
					currentState, previousState, attributeNames, session
			);

			// Should not contain duplicate indices
			assertThat(result.length).isEqualTo(Arrays.stream(result).distinct().count());
		});
	}

	@Test
	public void testResolveDirtyAttributeIndexes_NoDirtyFields() {
		inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

			Object[] state = new Object[] { 1L, "name1", "value1" };
			String[] attributeNames = {"name"};

			int[] result = persister.resolveDirtyAttributeIndexes(
					state, state, attributeNames, session
			);

			assertThat(result).isNotNull();
		});
	}

	@Test
	public void testResolveDirtyAttributeIndexes_NonExistentAttribute() {
		inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap(SessionImplementor.class);

			Object[] currentState = new Object[] { 1L, "name1", "value1" };
			Object[] previousState = new Object[] { 1L, "name2", "value2" };
			String[] attributeNames = {"nonExistent"};

			int[] result = persister.resolveDirtyAttributeIndexes(
					currentState, previousState, attributeNames, session
			);

			assertThat(result).isNotNull();
		});
	}

	@Entity
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String value;

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

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
