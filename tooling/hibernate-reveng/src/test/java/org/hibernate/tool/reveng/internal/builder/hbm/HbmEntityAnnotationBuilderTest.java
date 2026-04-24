/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLoaderType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;

import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
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

	// --- Entity Behavior tests ---

	@Test
	public void testImmutable() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setMutable(false);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		assertNotNull(entityClass.getAnnotationUsage(
				Immutable.class, ctx.getModelsContext()));
	}

	@Test
	public void testMutableDoesNotAddImmutable() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		// mutable defaults to true

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		assertNull(entityClass.getAnnotationUsage(
				Immutable.class, ctx.getModelsContext()));
	}

	@Test
	public void testDynamicInsert() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setDynamicInsert(true);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		assertNotNull(entityClass.getAnnotationUsage(
				DynamicInsert.class, ctx.getModelsContext()));
	}

	@Test
	public void testDynamicUpdate() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setDynamicUpdate(true);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		assertNotNull(entityClass.getAnnotationUsage(
				DynamicUpdate.class, ctx.getModelsContext()));
	}

	@Test
	public void testBatchSize() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setBatchSize(25);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		BatchSize bsAnn = entityClass.getAnnotationUsage(
				BatchSize.class, ctx.getModelsContext());
		assertNotNull(bsAnn);
		assertEquals(25, bsAnn.size());
	}

	@Test
	public void testOptimisticLockingDirty() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setOptimisticLock(OptimisticLockStyle.DIRTY);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		OptimisticLocking olAnn = entityClass.getAnnotationUsage(
				OptimisticLocking.class, ctx.getModelsContext());
		assertNotNull(olAnn);
		assertEquals(OptimisticLockType.DIRTY, olAnn.type());
	}

	@Test
	public void testOptimisticLockingVersionDefault() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		// optimistic-lock defaults to VERSION

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		// VERSION is the default — no annotation needed
		assertNull(entityClass.getAnnotationUsage(
				OptimisticLocking.class, ctx.getModelsContext()));
	}

	@Test
	public void testWhere() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setWhere("deleted = false");

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		SQLRestriction srAnn = entityClass.getAnnotationUsage(
				SQLRestriction.class, ctx.getModelsContext());
		assertNotNull(srAnn);
		assertEquals("deleted = false", srAnn.value());
	}

	@Test
	public void testCheck() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setCheck("salary > 0");

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		Check checkAnn = entityClass.getAnnotationUsage(
				Check.class, ctx.getModelsContext());
		assertNotNull(checkAnn);
		assertEquals("salary > 0", checkAnn.constraints());
	}

	@Test
	public void testRowId() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setRowid("ROWID");

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		RowId rowIdAnn = entityClass.getAnnotationUsage(
				RowId.class, ctx.getModelsContext());
		assertNotNull(rowIdAnn);
		assertEquals("ROWID", rowIdAnn.value());
	}

	@Test
	public void testSubselect() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setSubselect("select id, name from emp where active = 1");

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		Subselect ssAnn = entityClass.getAnnotationUsage(
				Subselect.class, ctx.getModelsContext());
		assertNotNull(ssAnn);
		assertEquals("select id, name from emp where active = 1", ssAnn.value());
	}

	@Test
	public void testCache() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		JaxbHbmCacheType cache = new JaxbHbmCacheType();
		cache.setUsage(AccessType.READ_WRITE);
		cache.setRegion("employee_cache");
		entityType.setCache(cache);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		Cache cacheAnn = entityClass.getAnnotationUsage(
				Cache.class, ctx.getModelsContext());
		assertNotNull(cacheAnn);
		assertEquals(CacheConcurrencyStrategy.READ_WRITE, cacheAnn.usage());
		assertEquals("employee_cache", cacheAnn.region());
	}

	@Test
	public void testComment() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setComment("Main employee table");

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		Comment commentAnn = entityClass.getAnnotationUsage(
				Comment.class, ctx.getModelsContext());
		assertNotNull(commentAnn);
		assertEquals("Main employee table", commentAnn.value());
	}

	@Test
	public void testSynchronize() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		JaxbHbmSynchronizeType sync1 = new JaxbHbmSynchronizeType();
		sync1.setTable("employee_view");
		JaxbHbmSynchronizeType sync2 = new JaxbHbmSynchronizeType();
		sync2.setTable("department_view");
		entityType.getSynchronize().add(sync1);
		entityType.getSynchronize().add(sync2);

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		Synchronize syncAnn = entityClass.getAnnotationUsage(
				Synchronize.class, ctx.getModelsContext());
		assertNotNull(syncAnn);
		assertEquals(2, syncAnn.value().length);
		assertEquals("employee_view", syncAnn.value()[0]);
		assertEquals("department_view", syncAnn.value()[1]);
	}

	// --- Named Query tests ---

	@Test
	public void testSingleNamedQuery() {
		JaxbHbmNamedQueryType query = new JaxbHbmNamedQueryType();
		query.setName("findActiveEmployees");
		query.getContent().add("from Employee where active = true");

		HbmEntityAnnotationBuilder.processNamedQueries(entityClass,
				List.of(query), ctx);

		NamedQuery nqAnn = entityClass.getAnnotationUsage(
				NamedQuery.class, ctx.getModelsContext());
		assertNotNull(nqAnn);
		assertEquals("com.example.Employee.findActiveEmployees", nqAnn.name());
		assertEquals("from Employee where active = true", nqAnn.query());
	}

	@Test
	public void testMultipleNamedQueries() {
		JaxbHbmNamedQueryType q1 = new JaxbHbmNamedQueryType();
		q1.setName("query1");
		q1.getContent().add("from Foo");
		JaxbHbmNamedQueryType q2 = new JaxbHbmNamedQueryType();
		q2.setName("query2");
		q2.getContent().add("from Bar");

		HbmEntityAnnotationBuilder.processNamedQueries(entityClass,
				List.of(q1, q2), ctx);

		NamedQueries nqsAnn = entityClass.getAnnotationUsage(
				NamedQueries.class, ctx.getModelsContext());
		assertNotNull(nqsAnn);
		assertEquals(2, nqsAnn.value().length);
		assertEquals("com.example.Employee.query1", nqsAnn.value()[0].name());
		assertEquals("com.example.Employee.query2", nqsAnn.value()[1].name());
	}

	@Test
	public void testSingleNamedNativeQuery() {
		JaxbHbmNamedNativeQueryType query = new JaxbHbmNamedNativeQueryType();
		query.setName("findAllEmployees");
		query.getContent().add("SELECT * FROM employee");

		HbmEntityAnnotationBuilder.processNamedNativeQueries(entityClass,
				List.of(query), ctx);

		NamedNativeQuery nnqAnn = entityClass.getAnnotationUsage(
				NamedNativeQuery.class, ctx.getModelsContext());
		assertNotNull(nnqAnn);
		assertEquals("com.example.Employee.findAllEmployees", nnqAnn.name());
		assertEquals("SELECT * FROM employee", nnqAnn.query());
	}

	@Test
	public void testNamedNativeQueryWithResultSetMapping() {
		JaxbHbmNamedNativeQueryType query = new JaxbHbmNamedNativeQueryType();
		query.setName("findAllEmployees");
		query.getContent().add("SELECT * FROM employee");
		query.setResultsetRef("employeeMapping");

		HbmEntityAnnotationBuilder.processNamedNativeQueries(entityClass,
				List.of(query), ctx);

		NamedNativeQuery nnqAnn = entityClass.getAnnotationUsage(
				NamedNativeQuery.class, ctx.getModelsContext());
		assertNotNull(nnqAnn);
		assertEquals("employeeMapping", nnqAnn.resultSetMapping());
	}

	@Test
	public void testDefaultBehaviorAddsNothing() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		// All defaults

		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass,
				entityType, ctx);

		assertNull(entityClass.getAnnotationUsage(Immutable.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(DynamicInsert.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(DynamicUpdate.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(BatchSize.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(OptimisticLocking.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(SQLRestriction.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(Check.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(RowId.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(Subselect.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(Cache.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(Comment.class, ctx.getModelsContext()));
		assertNull(entityClass.getAnnotationUsage(Synchronize.class, ctx.getModelsContext()));
	}

	// --- Proxy / ConcreteProxy tests ---

	@Test
	public void testProxyAttribute() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setProxy("com.example.EmployeeProxy");

		HbmEntityAnnotationBuilder.processProxy(entityClass, entityType, ctx);

		assertNotNull(entityClass.getAnnotationUsage(
				ConcreteProxy.class, ctx.getModelsContext()));
	}

	@Test
	public void testLazyTrue() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setLazy(true);

		HbmEntityAnnotationBuilder.processProxy(entityClass, entityType, ctx);

		assertNotNull(entityClass.getAnnotationUsage(
				ConcreteProxy.class, ctx.getModelsContext()));
	}

	@Test
	public void testNoProxyOrLazyAddsNothing() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		// proxy is null, lazy is null (defaults)

		HbmEntityAnnotationBuilder.processProxy(entityClass, entityType, ctx);

		assertNull(entityClass.getAnnotationUsage(
				ConcreteProxy.class, ctx.getModelsContext()));
	}

	// --- Loader / HQLSelect tests ---

	@Test
	public void testLoader() {
		JaxbHbmLoaderType loader = new JaxbHbmLoaderType();
		loader.setQueryRef("loadEmployee");

		HbmEntityAnnotationBuilder.processLoader(entityClass, loader, ctx);

		HQLSelect hqlAnn = entityClass.getAnnotationUsage(
				HQLSelect.class, ctx.getModelsContext());
		assertNotNull(hqlAnn);
		assertEquals("loadEmployee", hqlAnn.query());
	}

	@Test
	public void testNoLoader() {
		HbmEntityAnnotationBuilder.processLoader(entityClass, null, ctx);

		assertNull(entityClass.getAnnotationUsage(
				HQLSelect.class, ctx.getModelsContext()));
	}

	// --- ResultSet Mapping tests ---

	@Test
	public void testSingleResultSetMappingWithScalar() {
		JaxbHbmResultSetMappingType rs = new JaxbHbmResultSetMappingType();
		rs.setName("employeeScalars");
		JaxbHbmNativeQueryScalarReturnType scalar = new JaxbHbmNativeQueryScalarReturnType();
		scalar.setColumn("emp_name");
		scalar.setType("string");
		rs.getValueMappingSources().add(scalar);

		HbmEntityAnnotationBuilder.processResultSetMappings(entityClass,
				List.of(rs), ctx);

		SqlResultSetMapping rsAnn = entityClass.getAnnotationUsage(
				SqlResultSetMapping.class, ctx.getModelsContext());
		assertNotNull(rsAnn);
		assertEquals("employeeScalars", rsAnn.name());
		assertEquals(1, rsAnn.columns().length);
		assertEquals("emp_name", rsAnn.columns()[0].name());
	}

	@Test
	public void testSingleResultSetMappingWithEntity() {
		JaxbHbmResultSetMappingType rs = new JaxbHbmResultSetMappingType();
		rs.setName("employeeEntity");
		JaxbHbmNativeQueryReturnType entityReturn = new JaxbHbmNativeQueryReturnType();
		entityReturn.setClazz("java.lang.String");  // Using String as a resolvable class
		JaxbHbmNativeQueryPropertyReturnType prop = new JaxbHbmNativeQueryPropertyReturnType();
		prop.setName("name");
		prop.setColumn("emp_name");
		entityReturn.getReturnProperty().add(prop);
		rs.getValueMappingSources().add(entityReturn);

		HbmEntityAnnotationBuilder.processResultSetMappings(entityClass,
				List.of(rs), ctx);

		SqlResultSetMapping rsAnn = entityClass.getAnnotationUsage(
				SqlResultSetMapping.class, ctx.getModelsContext());
		assertNotNull(rsAnn);
		assertEquals("employeeEntity", rsAnn.name());
		assertEquals(1, rsAnn.entities().length);
		assertEquals(1, rsAnn.entities()[0].fields().length);
		assertEquals("name", rsAnn.entities()[0].fields()[0].name());
		assertEquals("emp_name", rsAnn.entities()[0].fields()[0].column());
	}

	@Test
	public void testMultipleResultSetMappings() {
		JaxbHbmResultSetMappingType rs1 = new JaxbHbmResultSetMappingType();
		rs1.setName("mapping1");
		JaxbHbmResultSetMappingType rs2 = new JaxbHbmResultSetMappingType();
		rs2.setName("mapping2");

		HbmEntityAnnotationBuilder.processResultSetMappings(entityClass,
				List.of(rs1, rs2), ctx);

		SqlResultSetMappings rssAnn = entityClass.getAnnotationUsage(
				SqlResultSetMappings.class, ctx.getModelsContext());
		assertNotNull(rssAnn);
		assertEquals(2, rssAnn.value().length);
		assertEquals("mapping1", rssAnn.value()[0].name());
		assertEquals("mapping2", rssAnn.value()[1].name());
	}

	@Test
	public void testEmptyResultSetMappings() {
		HbmEntityAnnotationBuilder.processResultSetMappings(entityClass,
				List.of(), ctx);

		assertNull(entityClass.getAnnotationUsage(
				SqlResultSetMapping.class, ctx.getModelsContext()));
	}
}
