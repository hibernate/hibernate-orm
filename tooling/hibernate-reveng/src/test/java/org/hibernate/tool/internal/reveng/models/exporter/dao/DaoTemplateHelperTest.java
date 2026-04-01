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
package org.hibernate.tool.internal.reveng.models.exporter.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.export.java.ImportContextImpl;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DaoTemplateHelper}.
 *
 * @author Koen Aers
 */
public class DaoTemplateHelperTest {

	private DaoTemplateHelper create(TableMetadata table) {
		return create(table, true);
	}

	private DaoTemplateHelper create(TableMetadata table, boolean ejb3) {
		return create(table, ejb3, "SessionFactory");
	}

	private DaoTemplateHelper create(TableMetadata table, boolean ejb3,
									 String sessionFactoryName) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		return new DaoTemplateHelper(entity, new ImportContextImpl(pkg),
				ejb3, sessionFactoryName);
	}

	private record TestContext(DaoTemplateHelper helper, ModelsContext modelsContext,
							   ClassDetails classDetails) {}

	private TestContext createWithContext(TableMetadata table) {
		return createWithContext(table, true);
	}

	private TestContext createWithContext(TableMetadata table, boolean ejb3) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		DaoTemplateHelper helper = new DaoTemplateHelper(entity,
				new ImportContextImpl(pkg), ejb3, "SessionFactory");
		return new TestContext(helper, builder.getModelsContext(), entity);
	}

	// --- Package / class ---

	@Test
	public void testGetPackageDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("package com.example;", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetPackageDeclarationEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetDeclarationName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("Employee", create(table).getDeclarationName());
	}

	@Test
	public void testGetQualifiedDeclarationName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getQualifiedDeclarationName());
	}

	@Test
	public void testGetEntityName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getEntityName());
	}

	// --- Mode ---

	@Test
	public void testIsEjb3True() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table, true).isEjb3());
	}

	@Test
	public void testIsEjb3False() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table, false).isEjb3());
	}

	@Test
	public void testGetSessionFactoryName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("MySessionFactory", create(table, false, "MySessionFactory").getSessionFactoryName());
	}

	// --- Identifier ---

	@Test
	public void testHasIdentifier() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).hasIdentifier());
	}

	@Test
	public void testHasNoIdentifier() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		assertFalse(create(table).hasIdentifier());
	}

	@Test
	public void testGetIdTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("Long", create(table).getIdTypeName());
	}

	// --- Natural ID ---

	@Test
	public void testHasNaturalIdFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).hasNaturalId());
	}

	@Test
	public void testHasNaturalIdTrue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
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
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
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
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		table.addColumn(new ColumnMetadata("CODE", "code", Integer.class));
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
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).getNamedQueries().isEmpty());
	}

	@Test
	public void testGetNamedQueries() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
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
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
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
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(ctx.modelsContext());
		nq.name("com.example.Other.findAll");
		nq.query("SELECT o FROM Other o");
		dc.addAnnotationUsage(nq);
		List<DaoTemplateHelper.NamedQueryInfo> queries = ctx.helper().getEntityNamedQueries();
		assertTrue(queries.isEmpty());
	}

	// --- unqualify ---

	@Test
	public void testUnqualify() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DaoTemplateHelper helper = create(table);
		assertEquals("findAll", helper.unqualify("com.example.Employee.findAll"));
		assertEquals("count", helper.unqualify("count"));
	}
}
