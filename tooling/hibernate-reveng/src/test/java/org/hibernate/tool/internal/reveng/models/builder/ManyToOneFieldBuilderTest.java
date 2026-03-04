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
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Tests for {@link ManyToOneFieldBuilder}, verifying that {@code @ManyToOne}
 * and {@code @JoinColumn} annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class ManyToOneFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails targetClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"Employee",
			"com.example.Employee",
			false,
			null,
			null,
			modelsContext
		);
		targetClassDetails = new DynamicClassDetails(
			"Department",
			"com.example.Department",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testManyToOneAnnotation() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("department", field.getName());

		ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, modelsContext);
		assertNotNull(m2o, "Should have @ManyToOne");
	}

	@Test
	public void testFieldType() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Department", field.getType().getName());
	}

	@Test
	public void testJoinColumn() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn, "Should have @JoinColumn");
		assertEquals("DEPARTMENT_ID", joinColumn.name());
	}

	@Test
	public void testFetchType() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY);

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, modelsContext);
		assertNotNull(m2o);
		assertEquals(FetchType.LAZY, m2o.fetch());
	}

	@Test
	public void testOptionalTrue() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example")
				.optional(true);

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, modelsContext);
		assertNotNull(m2o);
		assertTrue(m2o.optional());

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertTrue(joinColumn.nullable());
	}

	@Test
	public void testOptionalFalse() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example")
				.optional(false);

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, modelsContext);
		assertNotNull(m2o);
		assertFalse(m2o.optional());

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertFalse(joinColumn.nullable());
	}

	@Test
	public void testReferencedColumnName() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertEquals("DEPT_CODE", joinColumn.name());
		assertEquals("CODE", joinColumn.referencedColumnName());
	}

	@Test
	public void testNoReferencedColumnName() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertEquals("DEPARTMENT_ID", joinColumn.name());
		assertEquals("", joinColumn.referencedColumnName(),
			"referencedColumnName should be empty when not set");
	}

	@Test
	public void testDefaultOptionalIsTrue() {
		ForeignKeyMetadata fkMetadata =
			new ForeignKeyMetadata("department", "DEPARTMENT_ID", "Department", "com.example");

		ManyToOneFieldBuilder.buildManyToOneField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, modelsContext);
		assertNotNull(m2o);
		assertTrue(m2o.optional(), "Default optional should be true");
	}
}
