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
package org.hibernate.tool.internal.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
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
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.InheritanceDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

class ClassAnnotationGeneratorTest {

	private record TestContext(
			ClassAnnotationGenerator generator,
			TemplateHelper helper,
			ModelsContext modelsContext,
			ClassDetails classDetails) {}

	private TestContext create(TableDescriptor table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		ImportContext importContext = new ImportContext(pkg);
		TemplateHelper helper = new TemplateHelper(classDetails, builder.getModelsContext(),
				importContext, true, Collections.emptyMap(), Collections.emptyMap());
		ClassAnnotationGenerator generator = new ClassAnnotationGenerator(
				classDetails, importContext, helper);
		return new TestContext(generator, helper, builder.getModelsContext(), classDetails);
	}

	// --- @Entity and @Table ---

	@Test
	void testEntityAndTable() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		String result = create(table).generator().generate();
		assertTrue(result.contains("@Entity"), result);
		assertTrue(result.contains("@Table(name = \"EMPLOYEE\")"), result);
	}

	@Test
	void testTableWithSchemaAndCatalog() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		String result = create(table).generator().generate();
		assertTrue(result.contains("schema = \"HR\""), result);
		assertTrue(result.contains("catalog = \"MYDB\""), result);
	}

	// --- @Inheritance ---

	@Test
	void testInheritanceSingleTable() {
		TableDescriptor table = new TableDescriptor("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER)
				.discriminatorColumnLength(10));
		String result = create(table).generator().generate();
		assertTrue(result.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), result);
		assertTrue(result.contains("@DiscriminatorColumn(name = \"DTYPE\""), result);
		assertTrue(result.contains("discriminatorType = DiscriminatorType.INTEGER"), result);
		assertTrue(result.contains("length = 10"), result);
	}

	@Test
	void testDiscriminatorValue() {
		TableDescriptor table = new TableDescriptor("CAR", "Car", "com.example");
		table.discriminatorValue("CAR");
		String result = create(table).generator().generate();
		assertTrue(result.contains("@DiscriminatorValue(\"CAR\")"), result);
	}

	@Test
	void testPrimaryKeyJoinColumn() {
		TableDescriptor table = new TableDescriptor("CAR", "Car", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String result = create(table).generator().generate();
		assertTrue(result.contains("@PrimaryKeyJoinColumn(name = \"VEHICLE_ID\")"), result);
	}

	// --- @SecondaryTable ---

	@Test
	void testSecondaryTable() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		PrimaryKeyJoinColumnJpaAnnotation pkjc =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx.modelsContext());
		pkjc.name("EMP_ID");
		SecondaryTableJpaAnnotation st =
				JpaAnnotations.SECONDARY_TABLE.createUsage(ctx.modelsContext());
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] { pkjc });
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(st);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@SecondaryTable(name = \"EMP_DETAIL\""), result);
		assertTrue(result.contains("@PrimaryKeyJoinColumn(name = \"EMP_ID\")"), result);
	}

	// --- Hibernate-specific annotations ---

	@Test
	void testImmutable() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		dc.addAnnotationUsage(HibernateAnnotations.IMMUTABLE.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generate();
		assertTrue(result.contains("@Immutable"), result);
	}

	@Test
	void testDynamicInsert() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		dc.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generate();
		assertTrue(result.contains("@DynamicInsert"), result);
	}

	@Test
	void testBatchSize() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx.modelsContext());
		bs.size(25);
		dc.addAnnotationUsage(bs);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@BatchSize(size = 25)"), result);
	}

	@Test
	void testCacheReadWrite() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		dc.addAnnotationUsage(cache);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)"), result);
	}

	@Test
	void testCacheWithRegion() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.READ_ONLY);
		cache.region("employees");
		dc.addAnnotationUsage(cache);
		String result = ctx.generator().generate();
		assertTrue(result.contains("region = \"employees\""), result);
	}

	@Test
	void testCacheNoneSkipped() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.NONE);
		dc.addAnnotationUsage(cache);
		String result = ctx.generator().generate();
		assertFalse(result.contains("@Cache"), result);
	}

	// --- @NamedQuery ---

	@Test
	void testNamedQuery() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx.modelsContext());
		nq.name("Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@NamedQuery(name = \"Employee.findAll\""), result);
		assertTrue(result.contains("query = \"SELECT e FROM Employee e\""), result);
	}

	@Test
	void testNamedNativeQuery() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		NamedNativeQueryJpaAnnotation nnq =
				JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx.modelsContext());
		nnq.name("Employee.findByDept");
		nnq.query("SELECT * FROM EMPLOYEE WHERE DEPT = ?");
		dc.addAnnotationUsage(nnq);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@NamedNativeQuery(name = \"Employee.findByDept\""), result);
	}

	// --- @Filter / @FilterDef ---

	@Test
	void testFilterSimple() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeOnly");
		filter.condition("active = true");
		dc.addAnnotationUsage(filter);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@Filter(name = \"activeOnly\""), result);
		assertTrue(result.contains("condition = \"active = true\""), result);
	}

	@Test
	void testFilterDefWithParams() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		ParamDefAnnotation pd = HibernateAnnotations.PARAM_DEF.createUsage(ctx.modelsContext());
		pd.name("deptId");
		pd.type(Long.class);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx.modelsContext());
		fd.name("byDept");
		fd.defaultCondition("dept_id = :deptId");
		fd.parameters(new org.hibernate.annotations.ParamDef[] { pd });
		dc.addAnnotationUsage(fd);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@FilterDef(name = \"byDept\""), result);
		assertTrue(result.contains("defaultCondition = \"dept_id = :deptId\""), result);
		assertTrue(result.contains("@ParamDef(name = \"deptId\""), result);
	}

	// --- SQL DML ---

	@Test
	void testSQLInsert() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx.modelsContext());
		si.sql("INSERT INTO EMPLOYEE (name) VALUES (?)");
		dc.addAnnotationUsage(si);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@SQLInsert(sql = \"INSERT INTO EMPLOYEE (name) VALUES (?)\""), result);
	}

	@Test
	void testSQLUpdate() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx.modelsContext());
		su.sql("UPDATE EMPLOYEE SET name = ? WHERE id = ?");
		dc.addAnnotationUsage(su);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@SQLUpdate"), result);
	}

	@Test
	void testSQLDelete() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx.modelsContext());
		sd.sql("DELETE FROM EMPLOYEE WHERE id = ?");
		dc.addAnnotationUsage(sd);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@SQLDelete"), result);
	}

	// --- @EntityListeners ---

	@Test
	void testEntityListenersSingle() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		EntityListenersJpaAnnotation el =
				JpaAnnotations.ENTITY_LISTENERS.createUsage(ctx.modelsContext());
		el.value(new Class<?>[] { String.class });
		dc.addAnnotationUsage(el);
		String result = ctx.generator().generate();
		assertTrue(result.contains("@EntityListeners(String.class)"), result);
	}

	// --- @Embeddable ---

	@Test
	void testEmbeddableReturnsEarly() {
		TestContext ctx = create(new TableDescriptor("ADDRESS", "Address", "com.example"));
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		dc.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generate();
		assertTrue(result.contains("@Embeddable"), result);
		assertFalse(result.contains("@Entity"), result);
		assertFalse(result.contains("@Table"), result);
	}

	// --- generateTableAnnotation ---

	@Test
	void testGenerateTableAnnotation() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		jakarta.persistence.Table table = ctx.classDetails()
				.getDirectAnnotationUsage(jakarta.persistence.Table.class);
		String result = ctx.generator().generateTableAnnotation(table);
		assertTrue(result.contains("@Table(name = \"EMPLOYEE\")"), result);
	}

	// --- generateInheritanceAnnotation ---

	@Test
	void testGenerateInheritanceAnnotation() {
		TableDescriptor table = new TableDescriptor("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceDescriptor(InheritanceType.JOINED));
		TestContext ctx = create(table);
		jakarta.persistence.Inheritance inh = ctx.classDetails()
				.getDirectAnnotationUsage(jakarta.persistence.Inheritance.class);
		String result = ctx.generator().generateInheritanceAnnotation(inh);
		assertTrue(result.contains("@Inheritance(strategy = InheritanceType.JOINED)"), result);
	}

	// --- formatCheckConstraint / formatIndex / formatUniqueConstraint ---

	@Test
	void testFormatIndex() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		jakarta.persistence.Index idx = new jakarta.persistence.Index() {
			public String name() { return "idx_name"; }
			public String columnList() { return "name"; }
			public boolean unique() { return true; }
			public String options() { return ""; }
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return jakarta.persistence.Index.class;
			}
		};
		String result = ctx.generator().formatIndex(idx);
		assertTrue(result.contains("name = \"idx_name\""), result);
		assertTrue(result.contains("columnList = \"name\""), result);
		assertTrue(result.contains("unique = true"), result);
	}

	@Test
	void testFormatUniqueConstraint() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		jakarta.persistence.UniqueConstraint uc = new jakarta.persistence.UniqueConstraint() {
			public String name() { return "uk_email"; }
			public String[] columnNames() { return new String[] { "email" }; }
			public String options() { return ""; }
			public Class<? extends java.lang.annotation.Annotation> annotationType() {
				return jakarta.persistence.UniqueConstraint.class;
			}
		};
		String result = ctx.generator().formatUniqueConstraint(uc);
		assertTrue(result.contains("name = \"uk_email\""), result);
		assertTrue(result.contains("\"email\""), result);
	}
}
