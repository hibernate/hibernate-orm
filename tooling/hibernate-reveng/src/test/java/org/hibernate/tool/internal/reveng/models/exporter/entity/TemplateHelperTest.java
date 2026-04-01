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
package org.hibernate.tool.internal.reveng.models.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionIdAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.NotFoundAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.internal.jdk.JdkMethodDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.export.java.ImportContextImpl;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.builder.EmbeddableClassBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TemplateHelper}.
 *
 * @author Koen Aers
 */
public class TemplateHelperTest {

	private TemplateHelper create(TableMetadata table) {
		return create(table, true);
	}

	private TemplateHelper create(TableMetadata table, boolean annotated) {
		return create(table, annotated, Collections.emptyMap(), Collections.emptyMap());
	}

	private TemplateHelper create(TableMetadata table, boolean annotated,
								  Map<String, List<String>> classMetaAttributes,
								  Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		return new TemplateHelper(classDetails, builder.getModelsContext(),
				new ImportContextImpl(pkg), annotated,
				classMetaAttributes, fieldMetaAttributes);
	}

	private Map<String, Map<String, List<String>>> fieldMeta(String fieldName, String key, String value) {
		Map<String, Map<String, List<String>>> result = new HashMap<>();
		result.put(fieldName, Map.of(key, List.of(value)));
		return result;
	}

	// --- Package / class ---

	@Test
	public void testGetPackageDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("package com.example;", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetPackageDeclarationEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		assertEquals("", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetDeclarationName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("Employee", create(table).getDeclarationName());
	}

	@Test
	public void testGetExtendsDeclarationNoParent() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table).getExtendsDeclaration());
	}

	@Test
	public void testGetExtendsDeclarationWithParent() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertEquals("extends Vehicle ", create(table).getExtendsDeclaration());
	}

	@Test
	public void testGetImplementsDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("implements Serializable", create(table).getImplementsDeclaration());
	}

	// --- Field categorization ---

	@Test
	public void testGetBasicFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		List<FieldDetails> fields = helper.getBasicFields();
		assertEquals(2, fields.size());
		assertEquals("id", fields.get(0).getName());
		assertEquals("name", fields.get(1).getName());
	}

	@Test
	public void testGetManyToOneFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		List<FieldDetails> m2oFields = helper.getManyToOneFields();
		assertEquals(1, m2oFields.size());
		assertEquals("department", m2oFields.get(0).getName());
	}

	@Test
	public void testGetOneToManyFields() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		List<FieldDetails> o2mFields = helper.getOneToManyFields();
		assertEquals(1, o2mFields.size());
		assertEquals("employees", o2mFields.get(0).getName());
	}

	@Test
	public void testGetCompositeIdField() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		TemplateHelper helper = create(table);
		FieldDetails cid = helper.getCompositeIdField();
		assertNotNull(cid);
		assertEquals("id", cid.getName());
	}

	// --- Type name resolution ---

	@Test
	public void testGetJavaTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertEquals("String", helper.getJavaTypeName(field));
	}

	@Test
	public void testGetJavaTypeNameBigDecimal() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("BigDecimal", helper.getJavaTypeName(field));
		assertTrue(helper.generateImports().contains("import java.math.BigDecimal;"));
	}

	@Test
	public void testGetCollectionTypeNameOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("Set<Employee>", helper.getCollectionTypeName(field));
		assertTrue(helper.generateImports().contains("import java.util.Set;"));
	}

	// --- Getter/setter names ---

	@Test
	public void testGetterName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("getName", create(table).getGetterName("name"));
	}

	@Test
	public void testSetterName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("setName", create(table).getSetterName("name"));
	}

	@Test
	public void testGetterNameSingleChar() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("getX", create(table).getGetterName("x"));
	}

	// --- Annotation generation (annotated=true) ---

	@Test
	public void testGenerateClassAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@Entity"), result);
		assertTrue(result.contains("@Table(name = \"EMPLOYEE\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithSchemaAndCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("schema = \"HR\""), result);
		assertTrue(result.contains("catalog = \"MYDB\""), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithInheritance() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER)
				.discriminatorColumnLength(10));
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), result);
		assertTrue(result.contains("@DiscriminatorColumn(name = \"DTYPE\""), result);
		assertTrue(result.contains("discriminatorType = DiscriminatorType.INTEGER"), result);
		assertTrue(result.contains("length = 10"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithDiscriminatorValue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.discriminatorValue("CAR");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@DiscriminatorValue(\"CAR\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithPrimaryKeyJoinColumn() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@PrimaryKeyJoinColumn(name = \"VEHICLE_ID\")"), result);
	}

	// --- @SecondaryTable ---

	@Test
	public void testGenerateClassAnnotationsSecondaryTable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx.modelsContext());
		pkjc.name("EMP_ID");
		SecondaryTableJpaAnnotation st = JpaAnnotations.SECONDARY_TABLE.createUsage(ctx.modelsContext());
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] { pkjc });
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(st);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SecondaryTable(name = \"EMP_DETAIL\""), result);
		assertTrue(result.contains("@PrimaryKeyJoinColumn(name = \"EMP_ID\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsSecondaryTableNoPkJoinColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		SecondaryTableJpaAnnotation st = JpaAnnotations.SECONDARY_TABLE.createUsage(ctx.modelsContext());
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] {});
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(st);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SecondaryTable(name = \"EMP_DETAIL\")"), result);
		assertFalse(result.contains("PrimaryKeyJoinColumn"), result);
	}

	@Test
	public void testGetSecondaryTablesNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table);
		assertTrue(helper.getSecondaryTables().isEmpty());
	}

	@Test
	public void testGenerateIdAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		String result = helper.generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertFalse(result.contains("@GeneratedValue"), result);
	}

	@Test
	public void testGenerateIdAnnotationsWithGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		String result = helper.generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertTrue(result.contains("@GeneratedValue(strategy = GenerationType.IDENTITY)"), result);
	}

	@Test
	public void testGenerateVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("@Version", create(table).generateVersionAnnotation());
	}

	@Test
	public void testGenerateColumnAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		String result = helper.generateColumnAnnotation(field);
		assertEquals("@Column(name = \"NAME\")", result);
	}

	@Test
	public void testGenerateColumnAnnotationWithAttributes() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateColumnAnnotation(field);
		assertTrue(result.contains("nullable = false"), result);
		assertTrue(result.contains("unique = true"), result);
		assertTrue(result.contains("length = 100"), result);
	}

	@Test
	public void testGenerateColumnAnnotationWithPrecisionScale() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateColumnAnnotation(field);
		assertTrue(result.contains("precision = 10"), result);
		assertTrue(result.contains("scale = 2"), result);
	}

	@Test
	public void testGenerateBasicAnnotationNoAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateBasicAnnotation(field));
	}

	@Test
	public void testGenerateTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("@Temporal(TemporalType.TIMESTAMP)", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testGenerateTemporalAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testGenerateLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("@Lob", create(table).generateLobAnnotation());
	}

	@Test
	public void testGenerateManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("@ManyToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"DEPT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithFetchAndOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY)
				.optional(false));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.LAZY"), result);
		assertTrue(result.contains("optional = false"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithReferencedColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_CODE", "deptCode", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("referencedColumnName = \"CODE\""), result);
	}

	@Test
	public void testGenerateOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("@OneToMany(mappedBy = \"department\")",
				helper.generateOneToManyAnnotation(field));
	}

	@Test
	public void testGenerateOneToManyAnnotationWithFetchAndCascade() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER)
				.cascade(CascadeType.ALL)
				.orphanRemoval(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		String result = helper.generateOneToManyAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.EAGER"), result);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("@OneToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"ADDRESS_ID\")"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("mappedBy = \"address\""), result);
		assertFalse(result.contains("@JoinColumn"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationWithCascadeAndOrphanRemoval() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.cascade(CascadeType.ALL)
				.orphanRemoval(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("@ManyToMany"), result);
		assertTrue(result.contains("@JoinTable(name = \"EMPLOYEE_PROJECT\""), result);
		assertTrue(result.contains("joinColumns = @JoinColumn(name = \"EMPLOYEE_ID\")"), result);
		assertTrue(result.contains("inverseJoinColumns = @JoinColumn(name = \"PROJECT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("mappedBy = \"projects\""), result);
		assertFalse(result.contains("@JoinTable"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationWithMultipleCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("cascade = { CascadeType.PERSIST, CascadeType.MERGE }"), result);
	}

	@Test
	public void testGenerateEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getCompositeIdField();
		String result = helper.generateEmbeddedIdAnnotation(field);
		assertTrue(result.contains("@EmbeddedId"), result);
		assertTrue(result.contains("@AttributeOverrides"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"orderId\", column = @Column(name = \"ORDER_ID\"))"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"lineNumber\", column = @Column(name = \"LINE_NUMBER\"))"), result);
	}

	@Test
	public void testGenerateEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getEmbeddedFields().get(0);
		String result = helper.generateEmbeddedAnnotation(field);
		assertTrue(result.contains("@Embedded"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"street\", column = @Column(name = \"HOME_STREET\"))"), result);
	}

	// --- Annotation generation (annotated=false) ---

	@Test
	public void testUnannotatedClassAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateClassAnnotations());
	}

	@Test
	public void testUnannotatedIdAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateIdAnnotations(field));
	}

	@Test
	public void testUnannotatedVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateVersionAnnotation());
	}

	@Test
	public void testUnannotatedColumnAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateColumnAnnotation(field));
	}

	@Test
	public void testUnannotatedBasicAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateBasicAnnotation(field));
	}

	@Test
	public void testUnannotatedTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testUnannotatedLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateLobAnnotation());
	}

	@Test
	public void testUnannotatedManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getManyToOneFields().get(0);
		assertEquals("", helper.generateManyToOneAnnotation(field));
	}

	@Test
	public void testUnannotatedOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateOneToManyAnnotation(field));
	}

	@Test
	public void testUnannotatedOneToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getOneToOneFields().get(0);
		assertEquals("", helper.generateOneToOneAnnotation(field));
	}

	@Test
	public void testUnannotatedManyToManyAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getManyToManyFields().get(0);
		assertEquals("", helper.generateManyToManyAnnotation(field));
	}

	@Test
	public void testUnannotatedEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getCompositeIdField();
		assertEquals("", helper.generateEmbeddedIdAnnotation(field));
	}

	@Test
	public void testUnannotatedEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("address", "Address", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getEmbeddedFields().get(0);
		assertEquals("", helper.generateEmbeddedAnnotation(field));
	}

	// --- Subclass check ---

	@Test
	public void testIsSubclassFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).isSubclass());
	}

	@Test
	public void testIsSubclassTrue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertTrue(create(table).isSubclass());
	}

	// --- Constructor support ---

	@Test
	public void testNeedsFullConstructorWithColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		assertTrue(create(table).needsFullConstructor());
	}

	@Test
	public void testFullConstructorPropertiesBasicColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(2, props.size());
		assertEquals("Long", props.get(0).typeName());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("String", props.get(1).typeName());
		assertEquals("name", props.get(1).fieldName());
	}

	@Test
	public void testFullConstructorSkipsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorSkipsGenPropertyFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		List<TemplateHelper.FullConstructorProperty> props = helper.getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorIncludesCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("OrderLineId", props.get(0).typeName());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorIncludesRelationships() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example"));
		table.addOneToMany(new OneToManyMetadata("projects", "employee", "Project", "com.example"));
		table.addManyToMany(new ManyToManyMetadata("skills", "Skill", "com.example"));
		table.addEmbeddedField(new EmbeddedFieldMetadata("info", "Info", "com.example"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(5, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("address", props.get(1).fieldName());
		assertEquals("projects", props.get(2).fieldName());
		assertEquals("skills", props.get(3).fieldName());
		assertEquals("info", props.get(4).fieldName());
	}

	@Test
	public void testGetFullConstructorParameterList() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		assertEquals("Long id, String name", create(table).getFullConstructorParameterList());
	}

	// --- toString support ---

	@Test
	public void testToStringPropertiesDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
		assertEquals(2, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("getId", props.get(0).getterName());
		assertEquals("name", props.get(1).fieldName());
		assertEquals("getName", props.get(1).getterName());
	}

	@Test
	public void testToStringPropertiesExplicitUseInToString() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("SECRET", "secret", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "use-in-tostring", "true"));
		List<TemplateHelper.ToStringProperty> props = helper.getToStringProperties();
		assertEquals(1, props.size());
		assertEquals("name", props.get(0).fieldName());
	}

	@Test
	public void testToStringPropertiesWithCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	// --- equals/hashCode support ---

	@Test
	public void testHasCompositeIdFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).hasCompositeId());
	}

	@Test
	public void testHasCompositeIdTrue() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example"));
		assertTrue(create(table).hasCompositeId());
	}

	@Test
	public void testNeedsEqualsHashCodeWithPrimaryKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).needsEqualsHashCode());
	}

	@Test
	public void testNeedsEqualsHashCodeWithExplicitEquals() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("email", "use-in-equals", "true"));
		assertTrue(helper.needsEqualsHashCode());
	}

	@Test
	public void testHasExplicitEqualsColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("email", "use-in-equals", "true"));
		assertTrue(helper.hasExplicitEqualsColumns());
		List<FieldDetails> equalsFields = helper.getEqualsFields();
		assertEquals(1, equalsFields.size());
		assertEquals("email", equalsFields.get(0).getName());
	}

	@Test
	public void testGetIdentifierFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<FieldDetails> idFields = create(table).getIdentifierFields();
		assertEquals(1, idFields.size());
		assertEquals("id", idFields.get(0).getName());
	}

	@Test
	public void testGenerateEqualsExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateEqualsExpression(field);
		assertTrue(result.contains("this.getName()"), result);
		assertTrue(result.contains("other.getName()"), result);
		assertTrue(result.contains(".equals("), result);
	}

	@Test
	public void testGenerateHashCodeExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateHashCodeExpression(field);
		assertEquals("(this.getName() == null ? 0 : this.getName().hashCode())", result);
	}

	// --- Meta-attribute support ---

	@Test
	public void testHasClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("// custom")),
				Collections.emptyMap());
		assertTrue(helper.hasClassMetaAttribute("class-code"));
		assertFalse(helper.hasClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testGetClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("public void custom() {}")),
				Collections.emptyMap());
		assertEquals("public void custom() {}", helper.getClassMetaAttribute("class-code"));
	}

	@Test
	public void testGetClassMetaAttributeEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table).getClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testHasFieldMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The name"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.hasFieldMetaAttribute(field, "field-description"));
		assertFalse(helper.hasFieldMetaAttribute(field, "nonexistent"));
	}

	@Test
	public void testGetFieldMetaAsBool() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertFalse(helper.getFieldMetaAsBool(field, "gen-property", true));
	}

	@Test
	public void testGetFieldMetaAsBoolDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.getFieldMetaAsBool(field, "gen-property", true));
	}

	@Test
	public void testIsGenProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		List<FieldDetails> fields = helper.getBasicFields();
		FieldDetails nameField = fields.stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		FieldDetails internalField = fields.stream()
				.filter(f -> f.getName().equals("internal")).findFirst().orElseThrow();
		assertTrue(helper.isGenProperty(nameField));
		assertFalse(helper.isGenProperty(internalField));
	}

	@Test
	public void testHasFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The employee name"));
		FieldDetails nameField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		FieldDetails idField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		assertTrue(helper.hasFieldDescription(nameField));
		assertFalse(helper.hasFieldDescription(idField));
	}

	@Test
	public void testGetFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The employee name"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("The employee name", helper.getFieldDescription(field));
	}

	@Test
	public void testHasExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).hasExtraClassCode());
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("// extra")),
				Collections.emptyMap());
		assertTrue(helper.hasExtraClassCode());
	}

	@Test
	public void testGetExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("public void customMethod() {}")),
				Collections.emptyMap());
		assertEquals("public void customMethod() {}", helper.getExtraClassCode());
	}

	// --- Field info methods ---

	@Test
	public void testIsPrimaryKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails idField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		FieldDetails nameField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertTrue(helper.isPrimaryKey(idField));
		assertFalse(helper.isPrimaryKey(nameField));
	}

	@Test
	public void testIsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		TemplateHelper helper = create(table);
		FieldDetails versionField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("version")).findFirst().orElseThrow();
		assertTrue(helper.isVersion(versionField));
	}

	@Test
	public void testIsLob() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.isLob(field));
	}

	// --- Boolean getter prefix ---

	@Test
	public void testGetterNameBooleanField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", boolean.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		assertEquals("isActive", helper.getGetterName(field));
	}

	@Test
	public void testGetterNameNonBooleanField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertEquals("getName", helper.getGetterName(field));
	}

	@Test
	public void testGetterNameBooleanWrapperUsesGet() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", Boolean.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		assertEquals("getActive", helper.getGetterName(field));
	}

	// --- @Column insertable/updatable ---

	@Test
	public void testColumnAnnotationInsertableFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("COMPUTED", "computed", String.class);
		col.insertable(false);
		table.addColumn(col);
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String ann = helper.generateColumnAnnotation(field);
		assertTrue(ann.contains("insertable = false"), ann);
	}

	@Test
	public void testColumnAnnotationUpdatableFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("COMPUTED", "computed", String.class);
		col.updatable(false);
		table.addColumn(col);
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String ann = helper.generateColumnAnnotation(field);
		assertTrue(ann.contains("updatable = false"), ann);
	}

	// --- Embeddable support ---

	private TemplateHelper createEmbeddable(EmbeddableMetadata metadata) {
		ModelsContext ctx = new BasicModelsContextImpl(
				SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		ClassDetails classDetails = EmbeddableClassBuilder.buildEmbeddableClass(metadata, ctx);
		return new TemplateHelper(classDetails, ctx,
				new ImportContextImpl(metadata.getPackageName()), true,
				Collections.emptyMap(), Collections.emptyMap());
	}

	@Test
	public void testEmbeddableIsEmbeddable() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertTrue(helper.isEmbeddable());
	}

	@Test
	public void testEmbeddableClassAnnotations() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		TemplateHelper helper = createEmbeddable(metadata);
		String result = helper.generateClassAnnotations();
		assertTrue(result.contains("@Embeddable"), result);
		assertFalse(result.contains("@Entity"), result);
		assertFalse(result.contains("@Table"), result);
	}

	@Test
	public void testEmbeddableNeedsEqualsHashCode() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertTrue(helper.needsEqualsHashCode());
	}

	@Test
	public void testEmbeddableIdentifierFieldsReturnsAllFields() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		TemplateHelper helper = createEmbeddable(metadata);
		List<FieldDetails> idFields = helper.getIdentifierFields();
		assertEquals(2, idFields.size());
		assertEquals("orderId", idFields.get(0).getName());
		assertEquals("lineNumber", idFields.get(1).getName());
	}

	@Test
	public void testEmbeddableBasicFields() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("Address", "com.example")
				.addColumn(new ColumnMetadata("STREET", "street", String.class))
				.addColumn(new ColumnMetadata("CITY", "city", String.class))
				.addColumn(new ColumnMetadata("ZIP", "zip", String.class));
		TemplateHelper helper = createEmbeddable(metadata);
		List<FieldDetails> fields = helper.getBasicFields();
		assertEquals(3, fields.size());
	}

	@Test
	public void testEmbeddableDeclarationName() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertEquals("OrderLineId", helper.getDeclarationName());
	}

	@Test
	public void testEmbeddablePackageDeclaration() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertEquals("package com.example;", helper.getPackageDeclaration());
	}

	@Test
	public void testEmbeddableNotSubclass() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertFalse(helper.isSubclass());
	}

	@Test
	public void testEmbeddableFullConstructor() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		TemplateHelper helper = createEmbeddable(metadata);
		assertTrue(helper.needsFullConstructor());
		assertEquals("Long orderId, Integer lineNumber",
				helper.getFullConstructorParameterList());
	}

	@Test
	public void testEntityIsNotEmbeddable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).isEmbeddable());
	}

	// --- Hibernate-specific class annotations ---

	private record TestContext(TemplateHelper helper, ModelsContext modelsContext, ClassDetails classDetails) {}

	private TestContext createWithContext(TableMetadata table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		TemplateHelper helper = new TemplateHelper(classDetails, builder.getModelsContext(),
				new ImportContextImpl(pkg), true,
				Collections.emptyMap(), Collections.emptyMap());
		return new TestContext(helper, builder.getModelsContext(), classDetails);
	}

	@Test
	public void testGenerateClassAnnotationsImmutable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		dc.addAnnotationUsage(HibernateAnnotations.IMMUTABLE.createUsage(ctx.modelsContext()));
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@Immutable"), result);
	}

	@Test
	public void testGenerateClassAnnotationsDynamicInsert() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		dc.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx.modelsContext()));
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@DynamicInsert"), result);
	}

	@Test
	public void testGenerateClassAnnotationsDynamicUpdate() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		dc.addAnnotationUsage(HibernateAnnotations.DYNAMIC_UPDATE.createUsage(ctx.modelsContext()));
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@DynamicUpdate"), result);
	}

	@Test
	public void testGenerateClassAnnotationsBatchSize() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx.modelsContext());
		bs.size(25);
		dc.addAnnotationUsage(bs);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@BatchSize(size = 25)"), result);
	}

	// --- @Cache ---

	@Test
	public void testGenerateClassAnnotationsCacheReadWrite() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		dc.addAnnotationUsage(cache);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)"), result);
	}

	@Test
	public void testGenerateClassAnnotationsCacheWithRegion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.READ_ONLY);
		cache.region("employee-cache");
		dc.addAnnotationUsage(cache);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("CacheConcurrencyStrategy.READ_ONLY"), result);
		assertTrue(result.contains("region = \"employee-cache\""), result);
	}

	@Test
	public void testGenerateClassAnnotationsCacheIncludeLazyFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.NONSTRICT_READ_WRITE);
		cache.includeLazy(false);
		dc.addAnnotationUsage(cache);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("CacheConcurrencyStrategy.NONSTRICT_READ_WRITE"), result);
		assertTrue(result.contains("includeLazy = false"), result);
	}

	@Test
	public void testGenerateClassAnnotationsCacheNoneSkipped() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx.modelsContext());
		cache.usage(CacheConcurrencyStrategy.NONE);
		dc.addAnnotationUsage(cache);
		String result = ctx.helper().generateClassAnnotations();
		assertFalse(result.contains("@Cache"), result);
	}

	// --- @NaturalId ---

	@Test
	public void testGenerateNaturalIdAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("email")).findFirst().orElseThrow();
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(nid);
		assertEquals("@NaturalId", ctx.helper().generateNaturalIdAnnotation(field));
	}

	@Test
	public void testGenerateNaturalIdAnnotationMutable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("email")).findFirst().orElseThrow();
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		nid.mutable(true);
		((MutableAnnotationTarget) field).addAnnotationUsage(nid);
		assertEquals("@NaturalId(mutable = true)", ctx.helper().generateNaturalIdAnnotation(field));
	}

	@Test
	public void testGenerateNaturalIdAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateNaturalIdAnnotation(field));
	}

	@Test
	public void testHasNaturalId() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		assertFalse(ctx.helper().hasNaturalId());
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("email")).findFirst().orElseThrow();
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(nid);
		assertTrue(ctx.helper().hasNaturalId());
	}

	@Test
	public void testNaturalIdUsedForEqualsHashCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("email")).findFirst().orElseThrow();
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(nid);
		assertTrue(ctx.helper().needsEqualsHashCode());
		List<FieldDetails> idFields = ctx.helper().getIdentifierFields();
		assertEquals(1, idFields.size());
		assertEquals("email", idFields.get(0).getName());
	}

	// --- @OrderBy ---

	@Test
	public void testGenerateOrderByAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		OrderByJpaAnnotation ob = JpaAnnotations.ORDER_BY.createUsage(ctx.modelsContext());
		ob.value("lastName ASC, firstName ASC");
		((MutableAnnotationTarget) field).addAnnotationUsage(ob);
		assertEquals("@OrderBy(\"lastName ASC, firstName ASC\")",
				ctx.helper().generateOrderByAnnotation(field));
	}

	@Test
	public void testGenerateOrderByAnnotationEmpty() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateOrderByAnnotation(field));
	}

	// --- @OrderColumn ---

	@Test
	public void testGenerateOrderColumnAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		OrderColumnJpaAnnotation oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx.modelsContext());
		oc.name("POSITION");
		((MutableAnnotationTarget) field).addAnnotationUsage(oc);
		assertEquals("@OrderColumn(name = \"POSITION\")",
				ctx.helper().generateOrderColumnAnnotation(field));
	}

	@Test
	public void testGenerateOrderColumnAnnotationEmpty() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateOrderColumnAnnotation(field));
	}

	// --- @Convert ---

	@Test
	public void testGenerateConvertAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", Boolean.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		ConvertJpaAnnotation convert = JpaAnnotations.CONVERT.createUsage(ctx.modelsContext());
		convert.converter(org.hibernate.type.YesNoConverter.class);
		((MutableAnnotationTarget) field).addAnnotationUsage(convert);
		String result = ctx.helper().generateConvertAnnotation(field);
		assertTrue(result.contains("@Convert(converter = YesNoConverter.class)"), result);
	}

	@Test
	public void testGenerateConvertAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertEquals("", helper.generateConvertAnnotation(field));
	}

	@Test
	public void testGenerateConvertAnnotationDisabled() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", Boolean.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		ConvertJpaAnnotation convert = JpaAnnotations.CONVERT.createUsage(ctx.modelsContext());
		convert.converter(org.hibernate.type.YesNoConverter.class);
		convert.disableConversion(true);
		((MutableAnnotationTarget) field).addAnnotationUsage(convert);
		assertEquals("", ctx.helper().generateConvertAnnotation(field));
	}

	// --- @Fetch ---

	@Test
	public void testGenerateFetchAnnotationJoin() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx.modelsContext());
		fetch.value(FetchMode.JOIN);
		((MutableAnnotationTarget) field).addAnnotationUsage(fetch);
		assertEquals("@Fetch(FetchMode.JOIN)", ctx.helper().generateFetchAnnotation(field));
	}

	@Test
	public void testGenerateFetchAnnotationSubselect() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx.modelsContext());
		fetch.value(FetchMode.SUBSELECT);
		((MutableAnnotationTarget) field).addAnnotationUsage(fetch);
		assertEquals("@Fetch(FetchMode.SUBSELECT)", ctx.helper().generateFetchAnnotation(field));
	}

	@Test
	public void testGenerateFetchAnnotationNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateFetchAnnotation(field));
	}

	// --- @NotFound ---

	@Test
	public void testGenerateNotFoundAnnotationIgnore() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getManyToOneFields().get(0);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx.modelsContext());
		nf.action(NotFoundAction.IGNORE);
		((MutableAnnotationTarget) field).addAnnotationUsage(nf);
		assertEquals("@NotFound(action = NotFoundAction.IGNORE)",
				ctx.helper().generateNotFoundAnnotation(field));
	}

	@Test
	public void testGenerateNotFoundAnnotationExceptionSkipped() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getManyToOneFields().get(0);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx.modelsContext());
		nf.action(NotFoundAction.EXCEPTION);
		((MutableAnnotationTarget) field).addAnnotationUsage(nf);
		assertEquals("", ctx.helper().generateNotFoundAnnotation(field));
	}

	@Test
	public void testGenerateNotFoundAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		assertEquals("", helper.generateNotFoundAnnotation(field));
	}

	// --- @Any / @ManyToAny ---

	@Test
	public void testGenerateAnyAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails entity = (DynamicClassDetails) ctx.classDetails();
		ClassDetails objClass = ctx.modelsContext().getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		TypeDetails objType = new ClassTypeDetailsImpl(objClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entity.applyAttribute(
				"payment", objType, false, false, ctx.modelsContext());
		field.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx.modelsContext()));
		AnyDiscriminatorAnnotation ad = HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.modelsContext());
		ad.value(DiscriminatorType.STRING);
		field.addAnnotationUsage(ad);
		AnyDiscriminatorValueAnnotation v1 = HibernateAnnotations.ANY_DISCRIMINATOR_VALUE.createUsage(ctx.modelsContext());
		v1.discriminator("CC");
		v1.entity(String.class);
		AnyDiscriminatorValuesAnnotation container = HibernateAnnotations.ANY_DISCRIMINATOR_VALUES.createUsage(ctx.modelsContext());
		container.value(new AnyDiscriminatorValue[] { v1 });
		field.addAnnotationUsage(container);
		AnyKeyJavaClassAnnotation akjc = HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx.modelsContext());
		akjc.value(Long.class);
		field.addAnnotationUsage(akjc);
		String result = ctx.helper().generateAnyAnnotation(field);
		assertTrue(result.contains("@Any"), result);
		assertTrue(result.contains("@AnyDiscriminator(DiscriminatorType.STRING)"), result);
		assertTrue(result.contains("@AnyDiscriminatorValue(discriminator = \"CC\""), result);
		assertTrue(result.contains("@AnyKeyJavaClass(Long.class)"), result);
	}

	@Test
	public void testGenerateAnyAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table);
		assertTrue(helper.getAnyFields().isEmpty());
	}

	@Test
	public void testGenerateManyToAnyAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails entity = (DynamicClassDetails) ctx.classDetails();
		ClassDetails objClass = ctx.modelsContext().getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		ClassDetails setClass = ctx.modelsContext().getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails objType = new ClassTypeDetailsImpl(objClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(objType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"payments", fieldType, false, true, ctx.modelsContext());
		field.addAnnotationUsage(HibernateAnnotations.MANY_TO_ANY.createUsage(ctx.modelsContext()));
		AnyDiscriminatorAnnotation ad = HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.modelsContext());
		ad.value(DiscriminatorType.STRING);
		field.addAnnotationUsage(ad);
		String result = ctx.helper().generateManyToAnyAnnotation(field);
		assertTrue(result.contains("@ManyToAny"), result);
		assertTrue(result.contains("@AnyDiscriminator(DiscriminatorType.STRING)"), result);
	}

	// --- @Bag / @CollectionId ---

	@Test
	public void testGenerateBagAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		((MutableAnnotationTarget) field).addAnnotationUsage(
				HibernateAnnotations.BAG.createUsage(ctx.modelsContext()));
		assertEquals("@Bag", ctx.helper().generateBagAnnotation(field));
	}

	@Test
	public void testGenerateBagAnnotationNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateBagAnnotation(field));
	}

	@Test
	public void testGenerateCollectionIdAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		CollectionIdAnnotation cid = HibernateAnnotations.COLLECTION_ID.createUsage(ctx.modelsContext());
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx.modelsContext());
		col.name("COLL_ID");
		cid.column(col);
		cid.generator("increment");
		((MutableAnnotationTarget) field).addAnnotationUsage(cid);
		String result = ctx.helper().generateCollectionIdAnnotation(field);
		assertTrue(result.contains("@CollectionId("), result);
		assertTrue(result.contains("@Column(name = \"COLL_ID\")"), result);
		assertTrue(result.contains("generator = \"increment\""), result);
	}

	@Test
	public void testGenerateCollectionIdAnnotationNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateCollectionIdAnnotation(field));
	}

	// --- @MapKey / @MapKeyColumn ---

	@Test
	public void testGenerateMapKeyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		MapKeyJpaAnnotation mk = JpaAnnotations.MAP_KEY.createUsage(ctx.modelsContext());
		mk.name("employeeId");
		((MutableAnnotationTarget) field).addAnnotationUsage(mk);
		assertEquals("@MapKey(name = \"employeeId\")",
				ctx.helper().generateMapKeyAnnotation(field));
	}

	@Test
	public void testGenerateMapKeyAnnotationNoName() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		MapKeyJpaAnnotation mk = JpaAnnotations.MAP_KEY.createUsage(ctx.modelsContext());
		mk.name("");
		((MutableAnnotationTarget) field).addAnnotationUsage(mk);
		assertEquals("@MapKey", ctx.helper().generateMapKeyAnnotation(field));
	}

	@Test
	public void testGenerateMapKeyColumnAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		MapKeyColumnJpaAnnotation mkc = JpaAnnotations.MAP_KEY_COLUMN.createUsage(ctx.modelsContext());
		mkc.name("EMP_KEY");
		((MutableAnnotationTarget) field).addAnnotationUsage(mkc);
		assertEquals("@MapKeyColumn(name = \"EMP_KEY\")",
				ctx.helper().generateMapKeyColumnAnnotation(field));
	}

	@Test
	public void testGenerateMapKeyAnnotationNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateMapKeyAnnotation(field));
		assertEquals("", helper.generateMapKeyColumnAnnotation(field));
	}

	// --- @Formula ---

	@Test
	public void testGenerateFormulaAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("FULL_NAME", "fullName", String.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("fullName")).findFirst().orElseThrow();
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx.modelsContext());
		formula.value("first_name || ' ' || last_name");
		((MutableAnnotationTarget) field).addAnnotationUsage(formula);
		String result = ctx.helper().generateFormulaAnnotation(field);
		assertEquals("@Formula(\"first_name || ' ' || last_name\")", result);
	}

	@Test
	public void testGenerateFormulaAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateFormulaAnnotation(field));
	}

	@Test
	public void testHasFormula() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("FULL_NAME", "fullName", String.class));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().get(0);
		assertFalse(ctx.helper().hasFormula(field));
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx.modelsContext());
		formula.value("1+1");
		((MutableAnnotationTarget) field).addAnnotationUsage(formula);
		assertTrue(ctx.helper().hasFormula(field));
	}

	// --- @SequenceGenerator / @TableGenerator ---

	@Test
	public void testGenerateIdAnnotationsWithSequenceGenerator() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.SEQUENCE));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		// Add generator name to @GeneratedValue
		GeneratedValueJpaAnnotation gv = (GeneratedValueJpaAnnotation) field.getDirectAnnotationUsage(GeneratedValue.class);
		gv.generator("emp_seq");
		// Add @SequenceGenerator
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx.modelsContext());
		sg.name("emp_seq");
		sg.sequenceName("EMPLOYEE_SEQ");
		sg.allocationSize(1);
		((MutableAnnotationTarget) field).addAnnotationUsage(sg);
		String result = ctx.helper().generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertTrue(result.contains("GenerationType.SEQUENCE"), result);
		assertTrue(result.contains("generator = \"emp_seq\""), result);
		assertTrue(result.contains("@SequenceGenerator(name = \"emp_seq\""), result);
		assertTrue(result.contains("sequenceName = \"EMPLOYEE_SEQ\""), result);
		assertTrue(result.contains("allocationSize = 1"), result);
	}

	@Test
	public void testGenerateIdAnnotationsWithTableGenerator() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.TABLE));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		GeneratedValueJpaAnnotation gv = (GeneratedValueJpaAnnotation) field.getDirectAnnotationUsage(GeneratedValue.class);
		gv.generator("emp_tbl_gen");
		TableGeneratorJpaAnnotation tg = JpaAnnotations.TABLE_GENERATOR.createUsage(ctx.modelsContext());
		tg.name("emp_tbl_gen");
		tg.table("ID_GEN");
		tg.pkColumnName("GEN_NAME");
		tg.valueColumnName("GEN_VALUE");
		tg.allocationSize(10);
		((MutableAnnotationTarget) field).addAnnotationUsage(tg);
		String result = ctx.helper().generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertTrue(result.contains("GenerationType.TABLE"), result);
		assertTrue(result.contains("generator = \"emp_tbl_gen\""), result);
		assertTrue(result.contains("@TableGenerator(name = \"emp_tbl_gen\""), result);
		assertTrue(result.contains("table = \"ID_GEN\""), result);
		assertTrue(result.contains("pkColumnName = \"GEN_NAME\""), result);
		assertTrue(result.contains("valueColumnName = \"GEN_VALUE\""), result);
		assertTrue(result.contains("allocationSize = 10"), result);
	}

	// --- @ElementCollection ---

	@Test
	public void testGetElementCollectionFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		addElementCollectionField(dc, "nicknames", String.class, ctx.modelsContext());
		List<FieldDetails> ecFields = ctx.helper().getElementCollectionFields();
		assertEquals(1, ecFields.size());
		assertEquals("nicknames", ecFields.get(0).getName());
	}

	@Test
	public void testBasicFieldsExcludesElementCollection() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		addElementCollectionField(dc, "nicknames", String.class, ctx.modelsContext());
		List<FieldDetails> basicFields = ctx.helper().getBasicFields();
		assertEquals(2, basicFields.size());
		assertTrue(basicFields.stream().noneMatch(f -> f.getName().equals("nicknames")));
	}

	@Test
	public void testGenerateElementCollectionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		DynamicFieldDetails field = addElementCollectionField(
				dc, "nicknames", String.class, ctx.modelsContext());
		// Add @CollectionTable
		CollectionTableJpaAnnotation ct = JpaAnnotations.COLLECTION_TABLE.createUsage(ctx.modelsContext());
		ct.name("EMPLOYEE_NICKNAMES");
		field.addAnnotationUsage(ct);
		// Add @Column for element
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx.modelsContext());
		col.name("NICKNAME");
		field.addAnnotationUsage(col);
		String result = ctx.helper().generateElementCollectionAnnotation(field);
		assertTrue(result.contains("@ElementCollection"), result);
		assertTrue(result.contains("@CollectionTable(name = \"EMPLOYEE_NICKNAMES\""), result);
		assertTrue(result.contains("@Column(name = \"NICKNAME\")"), result);
	}

	@Test
	public void testGenerateElementCollectionAnnotationNoAnnotated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) classDetails;
		DynamicFieldDetails field = addElementCollectionField(
				dc, "nicknames", String.class, builder.getModelsContext());
		TemplateHelper helper = new TemplateHelper(classDetails, builder.getModelsContext(),
				new ImportContextImpl("com.example"), false,
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("", helper.generateElementCollectionAnnotation(field));
	}

	// --- Minimal constructor ---

	@Test
	public void testNeedsMinimalConstructorTrue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnMetadata("NICKNAME", "nickname", String.class));
		TemplateHelper helper = create(table);
		assertTrue(helper.needsMinimalConstructor());
	}

	@Test
	public void testNeedsMinimalConstructorFalseAllRequired() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		TemplateHelper helper = create(table);
		// minimal = [id (assigned), name (non-nullable)] same size as full = [id, name]
		assertFalse(helper.needsMinimalConstructor());
	}

	@Test
	public void testNeedsMinimalConstructorFalseNoRequired() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		// minimal = [] (ID has generator, NAME is nullable), full = [id, name]
		assertFalse(helper.needsMinimalConstructor());
	}

	@Test
	public void testMinimalConstructorProperties() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnMetadata("NICKNAME", "nickname", String.class));
		TemplateHelper helper = create(table);
		List<TemplateHelper.FullConstructorProperty> props = helper.getMinimalConstructorProperties();
		assertEquals(2, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("name", props.get(1).fieldName());
	}

	@Test
	public void testMinimalConstructorSkipsGeneratedId() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnMetadata("NICKNAME", "nickname", String.class));
		TemplateHelper helper = create(table);
		List<TemplateHelper.FullConstructorProperty> props = helper.getMinimalConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("name", props.get(0).fieldName());
	}

	@Test
	public void testMinimalConstructorSkipsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class)
				.version(true).nullable(false));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		TemplateHelper helper = create(table);
		List<TemplateHelper.FullConstructorProperty> props = helper.getMinimalConstructorProperties();
		assertTrue(props.stream().noneMatch(p -> p.fieldName().equals("version")));
	}

	@Test
	public void testMinimalConstructorIncludesNonOptionalManyToOne() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.optional(false));
		TemplateHelper helper = create(table);
		List<TemplateHelper.FullConstructorProperty> props = helper.getMinimalConstructorProperties();
		assertTrue(props.stream().anyMatch(p -> p.fieldName().equals("department")));
	}

	@Test
	public void testMinimalConstructorParameterList() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnMetadata("NICKNAME", "nickname", String.class));
		assertEquals("Long id, String name", create(table).getMinimalConstructorParameterList());
	}

	// --- Collection type differentiation ---

	@Test
	public void testGetCollectionTypeNameSet() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("Set<Employee>", helper.getCollectionTypeName(field));
	}

	@Test
	public void testGetCollectionInitializerTypeSet() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("HashSet", helper.getCollectionInitializerType(field));
	}

	@Test
	public void testGetCollectionTypeNameList() {
		ModelsContext ctx = new BasicModelsContextImpl(
				SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = new DynamicClassDetails(
				"Department", "com.example.Department",
				false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		// Create a List<Employee> field
		ClassDetails elementClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx);
		ClassDetails listClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(java.util.List.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				listClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"employees", fieldType, false, true, ctx);
		field.addAnnotationUsage(JpaAnnotations.ONE_TO_MANY.createUsage(ctx));
		TemplateHelper helper = new TemplateHelper(entity, ctx,
				new ImportContextImpl("com.example"), true,
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("List<Employee>", helper.getCollectionTypeName(field));
		assertEquals("ArrayList", helper.getCollectionInitializerType(field));
	}

	// --- @FilterDef / @Filter ---

	@Test
	public void testGenerateClassAnnotationsFilterSimple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeFilter");
		filter.condition("active = true");
		dc.addAnnotationUsage(filter);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@Filter(name = \"activeFilter\", condition = \"active = true\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsFilterNoCondition() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeFilter");
		filter.condition("");
		dc.addAnnotationUsage(filter);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@Filter(name = \"activeFilter\")"), result);
		assertFalse(result.contains("condition"), result);
	}

	@Test
	public void testGenerateClassAnnotationsMultipleFilters() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		FilterAnnotation f1 = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		f1.name("activeFilter");
		f1.condition("active = true");
		FilterAnnotation f2 = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		f2.name("tenantFilter");
		f2.condition("tenant_id = :tid");
		FiltersAnnotation filters = HibernateAnnotations.FILTERS.createUsage(ctx.modelsContext());
		filters.value(new Filter[] { f1, f2 });
		dc.addAnnotationUsage(filters);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@Filter(name = \"activeFilter\", condition = \"active = true\")"), result);
		assertTrue(result.contains("@Filter(name = \"tenantFilter\", condition = \"tenant_id = :tid\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsFilterDefSimple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx.modelsContext());
		fd.name("activeFilter");
		fd.defaultCondition("active = true");
		dc.addAnnotationUsage(fd);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@FilterDef(name = \"activeFilter\", defaultCondition = \"active = true\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsFilterDefNoCondition() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx.modelsContext());
		fd.name("activeFilter");
		fd.defaultCondition("");
		dc.addAnnotationUsage(fd);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@FilterDef(name = \"activeFilter\")"), result);
		assertFalse(result.contains("defaultCondition"), result);
	}

	@Test
	public void testGenerateClassAnnotationsFilterDefWithParams() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		ParamDefAnnotation pd = new ParamDefAnnotation(ctx.modelsContext());
		pd.name("isActive");
		pd.type(Boolean.class);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx.modelsContext());
		fd.name("activeFilter");
		fd.defaultCondition("active = :isActive");
		fd.parameters(new org.hibernate.annotations.ParamDef[] { pd });
		dc.addAnnotationUsage(fd);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@FilterDef(name = \"activeFilter\", defaultCondition = \"active = :isActive\", parameters = {"), result);
		assertTrue(result.contains("@ParamDef(name = \"isActive\", type = Boolean.class)"), result);
	}

	@Test
	public void testGetFiltersNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		assertTrue(ctx.helper().getFilters().isEmpty());
	}

	@Test
	public void testGetFilterDefsNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		assertTrue(ctx.helper().getFilterDefs().isEmpty());
	}

	// --- Collection-level @Filter ---

	@Test
	public void testGenerateFilterAnnotationsOnOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeFilter");
		filter.condition("active = true");
		((MutableAnnotationTarget) field).addAnnotationUsage(filter);
		String result = ctx.helper().generateFilterAnnotations(field);
		assertTrue(result.contains("@Filter(name = \"activeFilter\", condition = \"active = true\")"), result);
	}

	@Test
	public void testGenerateFilterAnnotationsNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateFilterAnnotations(field));
	}

	@Test
	public void testGenerateFilterAnnotationsMultiple() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		FilterAnnotation f1 = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		f1.name("activeFilter");
		f1.condition("active = true");
		FilterAnnotation f2 = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		f2.name("tenantFilter");
		f2.condition("tenant_id = :tid");
		FiltersAnnotation filters = HibernateAnnotations.FILTERS.createUsage(ctx.modelsContext());
		filters.value(new Filter[] { f1, f2 });
		((MutableAnnotationTarget) field).addAnnotationUsage(filters);
		String result = ctx.helper().generateFilterAnnotations(field);
		assertTrue(result.contains("@Filter(name = \"activeFilter\""), result);
		assertTrue(result.contains("@Filter(name = \"tenantFilter\""), result);
	}

	// --- @NamedQuery / @NamedNativeQuery ---

	@Test
	public void testGenerateClassAnnotationsNamedQuery() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx.modelsContext());
		nq.name("Employee.findAll");
		nq.query("SELECT e FROM Employee e");
		dc.addAnnotationUsage(nq);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@NamedQuery(name = \"Employee.findAll\", query = \"SELECT e FROM Employee e\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsNamedNativeQuery() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx.modelsContext());
		nnq.name("Employee.findAllNative");
		nnq.query("SELECT * FROM EMPLOYEE");
		dc.addAnnotationUsage(nnq);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@NamedNativeQuery(name = \"Employee.findAllNative\", query = \"SELECT * FROM EMPLOYEE\")"), result);
	}

	@Test
	public void testGetNamedQueriesNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		assertTrue(ctx.helper().getNamedQueries().isEmpty());
	}

	@Test
	public void testGetNamedNativeQueriesNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		assertTrue(ctx.helper().getNamedNativeQueries().isEmpty());
	}

	// --- @SQLInsert / @SQLUpdate / @SQLDelete ---

	@Test
	public void testGenerateClassAnnotationsSQLInsert() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx.modelsContext());
		si.sql("INSERT INTO EMPLOYEE (name) VALUES (?)");
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(si);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SQLInsert(sql = \"INSERT INTO EMPLOYEE (name) VALUES (?)\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsSQLUpdate() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx.modelsContext());
		su.sql("UPDATE EMPLOYEE SET name = ? WHERE id = ?");
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(su);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SQLUpdate(sql = \"UPDATE EMPLOYEE SET name = ? WHERE id = ?\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsSQLDelete() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx.modelsContext());
		sd.sql("DELETE FROM EMPLOYEE WHERE id = ?");
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(sd);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SQLDelete(sql = \"DELETE FROM EMPLOYEE WHERE id = ?\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsSQLInsertCallable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx.modelsContext());
		si.sql("{call insertEmployee(?)}");
		si.callable(true);
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(si);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("callable = true"), result);
	}

	// --- @SortNatural / @SortComparator ---

	@Test
	public void testGenerateSortNaturalAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		((MutableAnnotationTarget) field).addAnnotationUsage(
				HibernateAnnotations.SORT_NATURAL.createUsage(ctx.modelsContext()));
		assertEquals("@SortNatural", ctx.helper().generateSortAnnotation(field));
	}

	@Test
	public void testGenerateSortComparatorAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TestContext ctx = createWithContext(table);
		FieldDetails field = ctx.helper().getOneToManyFields().get(0);
		SortComparatorAnnotation sc = HibernateAnnotations.SORT_COMPARATOR.createUsage(ctx.modelsContext());
		sc.value(java.text.Collator.class);
		((MutableAnnotationTarget) field).addAnnotationUsage(sc);
		String result = ctx.helper().generateSortAnnotation(field);
		assertTrue(result.contains("@SortComparator("), result);
		assertTrue(result.contains("Collator.class"), result);
	}

	@Test
	public void testGenerateSortAnnotationNone() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateSortAnnotation(field));
	}

	// --- @FetchProfile ---

	@Test
	public void testGenerateClassAnnotationsFetchProfile() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		FetchProfileAnnotation fp = HibernateAnnotations.FETCH_PROFILE.createUsage(ctx.modelsContext());
		fp.name("employee-with-dept");
		fp.fetchOverrides(new org.hibernate.annotations.FetchProfile.FetchOverride[] {});
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(fp);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@FetchProfile(name = \"employee-with-dept\")"), result);
	}

	@Test
	public void testGetFetchProfilesNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertTrue(helper.getFetchProfiles().isEmpty());
	}

	// --- @EntityListeners ---

	@Test
	public void testGenerateClassAnnotationsEntityListenersSingle() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		EntityListenersJpaAnnotation el = JpaAnnotations.ENTITY_LISTENERS.createUsage(ctx.modelsContext());
		el.value(new Class<?>[] { java.io.Serializable.class });
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(el);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@EntityListeners(Serializable.class)"), result);
	}

	@Test
	public void testGenerateClassAnnotationsEntityListenersMultiple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		EntityListenersJpaAnnotation el = JpaAnnotations.ENTITY_LISTENERS.createUsage(ctx.modelsContext());
		el.value(new Class<?>[] { java.io.Serializable.class, java.lang.Comparable.class });
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(el);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@EntityListeners({Serializable.class, Comparable.class})"), result);
	}

	@Test
	public void testGenerateClassAnnotationsNoEntityListeners() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		String result = ctx.helper().generateClassAnnotations();
		assertFalse(result.contains("@EntityListeners"), result);
	}

	// --- Lifecycle callbacks ---

	static class WithPrePersist {
		@jakarta.persistence.PrePersist
		void onPrePersist() {}
	}

	static class WithMultipleCallbacks {
		@jakarta.persistence.PrePersist
		void onPrePersist() {}
		@jakarta.persistence.PostLoad
		void onPostLoad() {}
		@jakarta.persistence.PreUpdate
		void onPreUpdate() {}
	}

	@Test
	public void testGetLifecycleCallbacksSingle() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		addMethodsFrom(WithPrePersist.class, (DynamicClassDetails) ctx.classDetails(), ctx.modelsContext());
		List<TemplateHelper.LifecycleCallbackInfo> callbacks = ctx.helper().getLifecycleCallbacks();
		assertEquals(1, callbacks.size());
		assertEquals("PrePersist", callbacks.get(0).annotationType());
		assertEquals("onPrePersist", callbacks.get(0).methodName());
	}

	@Test
	public void testGetLifecycleCallbacksMultiple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		addMethodsFrom(WithMultipleCallbacks.class, (DynamicClassDetails) ctx.classDetails(), ctx.modelsContext());
		List<TemplateHelper.LifecycleCallbackInfo> callbacks = ctx.helper().getLifecycleCallbacks();
		assertEquals(3, callbacks.size());
	}

	@Test
	public void testGetLifecycleCallbacksNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertTrue(helper.getLifecycleCallbacks().isEmpty());
	}

	@Test
	public void testGenerateLifecycleCallbackAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		TemplateHelper.LifecycleCallbackInfo callback =
				new TemplateHelper.LifecycleCallbackInfo("PrePersist", "onPrePersist");
		String result = ctx.helper().generateLifecycleCallbackAnnotation(callback);
		assertEquals("@PrePersist", result);
	}

	private void addMethodsFrom(Class<?> source, DynamicClassDetails target, ModelsContext modelsContext) {
		for (java.lang.reflect.Method method : source.getDeclaredMethods()) {
			JdkMethodDetails md = new JdkMethodDetails(
					method, MethodDetails.MethodKind.OTHER, null, target, modelsContext);
			target.addMethod(md);
		}
	}

	private DynamicFieldDetails addElementCollectionField(
			DynamicClassDetails entity, String fieldName, Class<?> elementJavaType, ModelsContext ctx) {
		ClassDetails elementClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(elementJavaType.getName());
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				fieldName, fieldType, false, true, ctx);
		ElementCollectionJpaAnnotation ec = JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx);
		field.addAnnotationUsage(ec);
		return field;
	}

	// --- Meta-attribute: scope-field ---

	@Test
	public void testGetFieldModifiersDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("private", helper.getFieldModifiers(field));
	}

	@Test
	public void testGetFieldModifiersProtected() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = fieldMeta("name", "scope-field", "protected");
		TemplateHelper helper = create(table, true, Collections.emptyMap(), fieldMeta);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("protected", helper.getFieldModifiers(field));
	}

	// --- Meta-attribute: scope-get ---

	@Test
	public void testGetPropertyGetModifiersDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("public", helper.getPropertyGetModifiers(field));
	}

	@Test
	public void testGetPropertyGetModifiersProtected() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = fieldMeta("name", "scope-get", "protected");
		TemplateHelper helper = create(table, true, Collections.emptyMap(), fieldMeta);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("protected", helper.getPropertyGetModifiers(field));
	}

	// --- Meta-attribute: scope-set ---

	@Test
	public void testGetPropertySetModifiersDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("public", helper.getPropertySetModifiers(field));
	}

	@Test
	public void testGetPropertySetModifiersPrivate() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = fieldMeta("name", "scope-set", "private");
		TemplateHelper helper = create(table, true, Collections.emptyMap(), fieldMeta);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("private", helper.getPropertySetModifiers(field));
	}

	// --- Meta-attribute: implements ---

	@Test
	public void testGetImplementsDeclarationDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertEquals("implements Serializable", helper.getImplementsDeclaration());
	}

	@Test
	public void testGetImplementsDeclarationWithMeta() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("implements", List.of("java.lang.Comparable"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		String result = helper.getImplementsDeclaration();
		assertTrue(result.contains("Comparable"), result);
		assertTrue(result.contains("Serializable"), result);
	}

	@Test
	public void testGetImplementsDeclarationMultiple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("implements",
				List.of("java.lang.Comparable", "java.lang.Cloneable"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		String result = helper.getImplementsDeclaration();
		assertTrue(result.contains("Comparable"), result);
		assertTrue(result.contains("Cloneable"), result);
		assertTrue(result.contains("Serializable"), result);
	}

	// --- Meta-attribute: extends ---

	@Test
	public void testGetExtendsDeclarationDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertEquals("", helper.getExtendsDeclaration());
	}

	@Test
	public void testGetExtendsDeclarationWithMeta() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("extends", List.of("com.example.BaseEntity"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		String result = helper.getExtendsDeclaration();
		assertTrue(result.contains("extends"), result);
		assertTrue(result.contains("BaseEntity"), result);
	}

	// --- Meta-attribute: class-description ---

	@Test
	public void testHasClassDescriptionFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertFalse(helper.hasClassDescription());
	}

	@Test
	public void testHasClassDescriptionTrue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("class-description", List.of("Employee entity"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		assertTrue(helper.hasClassDescription());
		assertEquals("Employee entity", helper.getClassDescription());
	}

	// --- Meta-attribute: extra-import ---

	@Test
	public void testExtraImport() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("extra-import", List.of("com.example.util.MyHelper"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		String imports = helper.generateImports();
		assertTrue(imports.contains("com.example.util.MyHelper"), imports);
	}

	@Test
	public void testExtraImportMultiple() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("extra-import",
				List.of("com.example.util.MyHelper", "com.example.util.AnotherHelper"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		String imports = helper.generateImports();
		assertTrue(imports.contains("com.example.util.MyHelper"), imports);
		assertTrue(imports.contains("com.example.util.AnotherHelper"), imports);
	}

	// --- Meta-attribute: generated-class ---

	@Test
	public void testGetDeclarationNameDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertEquals("Employee", helper.getDeclarationName());
	}

	@Test
	public void testGetDeclarationNameWithGeneratedClass() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("generated-class", List.of("com.generated.EmployeeBase"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		assertEquals("EmployeeBase", helper.getDeclarationName());
	}

	@Test
	public void testGetPackageDeclarationWithGeneratedClass() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		Map<String, List<String>> classMeta = Map.of("generated-class", List.of("com.generated.EmployeeBase"));
		TemplateHelper helper = create(table, true, classMeta, Collections.emptyMap());
		assertEquals("package com.generated;", helper.getPackageDeclaration());
	}

	// --- @SqlResultSetMapping ---

	@Test
	public void testGetSqlResultSetMappingsNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		assertTrue(ctx.helper().getSqlResultSetMappings().isEmpty());
	}

	@Test
	public void testGetSqlResultSetMappingsWithEntityResult() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx.modelsContext());
		mapping.name("empMapping");
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(ctx.modelsContext());
		er.entityClass(String.class);
		FieldResultJpaAnnotation fr = JpaAnnotations.FIELD_RESULT.createUsage(ctx.modelsContext());
		fr.name("id");
		fr.column("EMP_ID");
		er.fields(new jakarta.persistence.FieldResult[]{fr});
		mapping.entities(new jakarta.persistence.EntityResult[]{er});
		mapping.columns(new jakarta.persistence.ColumnResult[]{});
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(mapping);
		List<TemplateHelper.SqlResultSetMappingInfo> result = ctx.helper().getSqlResultSetMappings();
		assertEquals(1, result.size());
		assertEquals("empMapping", result.get(0).name());
		assertEquals(1, result.get(0).entityResults().size());
		assertEquals("java.lang.String", result.get(0).entityResults().get(0).entityClass());
		assertEquals(1, result.get(0).entityResults().get(0).fieldResults().size());
		assertEquals("id", result.get(0).entityResults().get(0).fieldResults().get(0).name());
		assertEquals("EMP_ID", result.get(0).entityResults().get(0).fieldResults().get(0).column());
	}

	@Test
	public void testGetSqlResultSetMappingsWithColumnResult() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx.modelsContext());
		mapping.name("scalarMapping");
		mapping.entities(new jakarta.persistence.EntityResult[]{});
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(ctx.modelsContext());
		cr.name("TOTAL");
		mapping.columns(new jakarta.persistence.ColumnResult[]{cr});
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(mapping);
		List<TemplateHelper.SqlResultSetMappingInfo> result = ctx.helper().getSqlResultSetMappings();
		assertEquals(1, result.size());
		assertEquals(1, result.get(0).columnResults().size());
		assertEquals("TOTAL", result.get(0).columnResults().get(0).name());
	}

	@Test
	public void testGenerateClassAnnotationsSqlResultSetMapping() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx.modelsContext());
		mapping.name("empMapping");
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(ctx.modelsContext());
		er.entityClass(String.class);
		er.discriminatorColumn("DTYPE");
		FieldResultJpaAnnotation fr = JpaAnnotations.FIELD_RESULT.createUsage(ctx.modelsContext());
		fr.name("id");
		fr.column("EMP_ID");
		er.fields(new jakarta.persistence.FieldResult[]{fr});
		mapping.entities(new jakarta.persistence.EntityResult[]{er});
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(ctx.modelsContext());
		cr.name("EXTRA");
		mapping.columns(new jakarta.persistence.ColumnResult[]{cr});
		((MutableAnnotationTarget) ctx.classDetails()).addAnnotationUsage(mapping);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("@SqlResultSetMapping(name = \"empMapping\""), result);
		assertTrue(result.contains("@EntityResult(entityClass = String.class, discriminatorColumn = \"DTYPE\""), result);
		assertTrue(result.contains("@FieldResult(name = \"id\", column = \"EMP_ID\")"), result);
		assertTrue(result.contains("@ColumnResult(name = \"EXTRA\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsNamedNativeQueryWithResultClass() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx.modelsContext());
		nnq.name("findAll");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultClass(String.class);
		dc.addAnnotationUsage(nnq);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("resultClass = String.class"), result);
	}

	@Test
	public void testGenerateClassAnnotationsNamedNativeQueryWithResultSetMapping() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TestContext ctx = createWithContext(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.helper().getClassDetails();
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx.modelsContext());
		nnq.name("findWithMapping");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultSetMapping("empMapping");
		dc.addAnnotationUsage(nnq);
		String result = ctx.helper().generateClassAnnotations();
		assertTrue(result.contains("resultSetMapping = \"empMapping\""), result);
	}
}
