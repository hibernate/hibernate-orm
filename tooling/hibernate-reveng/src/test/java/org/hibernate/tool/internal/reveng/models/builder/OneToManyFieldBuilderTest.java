/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

/**
 * Tests for {@link OneToManyFieldBuilder}, verifying that {@code @OneToMany}
 * annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class OneToManyFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails elementClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"Department",
			"com.example.Department",
			false,
			null,
			null,
			modelsContext
		);
		elementClassDetails = new DynamicClassDetails(
			"Employee",
			"com.example.Employee",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testOneToManyAnnotation() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example");

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("employees", field.getName());

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m, "Should have @OneToMany");
		assertEquals("department", o2m.mappedBy());
	}

	@Test
	public void testFieldType() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example");

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertTrue(field.getType().getName().contains("Set"),
			"Field type should be a Set");
	}

	@Test
	public void testFetchType() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER);

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m);
		assertEquals(FetchType.EAGER, o2m.fetch());
	}

	@Test
	public void testCascadeTypes() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example")
				.cascade(CascadeType.PERSIST, CascadeType.REMOVE);

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m);
		CascadeType[] cascades = o2m.cascade();
		assertEquals(2, cascades.length);
		assertEquals(CascadeType.PERSIST, cascades[0]);
		assertEquals(CascadeType.REMOVE, cascades[1]);
	}

	@Test
	public void testOrphanRemoval() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example")
				.orphanRemoval(true);

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m);
		assertTrue(o2m.orphanRemoval());
	}

	@Test
	public void testDefaultOrphanRemovalIsFalse() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example");

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m);
		assertFalse(o2m.orphanRemoval());
	}

	@Test
	public void testFullyConfigured() {
		OneToManyMetadata o2mMetadata =
			new OneToManyMetadata("employees", "department", "Employee", "com.example")
				.fetchType(FetchType.LAZY)
				.cascade(CascadeType.ALL)
				.orphanRemoval(true);

		OneToManyFieldBuilder.buildOneToManyField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToMany o2m = field.getAnnotationUsage(OneToMany.class, modelsContext);
		assertNotNull(o2m);
		assertEquals("department", o2m.mappedBy());
		assertEquals(FetchType.LAZY, o2m.fetch());
		assertEquals(1, o2m.cascade().length);
		assertEquals(CascadeType.ALL, o2m.cascade()[0]);
		assertTrue(o2m.orphanRemoval());
	}
}
