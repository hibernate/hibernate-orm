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
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

/**
 * Tests for {@link ManyToManyFieldBuilder}, verifying that {@code @ManyToMany}
 * and {@code @JoinTable} annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class ManyToManyFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails targetClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"Student",
			"com.example.Student",
			false,
			null,
			null,
			modelsContext
		);
		targetClassDetails = new DynamicClassDetails(
			"Course",
			"com.example.Course",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testManyToManyAnnotation() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("courses", field.getName());

		ManyToMany m2m = field.getAnnotationUsage(ManyToMany.class, modelsContext);
		assertNotNull(m2m, "Should have @ManyToMany");
	}

	@Test
	public void testFieldType() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertTrue(field.getType().getName().contains("Set"),
			"Field type should be a Set");
	}

	@Test
	public void testMappedBy() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example")
				.mappedBy("students");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToMany m2m = field.getAnnotationUsage(ManyToMany.class, modelsContext);
		assertNotNull(m2m);
		assertEquals("students", m2m.mappedBy());
	}

	@Test
	public void testFetchType() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example")
				.fetchType(FetchType.LAZY);

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToMany m2m = field.getAnnotationUsage(ManyToMany.class, modelsContext);
		assertNotNull(m2m);
		assertEquals(FetchType.LAZY, m2m.fetch());
	}

	@Test
	public void testCascadeTypes() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE);

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToMany m2m = field.getAnnotationUsage(ManyToMany.class, modelsContext);
		assertNotNull(m2m);
		CascadeType[] cascades = m2m.cascade();
		assertEquals(2, cascades.length);
		assertEquals(CascadeType.PERSIST, cascades[0]);
		assertEquals(CascadeType.MERGE, cascades[1]);
	}

	@Test
	public void testJoinTable() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example")
				.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinTable joinTable = field.getAnnotationUsage(JoinTable.class, modelsContext);
		assertNotNull(joinTable, "Should have @JoinTable");
		assertEquals("STUDENT_COURSE", joinTable.name());
		assertEquals(1, joinTable.joinColumns().length);
		assertEquals("STUDENT_ID", joinTable.joinColumns()[0].name());
		assertEquals(1, joinTable.inverseJoinColumns().length);
		assertEquals("COURSE_ID", joinTable.inverseJoinColumns()[0].name());
	}

	@Test
	public void testMappedBySideHasNoJoinTable() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("students", "Student", "com.example")
				.mappedBy("courses");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(ManyToMany.class, modelsContext),
			"Should have @ManyToMany");
		assertNull(field.getAnnotationUsage(JoinTable.class, modelsContext),
			"Should NOT have @JoinTable on inverse side");
	}

	@Test
	public void testNoJoinTableWhenNotConfigured() {
		ManyToManyMetadata m2mMetadata =
			new ManyToManyMetadata("courses", "Course", "com.example");

		ManyToManyFieldBuilder.buildManyToManyField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNull(field.getAnnotationUsage(JoinTable.class, modelsContext),
			"Should NOT have @JoinTable when not configured");
	}
}
