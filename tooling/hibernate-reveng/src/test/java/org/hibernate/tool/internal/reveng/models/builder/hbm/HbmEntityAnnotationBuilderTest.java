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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;

public class HbmEntityAnnotationBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx.getModelsContext());
	}

	// --- Filter tests ---

	@Test
	public void testSingleFilter() {
		JaxbHbmFilterType filter = new JaxbHbmFilterType();
		filter.setName("activeFilter");
		filter.setCondition("active = true");

		HbmEntityAnnotationBuilder.processFilters(entityClass,
				List.of(filter), ctx);

		Filter filterAnn = entityClass.getAnnotationUsage(
				Filter.class, ctx.getModelsContext());
		assertNotNull(filterAnn);
		assertEquals("activeFilter", filterAnn.name());
		assertEquals("active = true", filterAnn.condition());
	}

	@Test
	public void testMultipleFilters() {
		JaxbHbmFilterType f1 = new JaxbHbmFilterType();
		f1.setName("filter1");
		JaxbHbmFilterType f2 = new JaxbHbmFilterType();
		f2.setName("filter2");

		HbmEntityAnnotationBuilder.processFilters(entityClass,
				List.of(f1, f2), ctx);

		Filters filtersAnn = entityClass.getAnnotationUsage(
				Filters.class, ctx.getModelsContext());
		assertNotNull(filtersAnn);
		assertEquals(2, filtersAnn.value().length);
		assertEquals("filter1", filtersAnn.value()[0].name());
		assertEquals("filter2", filtersAnn.value()[1].name());
	}

	@Test
	public void testEmptyFilters() {
		HbmEntityAnnotationBuilder.processFilters(entityClass,
				List.of(), ctx);

		assertNull(entityClass.getAnnotationUsage(
				Filter.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(
				Filters.class, ctx.getModelsContext()));
	}

	// --- FilterDef tests ---

	@Test
	public void testSingleFilterDef() {
		JaxbHbmFilterDefinitionType filterDef = new JaxbHbmFilterDefinitionType();
		filterDef.setName("activeFilter");
		filterDef.setCondition("active = :isActive");

		HbmEntityAnnotationBuilder.processFilterDefs(entityClass,
				List.of(filterDef), ctx);

		FilterDef filterDefAnn = entityClass.getAnnotationUsage(
				FilterDef.class, ctx.getModelsContext());
		assertNotNull(filterDefAnn);
		assertEquals("activeFilter", filterDefAnn.name());
		assertEquals("active = :isActive", filterDefAnn.defaultCondition());
	}

	@Test
	public void testMultipleFilterDefs() {
		JaxbHbmFilterDefinitionType fd1 = new JaxbHbmFilterDefinitionType();
		fd1.setName("filterDef1");
		JaxbHbmFilterDefinitionType fd2 = new JaxbHbmFilterDefinitionType();
		fd2.setName("filterDef2");

		HbmEntityAnnotationBuilder.processFilterDefs(entityClass,
				List.of(fd1, fd2), ctx);

		FilterDefs filterDefsAnn = entityClass.getAnnotationUsage(
				FilterDefs.class, ctx.getModelsContext());
		assertNotNull(filterDefsAnn);
		assertEquals(2, filterDefsAnn.value().length);
	}

	// --- FetchProfile tests ---

	@Test
	public void testSingleFetchProfile() {
		JaxbHbmFetchProfileType fp = new JaxbHbmFetchProfileType();
		fp.setName("withDepartment");

		HbmEntityAnnotationBuilder.processFetchProfiles(entityClass,
				List.of(fp), ctx);

		FetchProfile fpAnn = entityClass.getAnnotationUsage(
				FetchProfile.class, ctx.getModelsContext());
		assertNotNull(fpAnn);
		assertEquals("withDepartment", fpAnn.name());
	}

	@Test
	public void testMultipleFetchProfiles() {
		JaxbHbmFetchProfileType fp1 = new JaxbHbmFetchProfileType();
		fp1.setName("profile1");
		JaxbHbmFetchProfileType fp2 = new JaxbHbmFetchProfileType();
		fp2.setName("profile2");

		HbmEntityAnnotationBuilder.processFetchProfiles(entityClass,
				List.of(fp1, fp2), ctx);

		FetchProfiles fpsAnn = entityClass.getAnnotationUsage(
				FetchProfiles.class, ctx.getModelsContext());
		assertNotNull(fpsAnn);
		assertEquals(2, fpsAnn.value().length);
		assertEquals("profile1", fpsAnn.value()[0].name());
		assertEquals("profile2", fpsAnn.value()[1].name());
	}

	// --- SQL DML tests ---

	@Test
	public void testSqlInsert() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		JaxbHbmCustomSqlDmlType sqlInsert = new JaxbHbmCustomSqlDmlType();
		sqlInsert.setValue("INSERT INTO emp VALUES(?, ?)");
		sqlInsert.setCallable(false);
		entityType.setSqlInsert(sqlInsert);

		HbmEntityAnnotationBuilder.processSqlStatements(entityClass,
				entityType, ctx);

		SQLInsert ann = entityClass.getAnnotationUsage(
				SQLInsert.class, ctx.getModelsContext());
		assertNotNull(ann);
		assertEquals("INSERT INTO emp VALUES(?, ?)", ann.sql());
		assertFalse(ann.callable());
	}

	@Test
	public void testSqlUpdate() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		JaxbHbmCustomSqlDmlType sqlUpdate = new JaxbHbmCustomSqlDmlType();
		sqlUpdate.setValue("UPDATE emp SET name=? WHERE id=?");
		sqlUpdate.setCallable(false);
		entityType.setSqlUpdate(sqlUpdate);

		HbmEntityAnnotationBuilder.processSqlStatements(entityClass,
				entityType, ctx);

		SQLUpdate ann = entityClass.getAnnotationUsage(
				SQLUpdate.class, ctx.getModelsContext());
		assertNotNull(ann);
		assertEquals("UPDATE emp SET name=? WHERE id=?", ann.sql());
	}

	@Test
	public void testSqlDelete() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		JaxbHbmCustomSqlDmlType sqlDelete = new JaxbHbmCustomSqlDmlType();
		sqlDelete.setValue("{call deleteEmployee(?)}");
		sqlDelete.setCallable(true);
		entityType.setSqlDelete(sqlDelete);

		HbmEntityAnnotationBuilder.processSqlStatements(entityClass,
				entityType, ctx);

		SQLDelete ann = entityClass.getAnnotationUsage(
				SQLDelete.class, ctx.getModelsContext());
		assertNotNull(ann);
		assertEquals("{call deleteEmployee(?)}", ann.sql());
		assertTrue(ann.callable());
	}

	@Test
	public void testNoSqlStatements() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();

		HbmEntityAnnotationBuilder.processSqlStatements(entityClass,
				entityType, ctx);

		assertNull(entityClass.getAnnotationUsage(
				SQLInsert.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(
				SQLUpdate.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(
				SQLDelete.class, ctx.getModelsContext()));
	}

	// --- SecondaryTable tests ---

	@Test
	public void testSingleSecondaryTable() {
		JaxbHbmSecondaryTableType join = new JaxbHbmSecondaryTableType();
		join.setTable("EMP_DETAILS");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("EMP_ID");
		join.setKey(key);

		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				List.of(join), ctx);

		SecondaryTable stAnn = entityClass.getAnnotationUsage(
				SecondaryTable.class, ctx.getModelsContext());
		assertNotNull(stAnn);
		assertEquals("EMP_DETAILS", stAnn.name());
		assertEquals(1, stAnn.pkJoinColumns().length);
		assertEquals("EMP_ID", stAnn.pkJoinColumns()[0].name());
	}

	@Test
	public void testMultipleSecondaryTables() {
		JaxbHbmSecondaryTableType join1 = new JaxbHbmSecondaryTableType();
		join1.setTable("EMP_DETAILS");
		JaxbHbmKeyType key1 = new JaxbHbmKeyType();
		key1.setColumnAttribute("EMP_ID");
		join1.setKey(key1);

		JaxbHbmSecondaryTableType join2 = new JaxbHbmSecondaryTableType();
		join2.setTable("EMP_SALARY");
		JaxbHbmKeyType key2 = new JaxbHbmKeyType();
		key2.setColumnAttribute("EMP_ID");
		join2.setKey(key2);

		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				List.of(join1, join2), ctx);

		SecondaryTables stsAnn = entityClass.getAnnotationUsage(
				SecondaryTables.class, ctx.getModelsContext());
		assertNotNull(stsAnn);
		assertEquals(2, stsAnn.value().length);
		assertEquals("EMP_DETAILS", stsAnn.value()[0].name());
		assertEquals("EMP_SALARY", stsAnn.value()[1].name());
	}

	@Test
	public void testSecondaryTableWithSchemaAndCatalog() {
		JaxbHbmSecondaryTableType join = new JaxbHbmSecondaryTableType();
		join.setTable("EMP_DETAILS");
		join.setSchema("HR");
		join.setCatalog("COMPANY_DB");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("EMP_ID");
		join.setKey(key);

		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				List.of(join), ctx);

		SecondaryTable stAnn = entityClass.getAnnotationUsage(
				SecondaryTable.class, ctx.getModelsContext());
		assertNotNull(stAnn);
		assertEquals("HR", stAnn.schema());
		assertEquals("COMPANY_DB", stAnn.catalog());
	}

	@Test
	public void testSecondaryTableMultipleKeyColumns() {
		JaxbHbmSecondaryTableType join = new JaxbHbmSecondaryTableType();
		join.setTable("EMP_DETAILS");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		JaxbHbmColumnType col1 = new JaxbHbmColumnType();
		col1.setName("EMP_ID");
		JaxbHbmColumnType col2 = new JaxbHbmColumnType();
		col2.setName("DEPT_ID");
		key.getColumn().add(col1);
		key.getColumn().add(col2);
		join.setKey(key);

		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				List.of(join), ctx);

		SecondaryTable stAnn = entityClass.getAnnotationUsage(
				SecondaryTable.class, ctx.getModelsContext());
		assertNotNull(stAnn);
		PrimaryKeyJoinColumn[] pkCols = stAnn.pkJoinColumns();
		assertEquals(2, pkCols.length);
		assertEquals("EMP_ID", pkCols[0].name());
		assertEquals("DEPT_ID", pkCols[1].name());
	}

	@Test
	public void testEmptySecondaryTables() {
		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				List.of(), ctx);

		assertNull(entityClass.getAnnotationUsage(
				SecondaryTable.class, ctx.getModelsContext()));
	}
}
