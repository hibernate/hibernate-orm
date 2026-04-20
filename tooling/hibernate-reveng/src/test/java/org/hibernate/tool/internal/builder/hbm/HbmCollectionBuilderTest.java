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
package org.hibernate.tool.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.cache.spi.access.AccessType;

import jakarta.persistence.JoinTable;
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

	// --- Collection metadata tests ---

	private JaxbHbmSetType createSetWithOneToMany() {
		JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		set.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		set.setOneToMany(o2m);
		return set;
	}

	@Test
	public void testCascadeAll() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setCascade("all");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Cascade cascadeAnn = field.getAnnotationUsage(Cascade.class, ctx.getModelsContext());
		assertNotNull(cascadeAnn);
		assertEquals(1, cascadeAnn.value().length);
		assertEquals(CascadeType.ALL, cascadeAnn.value()[0]);
	}

	@Test
	public void testCascadeMultiple() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setCascade("persist, merge, refresh");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Cascade cascadeAnn = field.getAnnotationUsage(Cascade.class, ctx.getModelsContext());
		assertNotNull(cascadeAnn);
		assertEquals(3, cascadeAnn.value().length);
	}

	@Test
	public void testCascadeNone() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setCascade("none");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNull(field.getAnnotationUsage(Cascade.class, ctx.getModelsContext()));
	}

	@Test
	public void testFetchJoin() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setFetch(JaxbHbmFetchStyleWithSubselectEnum.JOIN);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Fetch fetchAnn = field.getAnnotationUsage(Fetch.class, ctx.getModelsContext());
		assertNotNull(fetchAnn);
		assertEquals(FetchMode.JOIN, fetchAnn.value());
	}

	@Test
	public void testFetchSubselect() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setFetch(JaxbHbmFetchStyleWithSubselectEnum.SUBSELECT);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Fetch fetchAnn = field.getAnnotationUsage(Fetch.class, ctx.getModelsContext());
		assertNotNull(fetchAnn);
		assertEquals(FetchMode.SUBSELECT, fetchAnn.value());
	}

	@Test
	public void testWhere() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setWhere("active = true");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		SQLRestriction srAnn = field.getAnnotationUsage(
				SQLRestriction.class, ctx.getModelsContext());
		assertNotNull(srAnn);
		assertEquals("active = true", srAnn.value());
	}

	@Test
	public void testBatchSize() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setBatchSize(10);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		BatchSize bsAnn = field.getAnnotationUsage(
				BatchSize.class, ctx.getModelsContext());
		assertNotNull(bsAnn);
		assertEquals(10, bsAnn.size());
	}

	@Test
	public void testCache() {
		JaxbHbmSetType set = createSetWithOneToMany();
		JaxbHbmCacheType cache = new JaxbHbmCacheType();
		cache.setUsage(AccessType.READ_ONLY);
		cache.setRegion("dept_employees");
		set.setCache(cache);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Cache cacheAnn = field.getAnnotationUsage(Cache.class, ctx.getModelsContext());
		assertNotNull(cacheAnn);
		assertEquals(CacheConcurrencyStrategy.READ_ONLY, cacheAnn.usage());
		assertEquals("dept_employees", cacheAnn.region());
	}

	@Test
	public void testFilter() {
		JaxbHbmSetType set = createSetWithOneToMany();
		JaxbHbmFilterType filter = new JaxbHbmFilterType();
		filter.setName("activeFilter");
		filter.setCondition("active = true");
		set.getFilter().add(filter);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Filter filterAnn = field.getAnnotationUsage(Filter.class, ctx.getModelsContext());
		assertNotNull(filterAnn);
		assertEquals("activeFilter", filterAnn.name());
		assertEquals("active = true", filterAnn.condition());
	}

	@Test
	public void testSqlInsertDelete() {
		JaxbHbmSetType set = createSetWithOneToMany();
		JaxbHbmCustomSqlDmlType sqlInsert = new JaxbHbmCustomSqlDmlType();
		sqlInsert.setValue("INSERT INTO dept_emp VALUES(?, ?)");
		sqlInsert.setCallable(false);
		set.setSqlInsert(sqlInsert);
		JaxbHbmCustomSqlDmlType sqlDelete = new JaxbHbmCustomSqlDmlType();
		sqlDelete.setValue("{call removeDeptEmp(?)}");
		sqlDelete.setCallable(true);
		set.setSqlDelete(sqlDelete);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		SQLInsert insertAnn = field.getAnnotationUsage(
				SQLInsert.class, ctx.getModelsContext());
		assertNotNull(insertAnn);
		assertEquals("INSERT INTO dept_emp VALUES(?, ?)", insertAnn.sql());

		SQLDelete deleteAnn = field.getAnnotationUsage(
				SQLDelete.class, ctx.getModelsContext());
		assertNotNull(deleteAnn);
		assertTrue(deleteAnn.callable());
	}

	@Test
	public void testImmutableCollection() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setMutable(false);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(Immutable.class, ctx.getModelsContext()));
	}

	@Test
	public void testOptimisticLockExcluded() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setOptimisticLock(false);

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		OptimisticLock olAnn = field.getAnnotationUsage(
				OptimisticLock.class, ctx.getModelsContext());
		assertNotNull(olAnn);
		assertTrue(olAnn.excluded());
	}

	@Test
	public void testJoinTable() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setTable("DEPT_EMP");
		set.setSchema("HR");
		set.setCatalog("COMPANY_DB");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		JoinTable jtAnn = field.getAnnotationUsage(
				JoinTable.class, ctx.getModelsContext());
		assertNotNull(jtAnn);
		assertEquals("DEPT_EMP", jtAnn.name());
		assertEquals("HR", jtAnn.schema());
		assertEquals("COMPANY_DB", jtAnn.catalog());
	}

	@Test
	public void testSortNatural() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setSort("natural");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNotNull(field.getAnnotationUsage(SortNatural.class, ctx.getModelsContext()));
	}

	@Test
	public void testOrderBy() {
		JaxbHbmSetType set = createSetWithOneToMany();
		set.setOrderBy("name ASC");

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		SQLOrder orderAnn = field.getAnnotationUsage(
				SQLOrder.class, ctx.getModelsContext());
		assertNotNull(orderAnn);
		assertEquals("name ASC", orderAnn.value());
	}

	@Test
	public void testDefaultMetadataAddsNothing() {
		JaxbHbmSetType set = createSetWithOneToMany();
		// All defaults

		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertNull(field.getAnnotationUsage(Cascade.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(Fetch.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(SQLRestriction.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(BatchSize.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(Cache.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(Immutable.class, ctx.getModelsContext()));
		assertNull(field.getAnnotationUsage(JoinTable.class, ctx.getModelsContext()));
	}
}
