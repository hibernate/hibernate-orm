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

import java.io.StringWriter;
import java.util.List;

import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DaoExporter}.
 *
 * @author Koen Aers
 */
public class DaoExporterTest {

	private String export(TableMetadata table) {
		return export(table, true);
	}

	private String export(TableMetadata table, boolean ejb3) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), ejb3);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		return writer.toString();
	}

	// --- EJB3 mode tests ---

	@Test
	public void testGeneratedHeader() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.matches("(?s).*// Generated .+ by Hibernate Tools .+\n.*"), source);
	}

	@Test
	public void testEjb3PackageDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("package com.example;"), source);
	}

	@Test
	public void testEjb3StatelessAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("@Stateless"), source);
	}

	@Test
	public void testEjb3ClassName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public class EmployeeHome {"), source);
	}

	@Test
	public void testEjb3PersistenceContext() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("@PersistenceContext"), source);
		assertTrue(source.contains("EntityManager entityManager"), source);
	}

	@Test
	public void testEjb3Persist() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public void persist(Employee transientInstance)"), source);
		assertTrue(source.contains("entityManager.persist(transientInstance)"), source);
	}

	@Test
	public void testEjb3Remove() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public void remove(Employee persistentInstance)"), source);
		assertTrue(source.contains("entityManager.remove(persistentInstance)"), source);
	}

	@Test
	public void testEjb3Merge() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public Employee merge(Employee detachedInstance)"), source);
		assertTrue(source.contains("entityManager.merge(detachedInstance)"), source);
	}

	@Test
	public void testEjb3FindById() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public Employee findById( Long id)"), source);
		assertTrue(source.contains("entityManager.find(Employee.class, id)"), source);
	}

	@Test
	public void testEjb3NoFindByIdWithoutIdentifier() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertFalse(source.contains("findById"), source);
	}

	// --- Hibernate Classic mode tests ---

	@Test
	public void testClassicNoStatelessAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertFalse(source.contains("@Stateless"), source);
	}

	@Test
	public void testClassicSessionFactory() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("SessionFactory sessionFactory"), source);
		assertTrue(source.contains("lookup(\"SessionFactory\")"), source);
	}

	@Test
	public void testClassicPersist() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("sessionFactory.getCurrentSession().persist(transientInstance)"), source);
	}

	@Test
	public void testClassicAttachDirty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public void attachDirty(Employee instance)"), source);
		assertTrue(source.contains("sessionFactory.getCurrentSession().merge(instance)"), source);
	}

	@Test
	public void testClassicAttachClean() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public void attachClean(Employee instance)"), source);
		assertTrue(source.contains("LockMode.NONE"), source);
	}

	@Test
	public void testClassicRemove() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("sessionFactory.getCurrentSession().remove(persistentInstance)"), source);
	}

	@Test
	public void testClassicMerge() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("sessionFactory.getCurrentSession()"), source);
		assertTrue(source.contains(".merge(detachedInstance)"), source);
	}

	@Test
	public void testClassicFindById() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public Employee findById( Long id)"), source);
		assertTrue(source.contains(".get(\"com.example.Employee\", id)"), source);
	}

	@Test
	public void testClassicFindByNaturalId() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		for (var field : dc.getFields()) {
			if ("email".equals(field.getName())) {
				NaturalIdAnnotation nid = new NaturalIdAnnotation(builder.getModelsContext());
				((DynamicFieldDetails) field).addAnnotationUsage(nid);
			}
		}
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), false);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("public Employee findByNaturalId(String email)"), source);
		assertTrue(source.contains("criteriaBuilder.equal(root.get(\"email\"), email)"), source);
	}

	@Test
	public void testClassicNamedQueryMethod() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(builder.getModelsContext());
		nq.name("com.example.Employee.findByDepartment");
		nq.query("SELECT e FROM Employee e WHERE e.department = :dept");
		dc.addAnnotationUsage(nq);
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), false);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("public List<Employee> findByDepartment(Object dept)"), source);
		assertTrue(source.contains("createNamedQuery(\"com.example.Employee.findByDepartment\")"), source);
		assertTrue(source.contains("query.setParameter(\"dept\", dept)"), source);
	}

	@Test
	public void testClassicCountQueryMethod() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(builder.getModelsContext());
		nq.name("com.example.Employee.countByDepartment");
		nq.query("SELECT COUNT(e) FROM Employee e WHERE e.department = :dept");
		dc.addAnnotationUsage(nq);
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), false);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("public int countByDepartment(Object dept)"), source);
		assertTrue(source.contains("query.uniqueResult()"), source);
		assertTrue(source.contains("query.setParameter(\"dept\", dept)"), source);
	}

	@Test
	public void testClassicNamedQueryNoParams() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(builder.getModelsContext());
		nq.name("com.example.Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), false);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("public List<Employee> findAll()"), source);
		assertFalse(source.contains("setParameter"), source);
	}

	@Test
	public void testClassicNamedQueryMultipleParams() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		NamedQueryJpaAnnotation nq = new NamedQueryJpaAnnotation(builder.getModelsContext());
		nq.name("com.example.Employee.findByDeptAndName");
		nq.query("SELECT e FROM Employee e WHERE e.department = :dept AND e.name = :name");
		dc.addAnnotationUsage(nq);
		DaoExporter exporter = DaoExporter.create(
				List.of(entity), builder.getModelsContext(), false);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("findByDeptAndName(Object dept, Object name)"), source);
		assertTrue(source.contains("query.setParameter(\"dept\", dept)"), source);
		assertTrue(source.contains("query.setParameter(\"name\", name)"), source);
	}

	// --- Batch operations (EJB3) ---

	@Test
	public void testEjb3PersistAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public void persistAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("entityManager.persist(entities.get(i))"), source);
		assertTrue(source.contains("entityManager.flush()"), source);
		assertTrue(source.contains("entityManager.clear()"), source);
	}

	@Test
	public void testEjb3MergeAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public List<Employee> mergeAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("entityManager.merge(entities.get(i))"), source);
	}

	@Test
	public void testEjb3RemoveAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public void removeAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("entityManager.remove("), source);
	}

	// --- Batch operations (Classic) ---

	@Test
	public void testClassicPersistAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public void persistAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("session.persist(entities.get(i))"), source);
		assertTrue(source.contains("session.flush()"), source);
		assertTrue(source.contains("session.clear()"), source);
	}

	@Test
	public void testClassicMergeAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public List<Employee> mergeAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("session.merge(entities.get(i))"), source);
	}

	@Test
	public void testClassicRemoveAll() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public void removeAll(List<Employee> entities, int batchSize)"), source);
		assertTrue(source.contains("session.remove("), source);
	}

	// --- Pagination (EJB3) ---

	@Test
	public void testEjb3FindAllPaginated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public List<Employee> findAll(int firstResult, int maxResults)"), source);
		assertTrue(source.contains("entityManager.getCriteriaBuilder()"), source);
		assertTrue(source.contains("criteriaBuilder.createQuery(Employee.class)"), source);
		assertTrue(source.contains(".setFirstResult(firstResult)"), source);
		assertTrue(source.contains(".setMaxResults(maxResults)"), source);
		assertTrue(source.contains(".getResultList()"), source);
	}

	// --- Pagination (Classic) ---

	@Test
	public void testClassicFindAllPaginated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("public List<Employee> findAll(int firstResult, int maxResults)"), source);
		assertTrue(source.contains("sessionFactory.getCriteriaBuilder()"), source);
		assertTrue(source.contains("criteriaBuilder.createQuery(Employee.class)"), source);
		assertTrue(source.contains(".setFirstResult(firstResult)"), source);
		assertTrue(source.contains(".setMaxResults(maxResults)"), source);
		assertTrue(source.contains(".getResultList()"), source);
	}

	// --- Import tests ---

	@Test
	public void testEjb3ImportsGenerated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("import jakarta.ejb.Stateless;"), source);
		assertTrue(source.contains("import jakarta.persistence.EntityManager;"), source);
		assertTrue(source.contains("import jakarta.persistence.PersistenceContext;"), source);
		assertTrue(source.contains("import java.util.logging.Logger;"), source);
		assertTrue(source.contains("import java.util.logging.Level;"), source);
	}

	@Test
	public void testClassicImportsGenerated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertTrue(source.contains("import org.hibernate.SessionFactory;"), source);
		assertTrue(source.contains("import org.hibernate.LockMode;"), source);
		assertFalse(source.contains("import jakarta.ejb.Stateless;"), source);
	}

	// --- Logger ---

	@Test
	public void testLoggerField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("Logger.getLogger(EmployeeHome.class.getName())"), source);
	}
}
