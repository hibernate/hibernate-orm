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
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * Tests for {@link OneToOneFieldBuilder}, verifying that {@code @OneToOne}
 * and {@code @JoinColumn} annotations are correctly applied to fields.
 *
 * @author Koen Aers
 */
public class OneToOneFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;
	private ClassDetails targetClassDetails;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"User",
			"com.example.User",
			false,
			null,
			null,
			modelsContext
		);
		targetClassDetails = new DynamicClassDetails(
			"Profile",
			"com.example.Profile",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testOneToOneAnnotation() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("profile", field.getName());

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o, "Should have @OneToOne");
	}

	@Test
	public void testFieldType() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Profile", field.getType().getName());
	}

	@Test
	public void testMappedBy() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.mappedBy("user");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o);
		assertEquals("user", o2o.mappedBy());
	}

	@Test
	public void testFetchType() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.fetchType(FetchType.LAZY);

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o);
		assertEquals(FetchType.LAZY, o2o.fetch());
	}

	@Test
	public void testCascadeTypes() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.cascade(CascadeType.ALL);

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o);
		CascadeType[] cascades = o2o.cascade();
		assertEquals(1, cascades.length);
		assertEquals(CascadeType.ALL, cascades[0]);
	}

	@Test
	public void testOptional() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.optional(false);

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o);
		assertFalse(o2o.optional());
	}

	@Test
	public void testOrphanRemoval() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.orphanRemoval(true);

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		OneToOne o2o = field.getAnnotationUsage(OneToOne.class, modelsContext);
		assertNotNull(o2o);
		assertTrue(o2o.orphanRemoval());
	}

	@Test
	public void testJoinColumn() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.foreignKeyColumnName("PROFILE_ID");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn, "Should have @JoinColumn");
		assertEquals("PROFILE_ID", joinColumn.name());
		assertTrue(joinColumn.unique());
	}

	@Test
	public void testJoinColumnWithReferencedColumn() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.foreignKeyColumnName("PROFILE_ID")
				.referencedColumnName("ID");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertEquals("PROFILE_ID", joinColumn.name());
		assertEquals("ID", joinColumn.referencedColumnName());
	}

	@Test
	public void testMappedBySideHasNoJoinColumn() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("user", "User", "com.example")
				.mappedBy("profile");

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(OneToOne.class, modelsContext),
			"Should have @OneToOne");
		assertNull(field.getAnnotationUsage(JoinColumn.class, modelsContext),
			"Should NOT have @JoinColumn on inverse side");
	}

	@Test
	public void testJoinColumnNullableMatchesOptional() {
		OneToOneMetadata o2oMetadata =
			new OneToOneMetadata("profile", "Profile", "com.example")
				.foreignKeyColumnName("PROFILE_ID")
				.optional(false);

		OneToOneFieldBuilder.buildOneToOneField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		JoinColumn joinColumn = field.getAnnotationUsage(JoinColumn.class, modelsContext);
		assertNotNull(joinColumn);
		assertFalse(joinColumn.nullable(),
			"@JoinColumn nullable should match optional=false");
	}
}
