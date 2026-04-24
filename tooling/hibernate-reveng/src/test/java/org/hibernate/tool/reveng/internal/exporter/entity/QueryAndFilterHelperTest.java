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
package org.hibernate.tool.reveng.internal.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterDefInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedNativeQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.SecondaryTableInfo;
import org.junit.jupiter.api.Test;

class QueryAndFilterHelperTest {

	private record TestContext(
			QueryAndFilterHelper helper,
			ModelsContext modelsContext,
			ClassDetails classDetails) {}

	private TestContext create() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		QueryAndFilterHelper helper = new QueryAndFilterHelper(classDetails);
		return new TestContext(helper, builder.getModelsContext(), classDetails);
	}

	// --- Named queries ---

	@Test
	void testGetNamedQueriesEmpty() {
		assertTrue(create().helper().getNamedQueries().isEmpty());
	}

	@Test
	void testGetNamedQueries() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx.modelsContext());
		nq.name("Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		List<NamedQueryInfo> queries = ctx.helper().getNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("Employee.findAll", queries.get(0).name());
		assertEquals("SELECT e FROM Employee e", queries.get(0).query());
	}

	// --- Named native queries ---

	@Test
	void testGetNamedNativeQueriesEmpty() {
		assertTrue(create().helper().getNamedNativeQueries().isEmpty());
	}

	@Test
	void testGetNamedNativeQueries() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedNativeQueryJpaAnnotation nnq =
				JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx.modelsContext());
		nnq.name("Employee.findByDept");
		nnq.query("SELECT * FROM EMPLOYEE WHERE DEPT = ?");
		dc.addAnnotationUsage(nnq);
		List<NamedNativeQueryInfo> queries = ctx.helper().getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals("Employee.findByDept", queries.get(0).name());
		assertEquals("SELECT * FROM EMPLOYEE WHERE DEPT = ?", queries.get(0).query());
	}

	// --- Filters ---

	@Test
	void testGetFiltersEmpty() {
		assertTrue(create().helper().getFilters().isEmpty());
	}

	@Test
	void testGetFilters() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeOnly");
		filter.condition("active = true");
		dc.addAnnotationUsage(filter);
		List<FilterInfo> filters = ctx.helper().getFilters();
		assertEquals(1, filters.size());
		assertEquals("activeOnly", filters.get(0).name());
		assertEquals("active = true", filters.get(0).condition());
	}

	// --- Filter defs ---

	@Test
	void testGetFilterDefsEmpty() {
		assertTrue(create().helper().getFilterDefs().isEmpty());
	}

	@Test
	void testGetFilterDefsWithParams() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		ParamDefAnnotation pd = HibernateAnnotations.PARAM_DEF.createUsage(ctx.modelsContext());
		pd.name("deptId");
		pd.type(Long.class);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx.modelsContext());
		fd.name("byDept");
		fd.defaultCondition("dept_id = :deptId");
		fd.parameters(new org.hibernate.annotations.ParamDef[] { pd });
		dc.addAnnotationUsage(fd);
		List<FilterDefInfo> defs = ctx.helper().getFilterDefs();
		assertEquals(1, defs.size());
		assertEquals("byDept", defs.get(0).name());
		assertEquals("dept_id = :deptId", defs.get(0).defaultCondition());
		assertTrue(defs.get(0).parameters().containsKey("deptId"));
		assertEquals(Long.class, defs.get(0).parameters().get("deptId"));
	}

	// --- SQL DML ---

	@Test
	void testGetSQLInsertsEmpty() {
		assertTrue(create().helper().getSQLInserts().isEmpty());
	}

	@Test
	void testGetSQLInserts() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx.modelsContext());
		si.sql("INSERT INTO EMPLOYEE (name) VALUES (?)");
		dc.addAnnotationUsage(si);
		assertEquals(1, ctx.helper().getSQLInserts().size());
	}

	@Test
	void testGetSQLUpdatesEmpty() {
		assertTrue(create().helper().getSQLUpdates().isEmpty());
	}

	@Test
	void testGetSQLUpdates() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx.modelsContext());
		su.sql("UPDATE EMPLOYEE SET name = ? WHERE id = ?");
		dc.addAnnotationUsage(su);
		assertEquals(1, ctx.helper().getSQLUpdates().size());
	}

	@Test
	void testGetSQLDeletesEmpty() {
		assertTrue(create().helper().getSQLDeletes().isEmpty());
	}

	@Test
	void testGetSQLDeletes() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx.modelsContext());
		sd.sql("DELETE FROM EMPLOYEE WHERE id = ?");
		dc.addAnnotationUsage(sd);
		assertEquals(1, ctx.helper().getSQLDeletes().size());
	}

	// --- Fetch profiles ---

	@Test
	void testGetFetchProfilesEmpty() {
		assertTrue(create().helper().getFetchProfiles().isEmpty());
	}

	// --- Secondary tables ---

	@Test
	void testGetSecondaryTablesEmpty() {
		assertTrue(create().helper().getSecondaryTables().isEmpty());
	}

	@Test
	void testGetSecondaryTables() {
		TestContext ctx = create();
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		PrimaryKeyJoinColumnJpaAnnotation pkjc =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx.modelsContext());
		pkjc.name("EMP_ID");
		SecondaryTableJpaAnnotation st =
				JpaAnnotations.SECONDARY_TABLE.createUsage(ctx.modelsContext());
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] { pkjc });
		dc.addAnnotationUsage(st);
		List<SecondaryTableInfo> tables = ctx.helper().getSecondaryTables();
		assertEquals(1, tables.size());
		assertEquals("EMP_DETAIL", tables.get(0).tableName());
		assertEquals(1, tables.get(0).keyColumns().size());
		assertEquals("EMP_ID", tables.get(0).keyColumns().get(0));
	}
}
