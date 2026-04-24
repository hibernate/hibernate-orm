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
package org.hibernate.tool.reveng.internal.exporter.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MappingQueryAndFilterHelper}.
 *
 * @author Koen Aers
 */
class MappingQueryAndFilterHelperTest {

	private ModelsContext createContext() {
		return new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
	}

	private DynamicClassDetails createMinimalEntity(ModelsContext ctx) {
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		return entity;
	}

	private MappingQueryAndFilterHelper createHelper(DynamicClassDetails entity) {
		return new MappingQueryAndFilterHelper(entity);
	}

	// --- Filters ---

	@Test
	void testGetFiltersEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFilters().isEmpty());
	}

	@Test
	void testGetFiltersSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("activeFilter");
		filter.condition("active = true");
		entity.addAnnotationUsage(filter);
		List<MappingXmlHelper.FilterInfo> filters = createHelper(entity).getFilters();
		assertEquals(1, filters.size());
		assertEquals("activeFilter", filters.get(0).name());
		assertEquals("active = true", filters.get(0).condition());
	}

	@Test
	void testGetFilterDefsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFilterDefs().isEmpty());
	}

	@Test
	void testGetFilterDefsSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("myFilter");
		fd.defaultCondition("status = :status");
		ParamDefAnnotation pd = HibernateAnnotations.PARAM_DEF.createUsage(ctx);
		pd.name("status");
		pd.type(String.class);
		fd.parameters(new org.hibernate.annotations.ParamDef[]{pd});
		entity.addAnnotationUsage(fd);
		List<MappingXmlHelper.FilterDefInfo> defs = createHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		assertEquals("myFilter", defs.get(0).name());
		assertEquals("status = :status", defs.get(0).defaultCondition());
		assertEquals("java.lang.String", defs.get(0).parameters().get("status"));
	}

	@Test
	void testGetFilterDefsNoParams() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("simpleFilter");
		fd.defaultCondition("active = true");
		fd.parameters(new org.hibernate.annotations.ParamDef[]{});
		entity.addAnnotationUsage(fd);
		List<MappingXmlHelper.FilterDefInfo> defs = createHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		assertTrue(defs.get(0).parameters().isEmpty());
	}

	// --- Collection-level filters ---

	@Test
	void testGetCollectionFiltersEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		TypeDetails stringType = new ClassTypeDetailsImpl(
				ctx.getClassDetailsRegistry().resolveClassDetails("java.lang.String"),
				TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = new DynamicFieldDetails(
				"name", stringType, entity, 0, false, false, ctx);
		entity.addField(field);
		assertTrue(createHelper(entity).getCollectionFilters(field).isEmpty());
	}

	@Test
	void testGetCollectionFiltersSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		TypeDetails stringType = new ClassTypeDetailsImpl(
				ctx.getClassDetailsRegistry().resolveClassDetails("java.lang.String"),
				TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = new DynamicFieldDetails(
				"items", stringType, entity, 0, false, false, ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("itemFilter");
		filter.condition("active = 1");
		field.addAnnotationUsage(filter);
		entity.addField(field);
		List<MappingXmlHelper.FilterInfo> filters = createHelper(entity).getCollectionFilters(field);
		assertEquals(1, filters.size());
		assertEquals("itemFilter", filters.get(0).name());
		assertEquals("active = 1", filters.get(0).condition());
	}

	// --- Named queries ---

	@Test
	void testGetNamedQueriesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getNamedQueries().isEmpty());
	}

	@Test
	void testGetNamedQueriesSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findAll");
		nq.query("from TestEntity");
		entity.addAnnotationUsage(nq);
		List<MappingXmlHelper.NamedQueryInfo> queries = createHelper(entity).getNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("findAll", queries.get(0).name());
		assertEquals("from TestEntity", queries.get(0).query());
	}

	@Test
	void testGetNamedNativeQueriesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getNamedNativeQueries().isEmpty());
	}

	@Test
	void testGetNamedNativeQueriesSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findAllNative");
		nnq.query("SELECT * FROM test_entity");
		entity.addAnnotationUsage(nnq);
		List<MappingXmlHelper.NamedQueryInfo> queries = createHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals("findAllNative", queries.get(0).name());
		assertEquals("SELECT * FROM test_entity", queries.get(0).query());
	}

	// --- Fetch profiles ---

	@Test
	void testGetFetchProfilesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFetchProfiles().isEmpty());
	}

	@Test
	void testGetFetchProfilesSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FetchProfileAnnotation fp = HibernateAnnotations.FETCH_PROFILE.createUsage(ctx);
		fp.name("withOrders");
		fp.fetchOverrides(new org.hibernate.annotations.FetchProfile.FetchOverride[]{});
		entity.addAnnotationUsage(fp);
		List<MappingXmlHelper.FetchProfileInfo> profiles = createHelper(entity).getFetchProfiles();
		assertEquals(1, profiles.size());
		assertEquals("withOrders", profiles.get(0).name());
		assertTrue(profiles.get(0).overrides().isEmpty());
	}

	// --- SQL operations ---

	@Test
	void testGetSQLInsertNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLInsert());
	}

	@Test
	void testGetSQLInsert() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("INSERT INTO t (id, name) VALUES (?, ?)");
		si.callable(false);
		entity.addAnnotationUsage(si);
		MappingXmlHelper.CustomSqlInfo info = createHelper(entity).getSQLInsert();
		assertNotNull(info);
		assertEquals("INSERT INTO t (id, name) VALUES (?, ?)", info.sql());
		assertFalse(info.callable());
	}

	@Test
	void testGetSQLInsertCallable() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("{call insert_entity(?, ?)}");
		si.callable(true);
		entity.addAnnotationUsage(si);
		MappingXmlHelper.CustomSqlInfo info = createHelper(entity).getSQLInsert();
		assertTrue(info.callable());
	}

	@Test
	void testGetSQLUpdateNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLUpdate());
	}

	@Test
	void testGetSQLUpdate() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx);
		su.sql("UPDATE t SET name=? WHERE id=?");
		su.callable(false);
		entity.addAnnotationUsage(su);
		MappingXmlHelper.CustomSqlInfo info = createHelper(entity).getSQLUpdate();
		assertNotNull(info);
		assertEquals("UPDATE t SET name=? WHERE id=?", info.sql());
	}

	@Test
	void testGetSQLDeleteNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLDelete());
	}

	@Test
	void testGetSQLDelete() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx);
		sd.sql("DELETE FROM t WHERE id=?");
		sd.callable(false);
		entity.addAnnotationUsage(sd);
		MappingXmlHelper.CustomSqlInfo info = createHelper(entity).getSQLDelete();
		assertNotNull(info);
		assertEquals("DELETE FROM t WHERE id=?", info.sql());
	}

	@Test
	void testGetSQLDeleteAllNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLDeleteAll());
	}

	@Test
	void testGetSQLDeleteAll() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLDeleteAllAnnotation sda = HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
		sda.sql("DELETE FROM t");
		sda.callable(false);
		entity.addAnnotationUsage(sda);
		MappingXmlHelper.CustomSqlInfo info = createHelper(entity).getSQLDeleteAll();
		assertNotNull(info);
		assertEquals("DELETE FROM t", info.sql());
	}

	// --- Entity listeners ---

	@Test
	void testGetEntityListenerClassNamesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getEntityListenerClassNames().isEmpty());
	}

	// --- Lifecycle callbacks ---

	@Test
	void testGetLifecycleCallbacksEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getLifecycleCallbacks().isEmpty());
	}
}
