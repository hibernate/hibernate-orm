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
package org.hibernate.tool.internal.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.annotations.Filter;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HbmQueryAndFilterHelper}.
 *
 * @author Koen Aers
 */
class HbmQueryAndFilterHelperTest {

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

	private DynamicFieldDetails addBasicField(
			DynamicClassDetails entity, String fieldName, Class<?> javaType, ModelsContext ctx) {
		ClassDetails typeClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(javaType.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(typeClass, TypeDetails.Kind.CLASS);
		return entity.applyAttribute(fieldName, fieldType, false, false, ctx);
	}

	private HbmQueryAndFilterHelper createHelper(DynamicClassDetails entity) {
		return new HbmQueryAndFilterHelper(entity, Collections.emptyMap());
	}

	private HbmQueryAndFilterHelper createHelper(DynamicClassDetails entity,
												  Map<String, List<String>> metaAttributes) {
		return new HbmQueryAndFilterHelper(entity, metaAttributes);
	}

	// --- Filters ---

	@Test
	void testGetFiltersNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFilters().isEmpty());
	}

	@Test
	void testGetFiltersSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("activeFilter");
		filter.condition("active = :isActive");
		entity.addAnnotationUsage(filter);
		List<HbmTemplateHelper.FilterInfo> filters = createHelper(entity).getFilters();
		assertEquals(1, filters.size());
		assertEquals("activeFilter", filters.get(0).name());
		assertEquals("active = :isActive", filters.get(0).condition());
	}

	@Test
	void testGetFiltersMultiple() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterAnnotation f1 = HibernateAnnotations.FILTER.createUsage(ctx);
		f1.name("activeFilter");
		f1.condition("active = true");
		FilterAnnotation f2 = HibernateAnnotations.FILTER.createUsage(ctx);
		f2.name("tenantFilter");
		f2.condition("tenant_id = :tid");
		FiltersAnnotation filters = HibernateAnnotations.FILTERS.createUsage(ctx);
		filters.value(new Filter[] { f1, f2 });
		entity.addAnnotationUsage(filters);
		List<HbmTemplateHelper.FilterInfo> result = createHelper(entity).getFilters();
		assertEquals(2, result.size());
		assertEquals("activeFilter", result.get(0).name());
		assertEquals("tenantFilter", result.get(1).name());
	}

	// --- Filter defs ---

	@Test
	void testGetFilterDefsNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFilterDefs().isEmpty());
	}

	@Test
	void testGetFilterDefsSimple() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("activeFilter");
		fd.defaultCondition("active = true");
		entity.addAnnotationUsage(fd);
		List<HbmTemplateHelper.FilterDefInfo> defs = createHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		assertEquals("activeFilter", defs.get(0).name());
		assertEquals("active = true", defs.get(0).defaultCondition());
		assertTrue(defs.get(0).parameters().isEmpty());
	}

	@Test
	void testGetFilterDefsWithParams() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ParamDefAnnotation pd = new ParamDefAnnotation(ctx);
		pd.name("isActive");
		pd.type(Boolean.class);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("activeFilter");
		fd.defaultCondition("active = :isActive");
		fd.parameters(new org.hibernate.annotations.ParamDef[] { pd });
		entity.addAnnotationUsage(fd);
		List<HbmTemplateHelper.FilterDefInfo> defs = createHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		Map<String, String> params = defs.get(0).parameters();
		assertEquals(1, params.size());
		assertEquals("java.lang.Boolean", params.get("isActive"));
	}

	// --- Named queries ---

	@Test
	void testGetNamedQueriesNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getNamedQueries().isEmpty());
	}

	@Test
	void testGetNamedQueriesJpaSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findAll");
		nq.query("SELECT e FROM TestEntity e");
		entity.addAnnotationUsage(nq);
		List<HbmTemplateHelper.NamedQueryInfo> queries = createHelper(entity).getNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("findAll", queries.get(0).name());
		assertEquals("SELECT e FROM TestEntity e", queries.get(0).query());
		assertEquals("", queries.get(0).flushMode());
		assertFalse(queries.get(0).cacheable());
	}

	@Test
	void testGetNamedQueriesHibernateWithAttributes() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedQueryAnnotation nq = HibernateAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findActive");
		nq.query("SELECT e FROM TestEntity e WHERE e.active = true");
		nq.flushMode(org.hibernate.annotations.FlushModeType.AUTO);
		nq.cacheable(true);
		nq.cacheRegion("myRegion");
		nq.fetchSize(25);
		nq.timeout(5000);
		nq.comment("Find active entities");
		nq.readOnly(true);
		entity.addAnnotationUsage(nq);
		List<HbmTemplateHelper.NamedQueryInfo> queries = createHelper(entity).getNamedQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedQueryInfo info = queries.get(0);
		assertEquals("findActive", info.name());
		assertEquals("auto", info.flushMode());
		assertTrue(info.cacheable());
		assertEquals("myRegion", info.cacheRegion());
		assertEquals(25, info.fetchSize());
		assertEquals(5000, info.timeout());
		assertEquals("Find active entities", info.comment());
		assertTrue(info.readOnly());
	}

	// --- Named native queries ---

	@Test
	void testGetNamedNativeQueriesNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getNamedNativeQueries().isEmpty());
	}

	@Test
	void testGetNamedNativeQueriesJpaSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findAllNative");
		nnq.query("SELECT * FROM TEST_ENTITY");
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals("findAllNative", queries.get(0).name());
		assertEquals("SELECT * FROM TEST_ENTITY", queries.get(0).query());
	}

	@Test
	void testGetNamedNativeQueriesHibernateWithAttributes() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findNativeActive");
		nnq.query("SELECT * FROM TEST_ENTITY WHERE ACTIVE = 1");
		nnq.flushMode(org.hibernate.annotations.FlushModeType.COMMIT);
		nnq.cacheable(true);
		nnq.cacheRegion("nativeRegion");
		nnq.fetchSize(50);
		nnq.timeout(3000);
		nnq.comment("Native find active");
		nnq.readOnly(true);
		nnq.querySpaces(new String[]{"TEST_ENTITY", "OTHER_TABLE"});
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedNativeQueryInfo info = queries.get(0);
		assertEquals("findNativeActive", info.name());
		assertEquals("commit", info.flushMode());
		assertTrue(info.cacheable());
		assertEquals("nativeRegion", info.cacheRegion());
		assertEquals(50, info.fetchSize());
		assertEquals(3000, info.timeout());
		assertTrue(info.readOnly());
		assertEquals(2, info.querySpaces().size());
	}

	@Test
	void testGetNamedNativeQueriesWithResultClass() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findEmployees");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultClass(String.class);
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals(1, queries.get(0).entityReturns().size());
		assertEquals("java.lang.String", queries.get(0).entityReturns().get(0).entityClass());
	}

	@Test
	void testGetNamedNativeQueriesWithResultSetMapping() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx);
		mapping.name("empMapping");
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(ctx);
		er.entityClass(String.class);
		FieldResultJpaAnnotation fr = JpaAnnotations.FIELD_RESULT.createUsage(ctx);
		fr.name("id");
		fr.column("EMP_ID");
		er.fields(new jakarta.persistence.FieldResult[]{fr});
		mapping.entities(new jakarta.persistence.EntityResult[]{er});
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(ctx);
		cr.name("DEPT_NAME");
		mapping.columns(new jakarta.persistence.ColumnResult[]{cr});
		entity.addAnnotationUsage(mapping);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findWithMapping");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultSetMapping("empMapping");
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedNativeQueryInfo info = queries.get(0);
		assertEquals(1, info.entityReturns().size());
		assertEquals("java.lang.String", info.entityReturns().get(0).entityClass());
		assertEquals(1, info.entityReturns().get(0).fieldMappings().size());
		assertEquals("id", info.entityReturns().get(0).fieldMappings().get(0).name());
		assertEquals("EMP_ID", info.entityReturns().get(0).fieldMappings().get(0).column());
		assertEquals(1, info.scalarReturns().size());
		assertEquals("DEPT_NAME", info.scalarReturns().get(0).column());
	}

	@Test
	void testGetNamedNativeQueriesWithMetaReturnJoin() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findWithJoin");
		nnq.query("SELECT * FROM EMP e JOIN DEPT d ON e.dept_id = d.id");
		entity.addAnnotationUsage(nnq);
		Map<String, List<String>> meta = new HashMap<>();
		meta.put("hibernate.sql-query.findWithJoin.return-join.alias", List.of("d"));
		meta.put("hibernate.sql-query.findWithJoin.return-join.property", List.of("e.department"));
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity, meta).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals(1, queries.get(0).returnJoins().size());
		assertEquals("d", queries.get(0).returnJoins().get(0).alias());
		assertEquals("e.department", queries.get(0).returnJoins().get(0).property());
	}

	@Test
	void testGetNamedNativeQueriesWithMetaLoadCollection() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findWithColl");
		nnq.query("SELECT * FROM EMP");
		entity.addAnnotationUsage(nnq);
		Map<String, List<String>> meta = new HashMap<>();
		meta.put("hibernate.sql-query.findWithColl.load-collection.alias", List.of("items"));
		meta.put("hibernate.sql-query.findWithColl.load-collection.role", List.of("com.example.Emp.items"));
		meta.put("hibernate.sql-query.findWithColl.load-collection.lock-mode", List.of("read"));
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity, meta).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals(1, queries.get(0).loadCollections().size());
		assertEquals("items", queries.get(0).loadCollections().get(0).alias());
		assertEquals("com.example.Emp.items", queries.get(0).loadCollections().get(0).role());
		assertEquals("read", queries.get(0).loadCollections().get(0).lockMode());
	}

	@Test
	void testGetNamedNativeQueriesWithMetaReturn() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findEmp");
		nnq.query("SELECT * FROM EMP");
		entity.addAnnotationUsage(nnq);
		Map<String, List<String>> meta = new HashMap<>();
		meta.put("hibernate.sql-query.findEmp.return.alias", List.of("emp"));
		meta.put("hibernate.sql-query.findEmp.return.class", List.of("com.example.Employee"));
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = createHelper(entity, meta).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals(1, queries.get(0).entityReturns().size());
		assertEquals("emp", queries.get(0).entityReturns().get(0).alias());
		assertEquals("com.example.Employee", queries.get(0).entityReturns().get(0).entityClass());
	}

	// --- Joins ---

	@Test
	void testGetJoinsNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getJoins().isEmpty());
	}

	@Test
	void testGetJoinsSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("EMP_ID");
		SecondaryTableJpaAnnotation st = JpaAnnotations.SECONDARY_TABLE.createUsage(ctx);
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] { pkjc });
		entity.addAnnotationUsage(st);
		List<HbmTemplateHelper.JoinInfo> joins = createHelper(entity).getJoins();
		assertEquals(1, joins.size());
		assertEquals("EMP_DETAIL", joins.get(0).tableName());
		assertEquals(1, joins.get(0).keyColumns().size());
		assertEquals("EMP_ID", joins.get(0).keyColumns().get(0));
	}

	@Test
	void testGetJoinProperties() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("DETAIL");
		col.table("EMP_DETAIL");
		field.addAnnotationUsage(col);
		addBasicField(entity, "name", String.class, ctx);
		List<org.hibernate.models.spi.FieldDetails> result = createHelper(entity).getJoinProperties("EMP_DETAIL");
		assertEquals(1, result.size());
		assertEquals("detail", result.get(0).getName());
	}

	@Test
	void testGetJoinComment() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, List<String>> meta = new HashMap<>();
		meta.put("hibernate.join.comment.EMP_DETAIL", List.of("Employee details table"));
		assertEquals("Employee details table",
				createHelper(entity, meta).getJoinComment("EMP_DETAIL"));
	}

	@Test
	void testGetJoinCommentNone() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getJoinComment("NONEXISTENT"));
	}

	// --- SQL operations ---

	@Test
	void testGetSQLInsert() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("INSERT INTO T (name) VALUES (?)");
		entity.addAnnotationUsage(si);
		HbmTemplateHelper.CustomSqlInfo info = createHelper(entity).getSQLInsert();
		assertNotNull(info);
		assertEquals("INSERT INTO T (name) VALUES (?)", info.sql());
		assertFalse(info.callable());
	}

	@Test
	void testGetSQLInsertNone() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLInsert());
	}

	@Test
	void testGetSQLUpdate() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx);
		su.sql("UPDATE T SET name = ? WHERE id = ?");
		entity.addAnnotationUsage(su);
		HbmTemplateHelper.CustomSqlInfo info = createHelper(entity).getSQLUpdate();
		assertNotNull(info);
		assertEquals("UPDATE T SET name = ? WHERE id = ?", info.sql());
	}

	@Test
	void testGetSQLDelete() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx);
		sd.sql("{call deleteEntity(?)}");
		sd.callable(true);
		entity.addAnnotationUsage(sd);
		HbmTemplateHelper.CustomSqlInfo info = createHelper(entity).getSQLDelete();
		assertNotNull(info);
		assertTrue(info.callable());
	}

	@Test
	void testGetSQLDeleteNone() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLDelete());
	}

	@Test
	void testGetSQLDeleteAll() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLDeleteAllAnnotation sda = HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
		sda.sql("DELETE FROM T WHERE owner_id = ?");
		entity.addAnnotationUsage(sda);
		HbmTemplateHelper.CustomSqlInfo info = createHelper(entity).getSQLDeleteAll();
		assertNotNull(info);
		assertEquals("DELETE FROM T WHERE owner_id = ?", info.sql());
	}

	@Test
	void testGetSQLDeleteAllNone() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSQLDeleteAll());
	}

	// --- Fetch profiles ---

	@Test
	void testGetFetchProfilesNone() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getFetchProfiles().isEmpty());
	}

	@Test
	void testGetFetchProfilesPresent() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FetchProfileAnnotation fp = HibernateAnnotations.FETCH_PROFILE.createUsage(ctx);
		fp.name("eager-loading");
		fp.fetchOverrides(new org.hibernate.annotations.FetchProfile.FetchOverride[] {});
		entity.addAnnotationUsage(fp);
		List<HbmTemplateHelper.FetchProfileInfo> profiles = createHelper(entity).getFetchProfiles();
		assertEquals(1, profiles.size());
		assertEquals("eager-loading", profiles.get(0).name());
	}
}
