/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

public class HbmIdBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx.getModelsContext());
	}

	@Test
	public void testSimpleIdWithIdentityGenerator() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");
		id.setTypeAttribute("long");
		id.setColumnAttribute("EMP_ID");

		JaxbHbmGeneratorSpecificationType generator = new JaxbHbmGeneratorSpecificationType();
		generator.setClazz("identity");
		id.setGenerator(generator);

		HbmIdBuilder.processId(entityClass, id, ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("id", field.getName());

		assertNotNull(field.getAnnotationUsage(Id.class, ctx.getModelsContext()));

		GeneratedValue gen = field.getAnnotationUsage(GeneratedValue.class, ctx.getModelsContext());
		assertNotNull(gen);
		assertEquals(GenerationType.IDENTITY, gen.strategy());

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("EMP_ID", col.name());
	}

	@Test
	public void testSimpleIdWithSequenceGenerator() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");
		id.setTypeAttribute("long");

		JaxbHbmGeneratorSpecificationType generator = new JaxbHbmGeneratorSpecificationType();
		generator.setClazz("sequence");
		id.setGenerator(generator);

		HbmIdBuilder.processId(entityClass, id, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		GeneratedValue gen = field.getAnnotationUsage(GeneratedValue.class, ctx.getModelsContext());
		assertNotNull(gen);
		assertEquals(GenerationType.SEQUENCE, gen.strategy());
	}

	@Test
	public void testSimpleIdWithAssignedGenerator() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");
		id.setTypeAttribute("string");

		JaxbHbmGeneratorSpecificationType generator = new JaxbHbmGeneratorSpecificationType();
		generator.setClazz("assigned");
		id.setGenerator(generator);

		HbmIdBuilder.processId(entityClass, id, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(Id.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(GeneratedValue.class, ctx.getModelsContext()),
				"assigned generator should not produce @GeneratedValue");
	}

	@Test
	public void testSimpleIdNoGenerator() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");
		id.setTypeAttribute("long");
		id.setColumnAttribute("ID");

		HbmIdBuilder.processId(entityClass, id, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(Id.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(GeneratedValue.class, ctx.getModelsContext()));
	}

	@Test
	public void testSimpleIdDefaultType() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");
		// No type set — should default to "long" (primitive)

		HbmIdBuilder.processId(entityClass, id, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("long", field.getType().getName());
	}

	@Test
	public void testSimpleIdColumnDefaultsToName() {
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		id.setName("id");

		HbmIdBuilder.processId(entityClass, id, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("id", col.name());
	}

	@Test
	public void testProcessIdNull() {
		HbmIdBuilder.processId(entityClass, null, ctx);
		assertTrue(entityClass.getFields().isEmpty());
	}

	@Test
	public void testCompositeIdKeyProperties() {
		JaxbHbmCompositeIdType compositeId = new JaxbHbmCompositeIdType();

		JaxbHbmCompositeKeyBasicAttributeType keyProp1 = new JaxbHbmCompositeKeyBasicAttributeType();
		keyProp1.setName("departmentId");
		keyProp1.setTypeAttribute("long");
		keyProp1.setColumnAttribute("DEPT_ID");

		JaxbHbmCompositeKeyBasicAttributeType keyProp2 = new JaxbHbmCompositeKeyBasicAttributeType();
		keyProp2.setName("employeeId");
		keyProp2.setTypeAttribute("long");
		keyProp2.setColumnAttribute("EMP_ID");

		compositeId.getKeyPropertyOrKeyManyToOne().add(keyProp1);
		compositeId.getKeyPropertyOrKeyManyToOne().add(keyProp2);

		HbmIdBuilder.processCompositeId(entityClass, compositeId, "com.example", ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(2, fields.size());

		for (FieldDetails field : fields) {
			assertNotNull(field.getAnnotationUsage(Id.class, ctx.getModelsContext()),
					field.getName() + " should have @Id");
			assertNotNull(field.getAnnotationUsage(Column.class, ctx.getModelsContext()),
					field.getName() + " should have @Column");
		}
	}

	@Test
	public void testCompositeIdKeyManyToOne() {
		JaxbHbmCompositeIdType compositeId = new JaxbHbmCompositeIdType();

		JaxbHbmCompositeKeyManyToOneType keyM2o = new JaxbHbmCompositeKeyManyToOneType();
		keyM2o.setName("department");
		keyM2o.setClazz("Department");

		compositeId.getKeyPropertyOrKeyManyToOne().add(keyM2o);

		HbmIdBuilder.processCompositeId(entityClass, compositeId, "com.example", ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("department", field.getName());

		assertNotNull(field.getAnnotationUsage(Id.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(ManyToOne.class, ctx.getModelsContext()));
	}

	@Test
	public void testCompositeIdNull() {
		HbmIdBuilder.processCompositeId(entityClass, null, "com.example", ctx);
		assertTrue(entityClass.getFields().isEmpty());
	}
}
