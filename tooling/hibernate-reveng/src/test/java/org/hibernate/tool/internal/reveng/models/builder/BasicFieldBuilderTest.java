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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

/**
 * Tests for {@link BasicFieldBuilder}, verifying that each JPA annotation
 * is correctly applied to fields based on {@link ColumnMetadata} configuration.
 *
 * @author Koen Aers
 */
public class BasicFieldBuilderTest {

	private ModelsContext modelsContext;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		modelsContext = new BasicModelsContextImpl(classLoading, false, null);
		entityClass = new DynamicClassDetails(
			"TestEntity",
			"com.example.TestEntity",
			false,
			null,
			null,
			modelsContext
		);
	}

	@Test
	public void testSimpleField() {
		ColumnMetadata column = new ColumnMetadata("USERNAME", "username", String.class)
			.nullable(true);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size(), "Should have one field");
		FieldDetails field = fields.get(0);
		assertEquals("username", field.getName());

		// Should have @Column
		Column col = field.getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(col, "Should have @Column");
		assertEquals("USERNAME", col.name());
		assertTrue(col.nullable());

		// Should NOT have @Id, @GeneratedValue, @Version, @Basic, @Temporal, @Lob
		assertNull(field.getAnnotationUsage(Id.class, modelsContext));
		assertNull(field.getAnnotationUsage(GeneratedValue.class, modelsContext));
		assertNull(field.getAnnotationUsage(Version.class, modelsContext));
		assertNull(field.getAnnotationUsage(Basic.class, modelsContext));
		assertNull(field.getAnnotationUsage(Temporal.class, modelsContext));
		assertNull(field.getAnnotationUsage(Lob.class, modelsContext));
	}

	@Test
	public void testPrimaryKeyField() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.nullable(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Id id = field.getAnnotationUsage(Id.class, modelsContext);
		assertNotNull(id, "Should have @Id");

		assertNull(field.getAnnotationUsage(GeneratedValue.class, modelsContext),
			"Should NOT have @GeneratedValue when autoIncrement is not set");
	}

	@Test
	public void testPrimaryKeyWithAutoIncrement() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.autoIncrement(true)
			.nullable(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Id.class, modelsContext), "Should have @Id");

		GeneratedValue gen = field.getAnnotationUsage(GeneratedValue.class, modelsContext);
		assertNotNull(gen, "Should have @GeneratedValue");
		assertEquals(GenerationType.IDENTITY, gen.strategy());
	}

	@Test
	public void testAutoIncrementWithoutPrimaryKeyIsIgnored() {
		ColumnMetadata column = new ColumnMetadata("SEQ", "seq", Long.class)
			.autoIncrement(true);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNull(field.getAnnotationUsage(Id.class, modelsContext),
			"Should NOT have @Id when not primary key");
		assertNull(field.getAnnotationUsage(GeneratedValue.class, modelsContext),
			"Should NOT have @GeneratedValue when not primary key");
	}

	@Test
	public void testVersionField() {
		ColumnMetadata column = new ColumnMetadata("VERSION", "version", Long.class)
			.version(true);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Version.class, modelsContext),
			"Should have @Version");
	}

	@Test
	public void testBasicWithFetchType() {
		ColumnMetadata column = new ColumnMetadata("DESCRIPTION", "description", String.class)
			.basicFetch(FetchType.LAZY);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Basic basic = field.getAnnotationUsage(Basic.class, modelsContext);
		assertNotNull(basic, "Should have @Basic");
		assertEquals(FetchType.LAZY, basic.fetch());
	}

	@Test
	public void testBasicWithOptional() {
		ColumnMetadata column = new ColumnMetadata("STATUS", "status", String.class)
			.basicOptional(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Basic basic = field.getAnnotationUsage(Basic.class, modelsContext);
		assertNotNull(basic, "Should have @Basic");
		assertFalse(basic.optional());
	}

	@Test
	public void testBasicWithFetchTypeAndOptional() {
		ColumnMetadata column = new ColumnMetadata("NOTES", "notes", String.class)
			.basicFetch(FetchType.EAGER)
			.basicOptional(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Basic basic = field.getAnnotationUsage(Basic.class, modelsContext);
		assertNotNull(basic, "Should have @Basic");
		assertEquals(FetchType.EAGER, basic.fetch());
		assertFalse(basic.optional());
	}

	@Test
	public void testTemporalDate() {
		ColumnMetadata column = new ColumnMetadata("EVENT_DATE", "eventDate", Date.class)
			.temporal(TemporalType.DATE);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Temporal temporal = field.getAnnotationUsage(Temporal.class, modelsContext);
		assertNotNull(temporal, "Should have @Temporal");
		assertEquals(TemporalType.DATE, temporal.value());
	}

	@Test
	public void testTemporalTimestamp() {
		ColumnMetadata column = new ColumnMetadata("CREATED_AT", "createdAt", Date.class)
			.temporal(TemporalType.TIMESTAMP);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Temporal temporal = field.getAnnotationUsage(Temporal.class, modelsContext);
		assertNotNull(temporal, "Should have @Temporal");
		assertEquals(TemporalType.TIMESTAMP, temporal.value());
	}

	@Test
	public void testLobField() {
		ColumnMetadata column = new ColumnMetadata("CONTENT", "content", String.class)
			.lob(true);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Lob.class, modelsContext),
			"Should have @Lob");
	}

	@Test
	public void testColumnWithLength() {
		ColumnMetadata column = new ColumnMetadata("NAME", "name", String.class)
			.length(255);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Column col = field.getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(col, "Should have @Column");
		assertEquals("NAME", col.name());
		assertEquals(255, col.length());
	}

	@Test
	public void testColumnWithPrecisionAndScale() {
		ColumnMetadata column = new ColumnMetadata("AMOUNT", "amount", BigDecimal.class)
			.precision(10)
			.scale(2);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Column col = field.getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(col, "Should have @Column");
		assertEquals(10, col.precision());
		assertEquals(2, col.scale());
	}

	@Test
	public void testColumnNotNullable() {
		ColumnMetadata column = new ColumnMetadata("EMAIL", "email", String.class)
			.nullable(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Column col = field.getAnnotationUsage(Column.class, modelsContext);
		assertNotNull(col, "Should have @Column");
		assertFalse(col.nullable());
	}

	@Test
	public void testFullyAnnotatedPrimaryKey() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.autoIncrement(true)
			.nullable(false);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Id.class, modelsContext));
		GeneratedValue gen = field.getAnnotationUsage(GeneratedValue.class, modelsContext);
		assertNotNull(gen);
		assertEquals(GenerationType.IDENTITY, gen.strategy());

		Column col = field.getAnnotationUsage(Column.class, modelsContext);
		assertEquals("ID", col.name());
		assertFalse(col.nullable());
	}

	@Test
	public void testLobWithLazyFetch() {
		ColumnMetadata column = new ColumnMetadata("DATA", "data", byte[].class)
			.lob(true)
			.basicFetch(FetchType.LAZY);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		assertNotNull(field.getAnnotationUsage(Lob.class, modelsContext), "Should have @Lob");

		Basic basic = field.getAnnotationUsage(Basic.class, modelsContext);
		assertNotNull(basic, "Should have @Basic");
		assertEquals(FetchType.LAZY, basic.fetch());
	}

	@Test
	public void testTemporalWithBasicFetch() {
		ColumnMetadata column = new ColumnMetadata("CREATED_AT", "createdAt", Date.class)
			.temporal(TemporalType.TIMESTAMP)
			.basicFetch(FetchType.EAGER);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);

		Temporal temporal = field.getAnnotationUsage(Temporal.class, modelsContext);
		assertNotNull(temporal, "Should have @Temporal");
		assertEquals(TemporalType.TIMESTAMP, temporal.value());

		Basic basic = field.getAnnotationUsage(Basic.class, modelsContext);
		assertNotNull(basic, "Should have @Basic");
		assertEquals(FetchType.EAGER, basic.fetch());
	}

	@Test
	public void testMultipleFields() {
		ColumnMetadata idColumn = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.autoIncrement(true)
			.nullable(false);
		ColumnMetadata nameColumn = new ColumnMetadata("NAME", "name", String.class)
			.length(100);
		ColumnMetadata versionColumn = new ColumnMetadata("VERSION", "version", Long.class)
			.version(true);

		BasicFieldBuilder.addBasicField(entityClass, idColumn, modelsContext);
		BasicFieldBuilder.addBasicField(entityClass, nameColumn, modelsContext);
		BasicFieldBuilder.addBasicField(entityClass, versionColumn, modelsContext);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(3, fields.size(), "Should have three fields");

		FieldDetails idField = findField(fields, "id");
		assertNotNull(idField);
		assertNotNull(idField.getAnnotationUsage(Id.class, modelsContext));
		assertNotNull(idField.getAnnotationUsage(GeneratedValue.class, modelsContext));

		FieldDetails nameField = findField(fields, "name");
		assertNotNull(nameField);
		assertEquals(100, nameField.getAnnotationUsage(Column.class, modelsContext).length());

		FieldDetails versionField = findField(fields, "version");
		assertNotNull(versionField);
		assertNotNull(versionField.getAnnotationUsage(Version.class, modelsContext));
	}

	@Test
	public void testFieldType() {
		ColumnMetadata column = new ColumnMetadata("AMOUNT", "amount", BigDecimal.class);

		BasicFieldBuilder.addBasicField(entityClass, column, modelsContext);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("amount", field.getName());
		assertEquals(BigDecimal.class.getName(), field.getType().getName());
	}

	private FieldDetails findField(List<FieldDetails> fields, String name) {
		return fields.stream()
			.filter(f -> f.getName().equals(name))
			.findFirst()
			.orElse(null);
	}
}
