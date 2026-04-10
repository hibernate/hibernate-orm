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

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

public class HbmAssociationBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx.getModelsContext());
	}

	// --- ManyToOne tests ---

	@Test
	public void testManyToOne() {
		JaxbHbmManyToOneType m2o = new JaxbHbmManyToOneType();
		m2o.setName("department");
		m2o.setClazz("Department");
		m2o.setColumnAttribute("DEPT_ID");

		HbmAssociationBuilder.processManyToOne(entityClass, m2o, "com.example", ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("department", field.getName());
		assertEquals("Department", field.getType().getName());

		assertNotNull(field.getAnnotationUsage(ManyToOne.class, ctx.getModelsContext()));

		JoinColumn joinCol = field.getAnnotationUsage(JoinColumn.class, ctx.getModelsContext());
		assertNotNull(joinCol);
		assertEquals("DEPT_ID", joinCol.name());
	}

	@Test
	public void testManyToOneNoColumn() {
		JaxbHbmManyToOneType m2o = new JaxbHbmManyToOneType();
		m2o.setName("department");
		m2o.setClazz("Department");
		// No column

		HbmAssociationBuilder.processManyToOne(entityClass, m2o, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ManyToOne.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(JoinColumn.class, ctx.getModelsContext()));
	}

	@Test
	public void testManyToOneFullyQualifiedClass() {
		JaxbHbmManyToOneType m2o = new JaxbHbmManyToOneType();
		m2o.setName("department");
		m2o.setClazz("com.other.Department");
		m2o.setColumnAttribute("DEPT_ID");

		HbmAssociationBuilder.processManyToOne(entityClass, m2o, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Department", field.getType().getName());
	}

	// --- OneToOne tests ---

	@Test
	public void testOneToOne() {
		JaxbHbmOneToOneType o2o = new JaxbHbmOneToOneType();
		o2o.setName("address");
		o2o.setClazz("Address");

		HbmAssociationBuilder.processOneToOne(entityClass, o2o, "com.example", ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("address", field.getName());
		assertEquals("Address", field.getType().getName());

		assertNotNull(field.getAnnotationUsage(OneToOne.class, ctx.getModelsContext()));
	}

	@Test
	public void testOneToOneFullyQualifiedClass() {
		JaxbHbmOneToOneType o2o = new JaxbHbmOneToOneType();
		o2o.setName("address");
		o2o.setClazz("com.other.Address");

		HbmAssociationBuilder.processOneToOne(entityClass, o2o, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Address", field.getType().getName());
	}

	// --- Any tests ---

	@Test
	public void testAnyBasic() {
		JaxbHbmAnyAssociationType any = new JaxbHbmAnyAssociationType();
		any.setName("payment");
		any.setMetaType("string");
		any.setIdType("long");

		// Discriminator column + FK column
		JaxbHbmColumnType discCol = new JaxbHbmColumnType();
		discCol.setName("PAYMENT_TYPE");
		JaxbHbmColumnType fkCol = new JaxbHbmColumnType();
		fkCol.setName("PAYMENT_ID");
		any.getColumn().add(discCol);
		any.getColumn().add(fkCol);

		HbmAssociationBuilder.processAny(entityClass, any, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("payment", field.getName());

		assertNotNull(field.getAnnotationUsage(Any.class, ctx.getModelsContext()));

		AnyDiscriminator discAnn = field.getAnnotationUsage(
				AnyDiscriminator.class, ctx.getModelsContext());
		assertNotNull(discAnn);
		assertEquals(DiscriminatorType.STRING, discAnn.value());

		AnyKeyJavaClass keyAnn = field.getAnnotationUsage(
				AnyKeyJavaClass.class, ctx.getModelsContext());
		assertNotNull(keyAnn);
		assertEquals(long.class, keyAnn.value());

		JoinColumn joinCol = field.getAnnotationUsage(
				JoinColumn.class, ctx.getModelsContext());
		assertNotNull(joinCol);
		assertEquals("PAYMENT_ID", joinCol.name());
	}

	@Test
	public void testAnyWithMetaValues() {
		JaxbHbmAnyAssociationType any = new JaxbHbmAnyAssociationType();
		any.setName("payment");
		any.setMetaType("string");
		any.setIdType("long");

		JaxbHbmAnyValueMappingType mv1 = new JaxbHbmAnyValueMappingType();
		mv1.setValue("CC");
		mv1.setClazz("com.example.CreditCard");
		JaxbHbmAnyValueMappingType mv2 = new JaxbHbmAnyValueMappingType();
		mv2.setValue("BA");
		mv2.setClazz("com.example.BankAccount");
		any.getMetaValue().add(mv1);
		any.getMetaValue().add(mv2);

		JaxbHbmColumnType discCol = new JaxbHbmColumnType();
		discCol.setName("PAYMENT_TYPE");
		JaxbHbmColumnType fkCol = new JaxbHbmColumnType();
		fkCol.setName("PAYMENT_ID");
		any.getColumn().add(discCol);
		any.getColumn().add(fkCol);

		HbmAssociationBuilder.processAny(entityClass, any, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		AnyDiscriminatorValues valuesAnn = field.getAnnotationUsage(
				AnyDiscriminatorValues.class, ctx.getModelsContext());
		assertNotNull(valuesAnn);
		assertEquals(2, valuesAnn.value().length);
		assertEquals("CC", valuesAnn.value()[0].discriminator());
		assertEquals("BA", valuesAnn.value()[1].discriminator());
	}

	@Test
	public void testAnyIntegerDiscriminator() {
		JaxbHbmAnyAssociationType any = new JaxbHbmAnyAssociationType();
		any.setName("payment");
		any.setMetaType("integer");
		any.setIdType("long");

		HbmAssociationBuilder.processAny(entityClass, any, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		AnyDiscriminator discAnn = field.getAnnotationUsage(
				AnyDiscriminator.class, ctx.getModelsContext());
		assertNotNull(discAnn);
		assertEquals(DiscriminatorType.INTEGER, discAnn.value());
	}
}
