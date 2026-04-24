/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.internal.exporter.entity.ImportContext;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DaoTemplateHelper}.
 *
 * @author Koen Aers
 */
public class DaoTemplateHelperTest {

	private DaoTemplateHelper create(TableDescriptor table) {
		return create(table, true);
	}

	private DaoTemplateHelper create(TableDescriptor table, boolean ejb3) {
		return create(table, ejb3, "SessionFactory");
	}

	private DaoTemplateHelper create(TableDescriptor table, boolean ejb3,
									String sessionFactoryName) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		return new DaoTemplateHelper(entity, new ImportContext(pkg),
				ejb3, sessionFactoryName);
	}

	private record TestContext(DaoTemplateHelper helper, ModelsContext modelsContext,
							ClassDetails classDetails) {}

	private TestContext createWithContext(TableDescriptor table) {
		return createWithContext(table, true);
	}

	private TestContext createWithContext(TableDescriptor table, boolean ejb3) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		DaoTemplateHelper helper = new DaoTemplateHelper(entity,
				new ImportContext(pkg), ejb3, "SessionFactory");
		return new TestContext(helper, builder.getModelsContext(), entity);
	}

	// --- Package / class ---

	@Test
	public void testGetPackageDeclaration() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("package com.example;", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetPackageDeclarationEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetDeclarationName() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("Employee", create(table).getDeclarationName());
	}

	@Test
	public void testGetQualifiedDeclarationName() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getQualifiedDeclarationName());
	}

	@Test
	public void testGetEntityName() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getEntityName());
	}

	// --- Mode ---

	@Test
	public void testIsEjb3True() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table, true).isEjb3());
	}

	@Test
	public void testIsEjb3False() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table, false).isEjb3());
	}

	@Test
	public void testGetSessionFactoryName() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("MySessionFactory", create(table, false, "MySessionFactory").getSessionFactoryName());
	}

	// --- Identifier ---

	@Test
	public void testHasIdentifier() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).hasIdentifier());
	}

	@Test
	public void testHasNoIdentifier() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		assertFalse(create(table).hasIdentifier());
	}

	@Test
	public void testGetIdTypeName() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertEquals("Long", create(table).getIdTypeName());
	}

	// --- Natural ID ---

	@Test
	public void testHasNaturalIdFalse() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).hasNaturalId());
	}

	@Test
	public void testHasNaturalIdTrue() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		for (var field : dc.getFields()) {
			if ("email".equals(field.getName())) {
				NaturalIdAnnotation nid = new NaturalIdAnnotation(ctx.modelsContext());
				((DynamicFieldDetails) field).addAnnotationUsage(nid);
			}
		}
		assertTrue(ctx.helper().hasNaturalId());
	}

	@Test
	public void testGetNaturalIdFields() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		for (var field : dc.getFields()) {
			if ("email".equals(field.getName())) {
				NaturalIdAnnotation nid = new NaturalIdAnnotation(ctx.modelsContext());
				((DynamicFieldDetails) field).addAnnotationUsage(nid);
			}
		}
		List<? extends org.hibernate.models.spi.FieldDetails> fields = ctx.helper().getNaturalIdFields();
		assertEquals(1, fields.size());
		assertEquals("email", fields.get(0).getName());
	}

	@Test
	public void testGetNaturalIdParameterList() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		table.addColumn(new ColumnDescriptor("CODE", "code", Integer.class));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		for (var field : dc.getFields()) {
			if ("email".equals(field.getName()) || "code".equals(field.getName())) {
				NaturalIdAnnotation nid = new NaturalIdAnnotation(ctx.modelsContext());
				((DynamicFieldDetails) field).addAnnotationUsage(nid);
			}
		}
		String params = ctx.helper().getNaturalIdParameterList();
		assertTrue(params.contains("String email"), params);
		assertTrue(params.contains("Integer code"), params);
		assertTrue(params.contains(", "), params);
	}

	// --- Named queries ---

	@Test
	public void testGetNamedQueriesEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).getNamedQueries().isEmpty());
	}

	@Test
	public void testGetNamedQueries() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(ctx.modelsContext());
		nq.name("com.example.Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		List<DaoTemplateHelper.NamedQueryInfo> queries = ctx.helper().getNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("com.example.Employee.findAll", queries.get(0).name());
		assertEquals("SELECT e FROM Employee e", queries.get(0).query());
	}

	@Test
	public void testGetEntityNamedQueriesMatching() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(ctx.modelsContext());
		nq.name("com.example.Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		List<DaoTemplateHelper.NamedQueryInfo> queries = ctx.helper().getEntityNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("com.example.Employee.findAll", queries.get(0).name());
	}

	@Test
	public void testGetEntityNamedQueriesNonMatching() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(ctx.modelsContext());
		nq.name("com.example.Other.findAll");
		nq.query("SELECT o FROM Other o");
		dc.addAnnotationUsage(nq);
		List<DaoTemplateHelper.NamedQueryInfo> queries = ctx.helper().getEntityNamedQueries();
		assertTrue(queries.isEmpty());
	}

	// --- Query parameters ---

	@Test
	public void testGetQueryParameterNamesEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query =
				new DaoTemplateHelper.NamedQueryInfo("findAll", "SELECT e FROM Employee e");
		assertTrue(helper.getQueryParameterNames(query).isEmpty());
	}

	@Test
	public void testGetQueryParameterNamesSingle() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query = new DaoTemplateHelper.NamedQueryInfo(
				"findByDept", "SELECT e FROM Employee e WHERE e.department = :dept");
		List<String> params = helper.getQueryParameterNames(query);
		assertEquals(1, params.size());
		assertEquals("dept", params.get(0));
	}

	@Test
	public void testGetQueryParameterNamesMultiple() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query = new DaoTemplateHelper.NamedQueryInfo(
				"findByDeptAndName",
				"SELECT e FROM Employee e WHERE e.department = :dept AND e.name = :name");
		List<String> params = helper.getQueryParameterNames(query);
		assertEquals(2, params.size());
		assertEquals("dept", params.get(0));
		assertEquals("name", params.get(1));
	}

	@Test
	public void testGetQueryParameterNamesDeduplicated() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query = new DaoTemplateHelper.NamedQueryInfo(
				"findByRange",
				"SELECT e FROM Employee e WHERE e.salary >= :minSalary AND e.salary <= :minSalary");
		List<String> params = helper.getQueryParameterNames(query);
		assertEquals(1, params.size());
		assertEquals("minSalary", params.get(0));
	}

	@Test
	public void testGetQueryParameterListEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query =
				new DaoTemplateHelper.NamedQueryInfo("findAll", "SELECT e FROM Employee e");
		assertEquals("", helper.getQueryParameterList(query));
	}

	@Test
	public void testGetQueryParameterListSingle() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query = new DaoTemplateHelper.NamedQueryInfo(
				"findByDept", "SELECT e FROM Employee e WHERE e.department = :dept");
		assertEquals("Object dept", helper.getQueryParameterList(query));
	}

	@Test
	public void testGetQueryParameterListMultiple() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		DaoTemplateHelper.NamedQueryInfo query = new DaoTemplateHelper.NamedQueryInfo(
				"findByDeptAndName",
				"SELECT e FROM Employee e WHERE e.department = :dept AND e.name = :name");
		assertEquals("Object dept, Object name", helper.getQueryParameterList(query));
	}

	// --- unqualify ---

	@Test
	public void testUnqualify() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		assertEquals("findAll", helper.unqualify("com.example.Employee.findAll"));
		assertEquals("count", helper.unqualify("count"));
	}
}
