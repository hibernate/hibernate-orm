/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapKeyBasicType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

class HbmCollectionFieldFactoryTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Department", "com.example.Department",
				false, null, null, ctx.getModelsContext());
	}

	private JaxbHbmKeyType key(String column) {
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute(column);
		return key;
	}

	// --- createCollectionField ---

	@Test
	void testCreateCollectionFieldOneToMany() {
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, "employees", o2m, null, null,
				key("DEPT_ID"), "java.util.Set", "com.example", ctx);
		assertNotNull(field);
		assertEquals("employees", field.getName());
		assertTrue(field.isPlural());
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(JoinColumn.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateCollectionFieldManyToMany() {
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Project");
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, "projects", null, m2m, null,
				key("DEPT_ID"), "java.util.Set", "com.example", ctx);
		assertNotNull(field);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(JoinTable.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateCollectionFieldElementCollection() {
		JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute("string");
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, "tags", null, null, element,
				key("DEPT_ID"), "java.util.Set", "com.example", ctx);
		assertNotNull(field);
		assertNotNull(field.getAnnotationUsage(ElementCollection.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateCollectionFieldReturnsNullWhenNoChild() {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, "empty", null, null, null,
				key("DEPT_ID"), "java.util.Set", "com.example", ctx);
		assertNull(field);
	}

	// --- createMapCollectionField ---

	@Test
	void testCreateMapCollectionFieldOneToMany() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("employeesByName");
		map.setKey(key("DEPT_ID"));
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");

		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, "employeesByName", o2m, null, null, null,
				key("DEPT_ID"), map, "com.example", ctx);
		assertNotNull(field);
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateMapCollectionFieldWithMapKeyColumn() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("employeesByCode");
		map.setKey(key("DEPT_ID"));
		JaxbHbmMapKeyBasicType mapKey = new JaxbHbmMapKeyBasicType();
		mapKey.setColumnAttribute("EMP_CODE");
		map.setMapKey(mapKey);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");

		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, "employeesByCode", o2m, null, null, null,
				key("DEPT_ID"), map, "com.example", ctx);
		assertNotNull(field);
		MapKeyColumn mkc = field.getAnnotationUsage(
				MapKeyColumn.class, ctx.getModelsContext());
		assertNotNull(mkc);
		assertEquals("EMP_CODE", mkc.name());
	}

	@Test
	void testCreateMapCollectionFieldManyToMany() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("projectsByCode");
		map.setKey(key("DEPT_ID"));
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Project");

		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, "projectsByCode", null, m2m, null, null,
				key("DEPT_ID"), map, "com.example", ctx);
		assertNotNull(field);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateMapCollectionFieldElementCollection() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("attributes");
		map.setKey(key("DEPT_ID"));
		JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute("string");

		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, "attributes", null, null, null, element,
				key("DEPT_ID"), map, "com.example", ctx);
		assertNotNull(field);
		assertNotNull(field.getAnnotationUsage(ElementCollection.class, ctx.getModelsContext()));
	}

	@Test
	void testCreateMapCollectionFieldReturnsNullWhenNoChild() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("empty");
		map.setKey(key("DEPT_ID"));

		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, "empty", null, null, null, null,
				key("DEPT_ID"), map, "com.example", ctx);
		assertNull(field);
	}

	// --- buildCompositeElementCollectionField ---

	@Test
	void testBuildCompositeElementCollectionFieldNullClass() {
		var compositeElement = new org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType();
		DynamicFieldDetails field = HbmCollectionFieldFactory.buildCompositeElementCollectionField(
				entityClass, "addresses", compositeElement,
				key("DEPT_ID"), "java.util.Set", "com.example", ctx);
		assertNull(field);
	}
}
