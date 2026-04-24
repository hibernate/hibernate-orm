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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionIdAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.boot.models.annotations.internal.NotFoundAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterInfo;
import org.junit.jupiter.api.Test;

class FieldAnnotationGeneratorTest {

	private record TestContext(
			FieldAnnotationGenerator generator,
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
		FieldAnnotationGenerator generator = new FieldAnnotationGenerator(importContext, helper);
		return new TestContext(generator, helper, builder.getModelsContext(), classDetails);
	}

	private FieldDetails findField(TemplateHelper helper, String name) {
		return helper.getBasicFields().stream()
				.filter(f -> f.getName().equals(name)).findFirst().orElseThrow();
	}

	// --- @Id ---

	@Test
	void testGenerateIdAnnotations() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "id");
		String result = ctx.generator().generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertFalse(result.contains("@GeneratedValue"), result);
	}

	@Test
	void testGenerateIdAnnotationsWithGeneratedValue() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "id");
		GeneratedValueJpaAnnotation gv =
				JpaAnnotations.GENERATED_VALUE.createUsage(ctx.modelsContext());
		gv.strategy(GenerationType.SEQUENCE);
		gv.generator("emp_seq");
		field.addAnnotationUsage(gv);
		String result = ctx.generator().generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertTrue(result.contains("@GeneratedValue(strategy = GenerationType.SEQUENCE"), result);
		assertTrue(result.contains("generator = \"emp_seq\""), result);
	}

	@Test
	void testGenerateIdAnnotationsWithSequenceGenerator() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "id");
		GeneratedValueJpaAnnotation gv =
				JpaAnnotations.GENERATED_VALUE.createUsage(ctx.modelsContext());
		gv.strategy(GenerationType.SEQUENCE);
		field.addAnnotationUsage(gv);
		SequenceGeneratorJpaAnnotation sg =
				JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx.modelsContext());
		sg.name("emp_seq");
		sg.sequenceName("EMPLOYEE_SEQ");
		sg.allocationSize(10);
		field.addAnnotationUsage(sg);
		String result = ctx.generator().generateIdAnnotations(field);
		assertTrue(result.contains("@SequenceGenerator(name = \"emp_seq\""), result);
		assertTrue(result.contains("sequenceName = \"EMPLOYEE_SEQ\""), result);
		assertTrue(result.contains("allocationSize = 10"), result);
	}

	@Test
	void testGenerateIdAnnotationsWithTableGenerator() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "id");
		GeneratedValueJpaAnnotation gv =
				JpaAnnotations.GENERATED_VALUE.createUsage(ctx.modelsContext());
		gv.strategy(GenerationType.TABLE);
		field.addAnnotationUsage(gv);
		TableGeneratorJpaAnnotation tg =
				JpaAnnotations.TABLE_GENERATOR.createUsage(ctx.modelsContext());
		tg.name("emp_gen");
		tg.table("ID_GEN");
		tg.pkColumnName("GEN_NAME");
		tg.valueColumnName("GEN_VALUE");
		field.addAnnotationUsage(tg);
		String result = ctx.generator().generateIdAnnotations(field);
		assertTrue(result.contains("@TableGenerator(name = \"emp_gen\""), result);
		assertTrue(result.contains("table = \"ID_GEN\""), result);
		assertTrue(result.contains("pkColumnName = \"GEN_NAME\""), result);
		assertTrue(result.contains("valueColumnName = \"GEN_VALUE\""), result);
	}

	// --- @Version ---

	@Test
	void testGenerateVersionAnnotation() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		assertEquals("@Version", ctx.generator().generateVersionAnnotation());
	}

	// --- @Basic ---

	@Test
	void testGenerateBasicAnnotationDefault() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "name");
		assertEquals("", ctx.generator().generateBasicAnnotation(field));
	}

	// --- @Column ---

	@Test
	void testGenerateColumnAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "name");
		String result = ctx.generator().generateColumnAnnotation(field);
		assertTrue(result.contains("@Column(name = \"NAME\""), result);
		assertTrue(result.contains("nullable = false"), result);
		assertTrue(result.contains("unique = true"), result);
		assertTrue(result.contains("length = 100"), result);
	}

	@Test
	void testGenerateColumnAnnotationPrecisionScale() {
		TableDescriptor table = new TableDescriptor("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnDescriptor("PRICE", "price", java.math.BigDecimal.class)
				.precision(10).scale(2));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "price");
		String result = ctx.generator().generateColumnAnnotation(field);
		assertTrue(result.contains("precision = 10"), result);
		assertTrue(result.contains("scale = 2"), result);
	}

	// --- @Formula ---

	@Test
	void testGenerateFormulaAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "name");
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx.modelsContext());
		formula.value("first_name || ' ' || last_name");
		field.addAnnotationUsage(formula);
		String result = ctx.generator().generateFormulaAnnotation(field);
		assertTrue(result.contains("@Formula(\"first_name || ' ' || last_name\")"), result);
	}

	// --- @NaturalId ---

	@Test
	void testGenerateNaturalIdAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "email");
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		field.addAnnotationUsage(nid);
		String result = ctx.generator().generateNaturalIdAnnotation(field);
		assertEquals("@NaturalId", result);
	}

	@Test
	void testGenerateNaturalIdAnnotationMutable() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "email");
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx.modelsContext());
		nid.mutable(true);
		field.addAnnotationUsage(nid);
		String result = ctx.generator().generateNaturalIdAnnotation(field);
		assertEquals("@NaturalId(mutable = true)", result);
	}

	// --- @Lob ---

	@Test
	void testGenerateLobAnnotation() {
		TestContext ctx = create(new TableDescriptor("EMPLOYEE", "Employee", "com.example"));
		assertEquals("@Lob", ctx.generator().generateLobAnnotation());
	}

	// --- @OrderBy / @OrderColumn ---

	@Test
	void testGenerateOrderByAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "name");
		OrderByJpaAnnotation ob = JpaAnnotations.ORDER_BY.createUsage(ctx.modelsContext());
		ob.value("name ASC");
		field.addAnnotationUsage(ob);
		String result = ctx.generator().generateOrderByAnnotation(field);
		assertEquals("@OrderBy(\"name ASC\")", result);
	}

	@Test
	void testGenerateOrderColumnAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("POS", "pos", Integer.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "pos");
		OrderColumnJpaAnnotation oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx.modelsContext());
		oc.name("POS");
		field.addAnnotationUsage(oc);
		String result = ctx.generator().generateOrderColumnAnnotation(field);
		assertEquals("@OrderColumn(name = \"POS\")", result);
	}

	// --- @Filter (field-level) ---

	@Test
	void testGenerateFilterAnnotations() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "name");
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx.modelsContext());
		filter.name("activeOnly");
		filter.condition("active = true");
		field.addAnnotationUsage(filter);
		String result = ctx.generator().generateFilterAnnotations(field);
		assertTrue(result.contains("@Filter(name = \"activeOnly\""), result);
		assertTrue(result.contains("condition = \"active = true\""), result);
	}

	@Test
	void testGetFieldFiltersEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "name");
		List<FilterInfo> filters = ctx.generator().getFieldFilters(field);
		assertTrue(filters.isEmpty());
	}

	// --- @MapKey / @MapKeyColumn ---

	@Test
	void testGenerateMapKeyAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("KEY", "key", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "key");
		MapKeyJpaAnnotation mk = JpaAnnotations.MAP_KEY.createUsage(ctx.modelsContext());
		mk.name("key");
		field.addAnnotationUsage(mk);
		String result = ctx.generator().generateMapKeyAnnotation(field);
		assertEquals("@MapKey(name = \"key\")", result);
	}

	@Test
	void testGenerateMapKeyColumnAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("MAP_KEY", "mapKey", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "mapKey");
		MapKeyColumnJpaAnnotation mkc =
				JpaAnnotations.MAP_KEY_COLUMN.createUsage(ctx.modelsContext());
		mkc.name("MAP_KEY");
		field.addAnnotationUsage(mkc);
		String result = ctx.generator().generateMapKeyColumnAnnotation(field);
		assertEquals("@MapKeyColumn(name = \"MAP_KEY\")", result);
	}

	// --- @Fetch ---

	@Test
	void testGenerateFetchAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("DEPT", "dept", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "dept");
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx.modelsContext());
		fetch.value(FetchMode.SUBSELECT);
		field.addAnnotationUsage(fetch);
		String result = ctx.generator().generateFetchAnnotation(field);
		assertEquals("@Fetch(FetchMode.SUBSELECT)", result);
	}

	// --- @NotFound ---

	@Test
	void testGenerateNotFoundAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("DEPT", "dept", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "dept");
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx.modelsContext());
		nf.action(NotFoundAction.IGNORE);
		field.addAnnotationUsage(nf);
		String result = ctx.generator().generateNotFoundAnnotation(field);
		assertEquals("@NotFound(action = NotFoundAction.IGNORE)", result);
	}

	@Test
	void testGenerateNotFoundAnnotationException() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("DEPT", "dept", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "dept");
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx.modelsContext());
		nf.action(NotFoundAction.EXCEPTION);
		field.addAnnotationUsage(nf);
		String result = ctx.generator().generateNotFoundAnnotation(field);
		assertEquals("", result);
	}

	// --- @Any ---

	@Test
	void testGenerateAnyAnnotation() {
		TableDescriptor table = new TableDescriptor("PROPERTY", "Property", "com.example");
		table.addColumn(new ColumnDescriptor("VALUE_TYPE", "valueType", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "valueType");
		field.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx.modelsContext()));
		AnyDiscriminatorAnnotation ad =
				HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.modelsContext());
		ad.value(jakarta.persistence.DiscriminatorType.STRING);
		field.addAnnotationUsage(ad);
		AnyKeyJavaClassAnnotation akjc =
				HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx.modelsContext());
		akjc.value(Long.class);
		field.addAnnotationUsage(akjc);
		String result = ctx.generator().generateAnyAnnotation(field);
		assertTrue(result.contains("@Any"), result);
		assertTrue(result.contains("@AnyDiscriminator(DiscriminatorType.STRING)"), result);
		assertTrue(result.contains("@AnyKeyJavaClass(Long.class)"), result);
	}

	// --- @Convert ---

	@Test
	void testGenerateConvertAnnotationDisabled() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("STATUS", "status", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "status");
		ConvertJpaAnnotation convert = JpaAnnotations.CONVERT.createUsage(ctx.modelsContext());
		convert.disableConversion(true);
		field.addAnnotationUsage(convert);
		String result = ctx.generator().generateConvertAnnotation(field);
		assertEquals("", result);
	}

	// --- @Bag ---

	@Test
	void testGenerateBagAnnotation() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ITEMS", "items", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "items");
		field.addAnnotationUsage(HibernateAnnotations.BAG.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generateBagAnnotation(field);
		assertEquals("@Bag", result);
	}

	@Test
	void testGenerateBagAnnotationNotPresent() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ITEMS", "items", String.class));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "items");
		String result = ctx.generator().generateBagAnnotation(field);
		assertEquals("", result);
	}

	// --- @SortNatural ---

	@Test
	void testGenerateSortAnnotationNatural() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("TAGS", "tags", String.class));
		TestContext ctx = create(table);
		DynamicFieldDetails field = (DynamicFieldDetails) findField(ctx.helper(), "tags");
		field.addAnnotationUsage(HibernateAnnotations.SORT_NATURAL.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generateSortAnnotation(field);
		assertEquals("@SortNatural", result);
	}

	@Test
	void testGenerateSortAnnotationNone() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("TAGS", "tags", String.class));
		TestContext ctx = create(table);
		FieldDetails field = findField(ctx.helper(), "tags");
		String result = ctx.generator().generateSortAnnotation(field);
		assertEquals("", result);
	}
}
