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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

public class HbmCollectionBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Department", "com.example.Department",
				false, null, null, ctx.getModelsContext());
	}

	// --- Set with OneToMany ---

	@Test
	public void testSetOneToMany() {
		JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		set.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		set.setOneToMany(o2m);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("employees", field.getName());
		assertTrue(field.isPlural());
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	// --- Set with ManyToMany ---

	@Test
	public void testSetManyToMany() {
		JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName("projects");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("EMP_ID");
		set.setKey(key);
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Project");
		set.setManyToMany(m2m);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
	}

	// --- Set with element (basic collection) ---

	@Test
	public void testSetElementCollection() {
		JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName("tags");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		set.setKey(key);
		JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute("string");
		set.setElement(element);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("tags", field.getName());
		assertTrue(field.isPlural());
		assertNotNull(field.getAnnotationUsage(ElementCollection.class, ctx.getModelsContext()));
	}

	// --- List ---

	@Test
	public void testListOneToMany() {
		JaxbHbmListType list = new JaxbHbmListType();
		list.setName("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		list.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		list.setOneToMany(o2m);

		HbmCollectionBuilder.processList(entityClass, list, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	// --- Bag ---

	@Test
	public void testBagOneToMany() {
		JaxbHbmBagCollectionType bag = new JaxbHbmBagCollectionType();
		bag.setName("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		bag.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		bag.setOneToMany(o2m);

		HbmCollectionBuilder.processBag(entityClass, bag, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	// --- Map ---

	@Test
	public void testMapOneToMany() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("employeesByName");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		map.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		map.setOneToMany(o2m);

		HbmCollectionBuilder.processMap(entityClass, map, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	@Test
	public void testMapManyToMany() {
		JaxbHbmMapType map = new JaxbHbmMapType();
		map.setName("projectsByCode");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		map.setKey(key);
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Project");
		map.setManyToMany(m2m);

		HbmCollectionBuilder.processMap(entityClass, map, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
	}

	// --- Array ---

	@Test
	public void testArrayOneToMany() {
		JaxbHbmArrayType array = new JaxbHbmArrayType();
		array.setName("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		array.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		array.setOneToMany(o2m);

		HbmCollectionBuilder.processArray(entityClass, array, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(OneToMany.class, ctx.getModelsContext()));
	}

	@Test
	public void testArrayManyToMany() {
		JaxbHbmArrayType array = new JaxbHbmArrayType();
		array.setName("projects");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("EMP_ID");
		array.setKey(key);
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Project");
		array.setManyToMany(m2m);

		HbmCollectionBuilder.processArray(entityClass, array, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
	}

	// --- PrimitiveArray ---

	@Test
	public void testPrimitiveArrayElementCollection() {
		JaxbHbmPrimitiveArrayType primArray = new JaxbHbmPrimitiveArrayType();
		primArray.setName("scores");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("EMP_ID");
		primArray.setKey(key);
		JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute("integer");
		primArray.setElement(element);

		HbmCollectionBuilder.processPrimitiveArray(entityClass, primArray, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("scores", field.getName());
		assertNotNull(field.getAnnotationUsage(ElementCollection.class, ctx.getModelsContext()));
	}

	// --- IdBag ---

	@Test
	public void testIdBagManyToMany() {
		JaxbHbmIdBagCollectionType idBag = new JaxbHbmIdBagCollectionType();
		idBag.setName("items");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("ORDER_ID");
		idBag.setKey(key);
		JaxbHbmManyToManyCollectionElementType m2m = new JaxbHbmManyToManyCollectionElementType();
		m2m.setClazz("Item");
		idBag.setManyToMany(m2m);

		HbmCollectionBuilder.processIdBag(entityClass, idBag, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ManyToMany.class, ctx.getModelsContext()));
	}

	@Test
	public void testIdBagElementCollection() {
		JaxbHbmIdBagCollectionType idBag = new JaxbHbmIdBagCollectionType();
		idBag.setName("tags");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("ITEM_ID");
		idBag.setKey(key);
		JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute("string");
		idBag.setElement(element);

		HbmCollectionBuilder.processIdBag(entityClass, idBag, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(ElementCollection.class, ctx.getModelsContext()));
	}
}
