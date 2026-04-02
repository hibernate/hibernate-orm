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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;

import java.util.Collections;
import java.util.Set;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.annotations.Filter;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.models.annotations.internal.AnyAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.boot.models.annotations.internal.NotFoundAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HbmTemplateHelper}.
 *
 * @author Koen Aers
 */
public class HbmTemplateHelperTest {

	private HbmTemplateHelper create(TableMetadata table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		return new HbmTemplateHelper(classDetails);
	}

	private DynamicClassDetails createMinimalEntity(ModelsContext ctx) {
		return createMinimalEntity(ctx, false);
	}

	private DynamicClassDetails createMinimalEntity(ModelsContext ctx, boolean isAbstract) {
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				isAbstract, null, null, ctx);
		entity.addAnnotationUsage(
				org.hibernate.boot.models.JpaAnnotations.ENTITY.createUsage(ctx));
		return entity;
	}

	private DynamicFieldDetails addBasicField(
			DynamicClassDetails entity, String fieldName, Class<?> javaType, ModelsContext ctx) {
		ClassDetails typeClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(javaType.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(typeClass, TypeDetails.Kind.CLASS);
		return entity.applyAttribute(fieldName, fieldType, false, false, ctx);
	}

	private DynamicFieldDetails addElementCollectionField(
			DynamicClassDetails entity, String fieldName, Class<?> elementJavaType, ModelsContext ctx) {
		ClassDetails elementClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(elementJavaType.getName());
		return addElementCollectionField(entity, fieldName, elementClass, ctx);
	}

	private DynamicFieldDetails addElementCollectionField(
			DynamicClassDetails entity, String fieldName, ClassDetails elementClass, ModelsContext ctx) {
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

	private DynamicFieldDetails addOneToManySetField(
			DynamicClassDetails entity, String fieldName, ModelsContext ctx) {
		ClassDetails elementClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				fieldName, fieldType, false, true, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.mappedBy("parent");
		field.addAnnotationUsage(o2m);
		return field;
	}

	// --- getHibernateTypeName ---

	@Test
	public void testGetHibernateTypeNameString() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("string", helper.getHibernateTypeName(field));
	}

	@Test
	public void testGetHibernateTypeNameWrapperType() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getIdFields().get(0);
		assertEquals("java.lang.Long", helper.getHibernateTypeName(field));
	}

	@Test
	public void testGetHibernateTypeNameBigDecimal() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class));
		HbmTemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("big_decimal", helper.getHibernateTypeName(field));
	}

	// --- toGeneratorClass ---

	@Test
	public void testGetGeneratorClassIdentity() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("identity", create(table).toGeneratorClass(GenerationType.IDENTITY));
	}

	@Test
	public void testGetGeneratorClassSequence() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("sequence", create(table).toGeneratorClass(GenerationType.SEQUENCE));
	}

	@Test
	public void testGetGeneratorClassTable() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("table", create(table).toGeneratorClass(GenerationType.TABLE));
	}

	@Test
	public void testGetGeneratorClassAuto() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("native", create(table).toGeneratorClass(GenerationType.AUTO));
	}

	@Test
	public void testGetGeneratorClassUuid() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("uuid2", create(table).toGeneratorClass(GenerationType.UUID));
	}

	@Test
	public void testGetGeneratorClassNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("assigned", create(table).toGeneratorClass(null));
	}

	@Test
	public void testGetGeneratorClassFromField() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		HbmTemplateHelper helper = create(table);
		assertEquals("identity", helper.getGeneratorClass(helper.getIdFields().get(0)));
	}

	@Test
	public void testGetGeneratorClassFromFieldNoGeneration() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		HbmTemplateHelper helper = create(table);
		assertEquals("assigned", helper.getGeneratorClass(helper.getIdFields().get(0)));
	}

	// --- getClassTag ---

	@Test
	public void testGetClassTagRootEntity() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("class", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagSingleTableSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertEquals("subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagJoinedSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		assertEquals("joined-subclass", create(table).getClassTag());
	}

	@Test
	public void testGetClassTagTablePerClassSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.TABLE_PER_CLASS));
		assertEquals("union-subclass", create(table).getClassTag());
	}

	// --- getClassName ---

	@Test
	public void testGetClassName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("com.example.Employee", create(table).getClassName());
	}

	@Test
	public void testGetClassNameNoPackage() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("Employee", create(table).getClassName());
	}

	// --- getParentClassName ---

	@Test
	public void testGetParentClassName() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertEquals("com.example.Vehicle", create(table).getParentClassName());
	}

	@Test
	public void testGetParentClassNameNoParent() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertNull(create(table).getParentClassName());
	}

	// --- getColumnAttributes ---

	@Test
	public void testGetColumnAttributesAllDefaults() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		HbmTemplateHelper helper = create(table);
		assertEquals("", helper.getColumnAttributes(helper.getBasicFields().get(0)));
	}

	@Test
	public void testGetColumnAttributesNotNull() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).nullable(false));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("not-null=\"true\""));
	}

	@Test
	public void testGetColumnAttributesUnique() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class).unique(true));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("unique=\"true\""));
	}

	@Test
	public void testGetColumnAttributesLength() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).length(100));
		HbmTemplateHelper helper = create(table);
		assertTrue(helper.getColumnAttributes(helper.getBasicFields().get(0)).contains("length=\"100\""));
	}

	@Test
	public void testGetColumnAttributesPrecisionAndScale() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		HbmTemplateHelper helper = create(table);
		String attrs = helper.getColumnAttributes(helper.getBasicFields().get(0));
		assertTrue(attrs.contains("precision=\"10\""), attrs);
		assertTrue(attrs.contains("scale=\"2\""), attrs);
	}

	@Test
	public void testGetColumnAttributesCombined() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		HbmTemplateHelper helper = create(table);
		String attrs = helper.getColumnAttributes(helper.getBasicFields().get(0));
		assertTrue(attrs.contains("not-null=\"true\""), attrs);
		assertTrue(attrs.contains("unique=\"true\""), attrs);
		assertTrue(attrs.contains("length=\"100\""), attrs);
	}

	// --- getCascadeString ---

	@Test
	public void testGetCascadeStringNone() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("none", create(table).getCascadeString(null));
	}

	@Test
	public void testGetCascadeStringEmpty() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("none", create(table).getCascadeString(new CascadeType[0]));
	}

	@Test
	public void testGetCascadeStringAll() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("all", create(table).getCascadeString(new CascadeType[] { CascadeType.ALL }));
	}

	@Test
	public void testGetCascadeStringPersist() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("persist", create(table).getCascadeString(new CascadeType[] { CascadeType.PERSIST }));
	}

	@Test
	public void testGetCascadeStringMerge() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("merge", create(table).getCascadeString(new CascadeType[] { CascadeType.MERGE }));
	}

	@Test
	public void testGetCascadeStringRemove() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("delete", create(table).getCascadeString(new CascadeType[] { CascadeType.REMOVE }));
	}

	@Test
	public void testGetCascadeStringRefresh() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("refresh", create(table).getCascadeString(new CascadeType[] { CascadeType.REFRESH }));
	}

	@Test
	public void testGetCascadeStringDetach() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("evict", create(table).getCascadeString(new CascadeType[] { CascadeType.DETACH }));
	}

	@Test
	public void testGetCascadeStringMultiple() {
		TableMetadata table = new TableMetadata("T", "T", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("persist, merge", create(table).getCascadeString(
				new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE }));
	}

	// --- isSubclass ---

	@Test
	public void testIsSubclassFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).isSubclass());
	}

	@Test
	public void testIsSubclassTrue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		assertTrue(create(table).isSubclass());
	}

	// --- needsDiscriminator ---

	@Test
	public void testNeedsDiscriminatorNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorNoColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE));
		assertFalse(create(table).needsDiscriminator());
	}

	@Test
	public void testNeedsDiscriminatorWithColumn() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE"));
		assertTrue(create(table).needsDiscriminator());
	}

	// --- getDiscriminatorTypeName ---

	@Test
	public void testGetDiscriminatorTypeNameChar() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.CHAR));
		assertEquals("character", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameInteger() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER));
		assertEquals("integer", create(table).getDiscriminatorTypeName());
	}

	@Test
	public void testGetDiscriminatorTypeNameNoInheritance() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertEquals("string", create(table).getDiscriminatorTypeName());
	}

	// --- isMutable ---

	@Test
	public void testIsMutableDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).isMutable());
	}

	@Test
	public void testIsMutableFalseWhenImmutable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.IMMUTABLE.createUsage(ctx));
		assertFalse(new HbmTemplateHelper(entity).isMutable());
	}

	// --- isDynamicUpdate ---

	@Test
	public void testIsDynamicUpdateDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new HbmTemplateHelper(entity).isDynamicUpdate());
	}

	@Test
	public void testIsDynamicUpdateTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_UPDATE.createUsage(ctx));
		assertTrue(new HbmTemplateHelper(entity).isDynamicUpdate());
	}

	// --- isDynamicInsert ---

	@Test
	public void testIsDynamicInsertDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new HbmTemplateHelper(entity).isDynamicInsert());
	}

	@Test
	public void testIsDynamicInsertTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx));
		assertTrue(new HbmTemplateHelper(entity).isDynamicInsert());
	}

	// --- getBatchSize ---

	@Test
	public void testGetBatchSizeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertEquals(0, new HbmTemplateHelper(entity).getBatchSize());
	}

	@Test
	public void testGetBatchSizeSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx);
		bs.size(25);
		entity.addAnnotationUsage(bs);
		assertEquals(25, new HbmTemplateHelper(entity).getBatchSize());
	}

	// --- getCacheUsage ---

	@Test
	public void testGetCacheUsageDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getCacheUsage());
	}

	@Test
	public void testGetCacheUsageReadWrite() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		entity.addAnnotationUsage(cache);
		assertEquals("read-write", new HbmTemplateHelper(entity).getCacheUsage());
	}

	@Test
	public void testGetCacheUsageReadOnly() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_ONLY);
		entity.addAnnotationUsage(cache);
		assertEquals("read-only", new HbmTemplateHelper(entity).getCacheUsage());
	}

	@Test
	public void testGetCacheUsageNonstrictReadWrite() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONSTRICT_READ_WRITE);
		entity.addAnnotationUsage(cache);
		assertEquals("nonstrict-read-write", new HbmTemplateHelper(entity).getCacheUsage());
	}

	@Test
	public void testGetCacheUsageNoneReturnsNull() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONE);
		entity.addAnnotationUsage(cache);
		assertNull(new HbmTemplateHelper(entity).getCacheUsage());
	}

	// --- getCacheRegion ---

	@Test
	public void testGetCacheRegionDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getCacheRegion());
	}

	@Test
	public void testGetCacheRegionSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		cache.region("employee-cache");
		entity.addAnnotationUsage(cache);
		assertEquals("employee-cache", new HbmTemplateHelper(entity).getCacheRegion());
	}

	// --- getCacheInclude ---

	@Test
	public void testGetCacheIncludeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getCacheInclude());
	}

	@Test
	public void testGetCacheIncludeNonLazy() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		cache.includeLazy(false);
		entity.addAnnotationUsage(cache);
		assertEquals("non-lazy", new HbmTemplateHelper(entity).getCacheInclude());
	}

	// --- getNaturalIdFields ---

	@Test
	public void testGetNaturalIdFieldsEmpty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getNaturalIdFields().isEmpty());
	}

	@Test
	public void testGetNaturalIdFieldsPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "email", String.class, ctx);
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx);
		field.addAnnotationUsage(nid);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(1, helper.getNaturalIdFields().size());
		assertEquals("email", helper.getNaturalIdFields().get(0).getName());
		// Should not appear in basic fields
		assertFalse(helper.getBasicFields().stream()
				.anyMatch(f -> f.getName().equals("email")));
	}

	@Test
	public void testIsNaturalIdMutableDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "email", String.class, ctx);
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx);
		field.addAnnotationUsage(nid);
		assertFalse(new HbmTemplateHelper(entity).isNaturalIdMutable());
	}

	@Test
	public void testIsNaturalIdMutableTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "email", String.class, ctx);
		NaturalIdAnnotation nid = HibernateAnnotations.NATURAL_ID.createUsage(ctx);
		nid.mutable(true);
		field.addAnnotationUsage(nid);
		assertTrue(new HbmTemplateHelper(entity).isNaturalIdMutable());
	}

	// --- getWhere ---

	@Test
	public void testGetWhereDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getWhere());
	}

	@Test
	public void testGetWhereSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLRestrictionAnnotation sr = HibernateAnnotations.SQL_RESTRICTION.createUsage(ctx);
		sr.value("active = true");
		entity.addAnnotationUsage(sr);
		assertEquals("active = true", new HbmTemplateHelper(entity).getWhere());
	}

	// --- isAbstract ---

	@Test
	public void testIsAbstractDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new HbmTemplateHelper(entity).isAbstract());
	}

	@Test
	public void testIsAbstractTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx, true);
		assertTrue(new HbmTemplateHelper(entity).isAbstract());
	}

	// --- getOptimisticLockMode ---

	@Test
	public void testGetOptimisticLockModeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getOptimisticLockMode());
	}

	@Test
	public void testGetOptimisticLockModeVersion() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.VERSION);
		entity.addAnnotationUsage(ol);
		assertNull(new HbmTemplateHelper(entity).getOptimisticLockMode());
	}

	@Test
	public void testGetOptimisticLockModeAll() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.ALL);
		entity.addAnnotationUsage(ol);
		assertEquals("all", new HbmTemplateHelper(entity).getOptimisticLockMode());
	}

	@Test
	public void testGetOptimisticLockModeDirty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.DIRTY);
		entity.addAnnotationUsage(ol);
		assertEquals("dirty", new HbmTemplateHelper(entity).getOptimisticLockMode());
	}

	// --- getRowId ---

	@Test
	public void testGetRowIdDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getRowId());
	}

	@Test
	public void testGetRowIdSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		RowIdAnnotation rid = HibernateAnnotations.ROW_ID.createUsage(ctx);
		rid.value("ROWID");
		entity.addAnnotationUsage(rid);
		assertEquals("ROWID", new HbmTemplateHelper(entity).getRowId());
	}

	// --- getSubselect ---

	@Test
	public void testGetSubselectDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getSubselect());
	}

	@Test
	public void testGetSubselectSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SubselectAnnotation ss = HibernateAnnotations.SUBSELECT.createUsage(ctx);
		ss.value("select * from EMPLOYEE where active = true");
		entity.addAnnotationUsage(ss);
		assertEquals("select * from EMPLOYEE where active = true", new HbmTemplateHelper(entity).getSubselect());
	}

	// --- isConcreteProxy ---

	@Test
	public void testIsConcreteProxyDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(new HbmTemplateHelper(entity).isConcreteProxy());
	}

	@Test
	public void testIsConcreteProxyTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.CONCRETE_PROXY.createUsage(ctx));
		assertTrue(new HbmTemplateHelper(entity).isConcreteProxy());
	}

	// --- getEntityName ---

	@Test
	public void testGetEntityNameDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getEntityName());
	}

	@Test
	public void testGetEntityNameCustom() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				false, null, null, ctx);
		var entityAnnotation = org.hibernate.boot.models.JpaAnnotations.ENTITY.createUsage(ctx);
		entityAnnotation.name("CustomName");
		entity.addAnnotationUsage(entityAnnotation);
		assertEquals("CustomName", new HbmTemplateHelper(entity).getEntityName());
	}

	// --- getCollectionTag ---

	@Test
	public void testGetCollectionTagDefaultSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertEquals("set", new HbmTemplateHelper(entity).getCollectionTag(field));
	}

	@Test
	public void testGetCollectionTagBag() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		field.addAnnotationUsage(HibernateAnnotations.BAG.createUsage(ctx));
		assertEquals("bag", new HbmTemplateHelper(entity).getCollectionTag(field));
	}

	@Test
	public void testGetCollectionTagList() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		OrderColumnJpaAnnotation oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx);
		oc.name("POSITION");
		field.addAnnotationUsage(oc);
		assertEquals("list", new HbmTemplateHelper(entity).getCollectionTag(field));
	}

	// --- isCollectionInverse ---

	@Test
	public void testIsCollectionInverseWithMappedBy() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertTrue(new HbmTemplateHelper(entity).isCollectionInverse(field));
	}

	@Test
	public void testIsCollectionInverseWithoutMappedBy() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails elementClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"items", fieldType, false, true, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		field.addAnnotationUsage(o2m);
		assertFalse(new HbmTemplateHelper(entity).isCollectionInverse(field));
	}

	// --- getCollectionLazy ---

	@Test
	public void testGetCollectionLazyDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionLazy(field));
	}

	@Test
	public void testGetCollectionLazyEager() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails elementClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"items", fieldType, false, true, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.fetch(jakarta.persistence.FetchType.EAGER);
		field.addAnnotationUsage(o2m);
		assertEquals("false", new HbmTemplateHelper(entity).getCollectionLazy(field));
	}

	// --- getCollectionFetchMode ---

	@Test
	public void testGetCollectionFetchModeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionFetchMode(field));
	}

	@Test
	public void testGetCollectionFetchModeJoin() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.JOIN);
		field.addAnnotationUsage(fetch);
		assertEquals("join", new HbmTemplateHelper(entity).getCollectionFetchMode(field));
	}

	@Test
	public void testGetCollectionFetchModeSubselect() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SUBSELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("subselect", new HbmTemplateHelper(entity).getCollectionFetchMode(field));
	}

	// --- getCollectionBatchSize ---

	@Test
	public void testGetCollectionBatchSizeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertEquals(0, new HbmTemplateHelper(entity).getCollectionBatchSize(field));
	}

	@Test
	public void testGetCollectionBatchSizeSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx);
		bs.size(10);
		field.addAnnotationUsage(bs);
		assertEquals(10, new HbmTemplateHelper(entity).getCollectionBatchSize(field));
	}

	// --- getListIndexColumnName ---

	@Test
	public void testGetListIndexColumnName() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		OrderColumnJpaAnnotation oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx);
		oc.name("POSITION");
		field.addAnnotationUsage(oc);
		assertEquals("POSITION", new HbmTemplateHelper(entity).getListIndexColumnName(field));
	}

	// --- getCollectionOrderBy ---

	@Test
	public void testGetCollectionOrderByDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionOrderBy(field));
	}

	@Test
	public void testGetCollectionOrderBySet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		OrderByJpaAnnotation ob = JpaAnnotations.ORDER_BY.createUsage(ctx);
		ob.value("name ASC");
		field.addAnnotationUsage(ob);
		assertEquals("name ASC", new HbmTemplateHelper(entity).getCollectionOrderBy(field));
	}

	// --- getCollectionCascadeString ---

	@Test
	public void testGetCollectionCascadeStringDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionCascadeString(field));
	}

	@Test
	public void testGetCollectionCascadeStringWithCascade() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails elementClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"items", fieldType, false, true, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.cascade(new CascadeType[] { CascadeType.ALL });
		field.addAnnotationUsage(o2m);
		assertEquals("all", new HbmTemplateHelper(entity).getCollectionCascadeString(field));
	}

	// --- getFormula ---

	@Test
	public void testGetFormulaDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "total", BigDecimal.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getFormula(field));
	}

	@Test
	public void testGetFormulaSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "total", BigDecimal.class, ctx);
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx);
		formula.value("price * quantity");
		field.addAnnotationUsage(formula);
		assertEquals("price * quantity", new HbmTemplateHelper(entity).getFormula(field));
	}

	// --- getAccessType ---

	@Test
	public void testGetAccessTypeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getAccessType(field));
	}

	@Test
	public void testGetAccessTypeField() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.FIELD);
		field.addAnnotationUsage(access);
		assertNull(new HbmTemplateHelper(entity).getAccessType(field));
	}

	@Test
	public void testGetAccessTypeProperty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.PROPERTY);
		field.addAnnotationUsage(access);
		assertEquals("property", new HbmTemplateHelper(entity).getAccessType(field));
	}

	// --- getFetchMode ---

	@Test
	public void testGetFetchModeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getFetchMode(field));
	}

	@Test
	public void testGetFetchModeJoin() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.JOIN);
		field.addAnnotationUsage(fetch);
		assertEquals("join", new HbmTemplateHelper(entity).getFetchMode(field));
	}

	@Test
	public void testGetFetchModeSelect() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("select", new HbmTemplateHelper(entity).getFetchMode(field));
	}

	@Test
	public void testGetFetchModeSubselect() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SUBSELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("subselect", new HbmTemplateHelper(entity).getFetchMode(field));
	}

	// --- getNotFoundAction ---

	@Test
	public void testGetNotFoundActionDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getNotFoundAction(field));
	}

	@Test
	public void testGetNotFoundActionException() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx);
		nf.action(NotFoundAction.EXCEPTION);
		field.addAnnotationUsage(nf);
		assertNull(new HbmTemplateHelper(entity).getNotFoundAction(field));
	}

	@Test
	public void testGetNotFoundActionIgnore() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx);
		nf.action(NotFoundAction.IGNORE);
		field.addAnnotationUsage(nf);
		assertEquals("ignore", new HbmTemplateHelper(entity).getNotFoundAction(field));
	}

	// --- isTimestamp ---

	@Test
	public void testIsTimestampString() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampLong() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "version", Long.class, ctx);
		assertFalse(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampDate() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "updated", java.util.Date.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampSqlTimestamp() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "updated", java.sql.Timestamp.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampCalendar() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "updated", java.util.Calendar.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampInstant() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "updated", java.time.Instant.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	@Test
	public void testIsTimestampLocalDateTime() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "updated", java.time.LocalDateTime.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isTimestamp(field));
	}

	// --- getGeneratorParameters ---

	@Test
	public void testGetGeneratorParametersNoAnnotation() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		Map<String, String> params = new HbmTemplateHelper(entity).getGeneratorParameters(field);
		assertTrue(params.isEmpty());
	}

	@Test
	public void testGetGeneratorParametersSequenceGenerator() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx);
		sg.sequenceName("MY_SEQ");
		sg.allocationSize(10);
		sg.initialValue(100);
		field.addAnnotationUsage(sg);
		Map<String, String> params = new HbmTemplateHelper(entity).getGeneratorParameters(field);
		assertEquals("MY_SEQ", params.get("sequence"));
		assertEquals("10", params.get("increment_size"));
		assertEquals("100", params.get("initial_value"));
	}

	@Test
	public void testGetGeneratorParametersSequenceGeneratorDefaults() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx);
		sg.sequenceName("MY_SEQ");
		field.addAnnotationUsage(sg);
		Map<String, String> params = new HbmTemplateHelper(entity).getGeneratorParameters(field);
		assertEquals("MY_SEQ", params.get("sequence"));
		assertNull(params.get("increment_size"));
		assertNull(params.get("initial_value"));
	}

	@Test
	public void testGetGeneratorParametersTableGenerator() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		TableGeneratorJpaAnnotation tg = JpaAnnotations.TABLE_GENERATOR.createUsage(ctx);
		tg.table("ID_GEN");
		tg.pkColumnName("GEN_NAME");
		tg.valueColumnName("GEN_VAL");
		tg.pkColumnValue("MY_ENTITY");
		field.addAnnotationUsage(tg);
		Map<String, String> params = new HbmTemplateHelper(entity).getGeneratorParameters(field);
		assertEquals("ID_GEN", params.get("table"));
		assertEquals("GEN_NAME", params.get("segment_column_name"));
		assertEquals("GEN_VAL", params.get("value_column_name"));
		assertEquals("MY_ENTITY", params.get("segment_value"));
	}

	// --- getFilters ---

	@Test
	public void testGetFiltersNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getFilters().isEmpty());
	}

	@Test
	public void testGetFiltersSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("activeFilter");
		filter.condition("active = :isActive");
		entity.addAnnotationUsage(filter);
		List<HbmTemplateHelper.FilterInfo> filters = new HbmTemplateHelper(entity).getFilters();
		assertEquals(1, filters.size());
		assertEquals("activeFilter", filters.get(0).name());
		assertEquals("active = :isActive", filters.get(0).condition());
	}

	@Test
	public void testGetFiltersMultiple() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
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
		List<HbmTemplateHelper.FilterInfo> result = new HbmTemplateHelper(entity).getFilters();
		assertEquals(2, result.size());
		assertEquals("activeFilter", result.get(0).name());
		assertEquals("tenantFilter", result.get(1).name());
	}

	// --- getFilterDefs ---

	@Test
	public void testGetFilterDefsNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getFilterDefs().isEmpty());
	}

	@Test
	public void testGetFilterDefsSimple() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("activeFilter");
		fd.defaultCondition("active = true");
		entity.addAnnotationUsage(fd);
		List<HbmTemplateHelper.FilterDefInfo> defs = new HbmTemplateHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		assertEquals("activeFilter", defs.get(0).name());
		assertEquals("active = true", defs.get(0).defaultCondition());
		assertTrue(defs.get(0).parameters().isEmpty());
	}

	@Test
	public void testGetFilterDefsWithParams() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ParamDefAnnotation pd = new ParamDefAnnotation(ctx);
		pd.name("isActive");
		pd.type(Boolean.class);
		FilterDefAnnotation fd = HibernateAnnotations.FILTER_DEF.createUsage(ctx);
		fd.name("activeFilter");
		fd.defaultCondition("active = :isActive");
		fd.parameters(new org.hibernate.annotations.ParamDef[] { pd });
		entity.addAnnotationUsage(fd);
		List<HbmTemplateHelper.FilterDefInfo> defs = new HbmTemplateHelper(entity).getFilterDefs();
		assertEquals(1, defs.size());
		assertEquals("activeFilter", defs.get(0).name());
		Map<String, String> params = defs.get(0).parameters();
		assertEquals(1, params.size());
		assertEquals("java.lang.Boolean", params.get("isActive"));
	}

	// --- getCollectionFilters ---

	@Test
	public void testGetCollectionFiltersNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertTrue(new HbmTemplateHelper(entity).getCollectionFilters(field).isEmpty());
	}

	@Test
	public void testGetCollectionFiltersSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("activeFilter");
		filter.condition("active = true");
		field.addAnnotationUsage(filter);
		List<HbmTemplateHelper.FilterInfo> filters = new HbmTemplateHelper(entity).getCollectionFilters(field);
		assertEquals(1, filters.size());
		assertEquals("activeFilter", filters.get(0).name());
		assertEquals("active = true", filters.get(0).condition());
	}

	// --- getJoins ---

	@Test
	public void testGetJoinsNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getJoins().isEmpty());
	}

	@Test
	public void testGetJoinsSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("EMP_ID");
		SecondaryTableJpaAnnotation st = JpaAnnotations.SECONDARY_TABLE.createUsage(ctx);
		st.name("EMP_DETAIL");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[] { pkjc });
		entity.addAnnotationUsage(st);
		List<HbmTemplateHelper.JoinInfo> joins = new HbmTemplateHelper(entity).getJoins();
		assertEquals(1, joins.size());
		assertEquals("EMP_DETAIL", joins.get(0).tableName());
		assertEquals(1, joins.get(0).keyColumns().size());
		assertEquals("EMP_ID", joins.get(0).keyColumns().get(0));
	}

	// --- getJoinProperties ---

	@Test
	public void testGetJoinPropertiesEmpty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getJoinProperties("EMP_DETAIL").isEmpty());
	}

	@Test
	public void testGetJoinPropertiesMatchesTable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails mainField = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation mainCol = JpaAnnotations.COLUMN.createUsage(ctx);
		mainCol.name("NAME");
		mainField.addAnnotationUsage(mainCol);
		DynamicFieldDetails joinField = addBasicField(entity, "bio", String.class, ctx);
		ColumnJpaAnnotation joinCol = JpaAnnotations.COLUMN.createUsage(ctx);
		joinCol.name("BIO");
		joinCol.table("EMP_DETAIL");
		joinField.addAnnotationUsage(joinCol);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		List<FieldDetails> joinProps = helper.getJoinProperties("EMP_DETAIL");
		assertEquals(1, joinProps.size());
		assertEquals("bio", joinProps.get(0).getName());
		// main field should NOT appear in basic fields (excluded from primary table)
		// but the join field should NOT appear in basic fields either
		List<FieldDetails> basicFields = helper.getBasicFields();
		assertEquals(1, basicFields.size());
		assertEquals("name", basicFields.get(0).getName());
	}

	// --- getAnyFields ---

	@Test
	public void testGetAnyFieldsNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getAnyFields().isEmpty());
	}

	@Test
	public void testGetAnyFieldsPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		AnyAnnotation any = HibernateAnnotations.ANY.createUsage(ctx);
		field.addAnnotationUsage(any);
		List<FieldDetails> anyFields = new HbmTemplateHelper(entity).getAnyFields();
		assertEquals(1, anyFields.size());
		assertEquals("payment", anyFields.get(0).getName());
	}

	@Test
	public void testGetAnyFieldsExcludedFromBasicFields() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails nameField = addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails anyField = addBasicField(entity, "payment", Object.class, ctx);
		anyField.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx));
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(1, helper.getBasicFields().size());
		assertEquals("name", helper.getBasicFields().get(0).getName());
	}

	// --- getAnyIdType ---

	@Test
	public void testGetAnyIdTypeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		assertEquals("long", new HbmTemplateHelper(entity).getAnyIdType(field));
	}

	@Test
	public void testGetAnyIdTypeWithAnnotation() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		AnyKeyJavaClassAnnotation akjc = HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx);
		akjc.value(Long.class);
		field.addAnnotationUsage(akjc);
		assertEquals("java.lang.Long", new HbmTemplateHelper(entity).getAnyIdType(field));
	}

	// --- getAnyMetaType ---

	@Test
	public void testGetAnyMetaTypeDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		assertEquals("string", new HbmTemplateHelper(entity).getAnyMetaType(field));
	}

	@Test
	public void testGetAnyMetaTypeInteger() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		AnyDiscriminatorAnnotation ad = HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx);
		ad.value(jakarta.persistence.DiscriminatorType.INTEGER);
		field.addAnnotationUsage(ad);
		assertEquals("integer", new HbmTemplateHelper(entity).getAnyMetaType(field));
	}

	// --- getAnyMetaValues ---

	@Test
	public void testGetAnyMetaValuesNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getAnyMetaValues(field).isEmpty());
	}

	@Test
	public void testGetAnyMetaValuesWithContainer() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "payment", Object.class, ctx);
		AnyDiscriminatorValueAnnotation v1 = HibernateAnnotations.ANY_DISCRIMINATOR_VALUE.createUsage(ctx);
		v1.discriminator("CC");
		v1.entity(String.class);
		AnyDiscriminatorValueAnnotation v2 = HibernateAnnotations.ANY_DISCRIMINATOR_VALUE.createUsage(ctx);
		v2.discriminator("WI");
		v2.entity(Long.class);
		AnyDiscriminatorValuesAnnotation container = HibernateAnnotations.ANY_DISCRIMINATOR_VALUES.createUsage(ctx);
		container.value(new AnyDiscriminatorValue[] { v1, v2 });
		field.addAnnotationUsage(container);
		List<HbmTemplateHelper.AnyMetaValue> metaValues = new HbmTemplateHelper(entity).getAnyMetaValues(field);
		assertEquals(2, metaValues.size());
		assertEquals("CC", metaValues.get(0).value());
		assertEquals("java.lang.String", metaValues.get(0).entityClass());
		assertEquals("WI", metaValues.get(1).value());
		assertEquals("java.lang.Long", metaValues.get(1).entityClass());
	}

	// --- isPropertyUpdatable ---

	@Test
	public void testIsPropertyUpdatableDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isPropertyUpdatable(field));
	}

	@Test
	public void testIsPropertyUpdatableFalse() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.updatable(false);
		field.addAnnotationUsage(col);
		assertFalse(new HbmTemplateHelper(entity).isPropertyUpdatable(field));
	}

	// --- isPropertyInsertable ---

	@Test
	public void testIsPropertyInsertableDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).isPropertyInsertable(field));
	}

	@Test
	public void testIsPropertyInsertableFalse() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.insertable(false);
		field.addAnnotationUsage(col);
		assertFalse(new HbmTemplateHelper(entity).isPropertyInsertable(field));
	}

	// --- isPropertyLazy ---

	@Test
	public void testIsPropertyLazyDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(new HbmTemplateHelper(entity).isPropertyLazy(field));
	}

	@Test
	public void testIsPropertyLazyTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		BasicJpaAnnotation basic = JpaAnnotations.BASIC.createUsage(ctx);
		basic.fetch(jakarta.persistence.FetchType.LAZY);
		field.addAnnotationUsage(basic);
		assertTrue(new HbmTemplateHelper(entity).isPropertyLazy(field));
	}

	// --- isOptimisticLockExcluded ---

	@Test
	public void testIsOptimisticLockExcludedDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(new HbmTemplateHelper(entity).isOptimisticLockExcluded(field));
	}

	@Test
	public void testIsOptimisticLockExcludedTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		OptimisticLockAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(ctx);
		ol.excluded(true);
		field.addAnnotationUsage(ol);
		assertTrue(new HbmTemplateHelper(entity).isOptimisticLockExcluded(field));
	}

	// --- getElementCollectionFields ---

	@Test
	public void testGetElementCollectionFieldsNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getElementCollectionFields().isEmpty());
	}

	@Test
	public void testGetElementCollectionFieldsPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		List<FieldDetails> ecFields = new HbmTemplateHelper(entity).getElementCollectionFields();
		assertEquals(1, ecFields.size());
		assertEquals("tags", ecFields.get(0).getName());
	}

	@Test
	public void testGetElementCollectionFieldsExcludedFromBasicFields() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		addElementCollectionField(entity, "tags", String.class, ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(1, helper.getBasicFields().size());
		assertEquals("name", helper.getBasicFields().get(0).getName());
	}

	// --- getElementCollectionTableName ---

	@Test
	public void testGetElementCollectionTableNameDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getElementCollectionTableName(field));
	}

	@Test
	public void testGetElementCollectionTableNameSet() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		CollectionTableJpaAnnotation ct = JpaAnnotations.COLLECTION_TABLE.createUsage(ctx);
		ct.name("ENTITY_TAGS");
		field.addAnnotationUsage(ct);
		assertEquals("ENTITY_TAGS", new HbmTemplateHelper(entity).getElementCollectionTableName(field));
	}

	// --- getElementCollectionKeyColumnName ---

	@Test
	public void testGetElementCollectionKeyColumnName() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("ENTITY_ID");
		CollectionTableJpaAnnotation ct = JpaAnnotations.COLLECTION_TABLE.createUsage(ctx);
		ct.name("ENTITY_TAGS");
		ct.joinColumns(new jakarta.persistence.JoinColumn[] { jc });
		field.addAnnotationUsage(ct);
		assertEquals("ENTITY_ID", new HbmTemplateHelper(entity).getElementCollectionKeyColumnName(field));
	}

	// --- getElementCollectionElementType ---

	@Test
	public void testGetElementCollectionElementTypeBasic() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		assertEquals("string", new HbmTemplateHelper(entity).getElementCollectionElementType(field));
	}

	// --- isElementCollectionOfEmbeddable ---

	@Test
	public void testIsElementCollectionOfEmbeddableBasic() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		assertFalse(new HbmTemplateHelper(entity).isElementCollectionOfEmbeddable(field));
	}

	@Test
	public void testIsElementCollectionOfEmbeddableTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"Address", "com.example.Address", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		DynamicFieldDetails field = addElementCollectionField(entity, "addresses", embeddable, ctx);
		assertTrue(new HbmTemplateHelper(entity).isElementCollectionOfEmbeddable(field));
	}

	// --- getPackageName ---

	@Test
	public void testGetPackageNameWithPackage() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertEquals("com.example", new HbmTemplateHelper(entity).getPackageName());
	}

	@Test
	public void testGetPackageNameNoPackage() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "TestEntity", false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		assertNull(new HbmTemplateHelper(entity).getPackageName());
	}

	// --- getNamedQueries ---

	@Test
	public void testGetNamedQueriesNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getNamedQueries().isEmpty());
	}

	@Test
	public void testGetNamedQueriesSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findAll");
		nq.query("SELECT e FROM TestEntity e");
		entity.addAnnotationUsage(nq);
		List<HbmTemplateHelper.NamedQueryInfo> queries = new HbmTemplateHelper(entity).getNamedQueries();
		assertEquals(1, queries.size());
		assertEquals("findAll", queries.get(0).name());
		assertEquals("SELECT e FROM TestEntity e", queries.get(0).query());
	}

	@Test
	public void testGetNamedQueriesHibernateWithAttributes() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
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
		List<HbmTemplateHelper.NamedQueryInfo> queries = new HbmTemplateHelper(entity).getNamedQueries();
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

	@Test
	public void testGetNamedQueriesJpaDefaultAttributes() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedQueryJpaAnnotation nq = JpaAnnotations.NAMED_QUERY.createUsage(ctx);
		nq.name("findAll");
		nq.query("SELECT e FROM TestEntity e");
		entity.addAnnotationUsage(nq);
		List<HbmTemplateHelper.NamedQueryInfo> queries = new HbmTemplateHelper(entity).getNamedQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedQueryInfo info = queries.get(0);
		assertEquals("", info.flushMode());
		assertFalse(info.cacheable());
		assertEquals("", info.cacheRegion());
		assertEquals(-1, info.fetchSize());
		assertEquals(-1, info.timeout());
	}

	// --- getNamedNativeQueries ---

	@Test
	public void testGetNamedNativeQueriesNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getNamedNativeQueries().isEmpty());
	}

	@Test
	public void testGetNamedNativeQueriesSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findAllNative");
		nnq.query("SELECT * FROM TEST_ENTITY");
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = new HbmTemplateHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals("findAllNative", queries.get(0).name());
		assertEquals("SELECT * FROM TEST_ENTITY", queries.get(0).query());
	}

	@Test
	public void testGetNamedNativeQueriesHibernateWithAttributes() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
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
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = new HbmTemplateHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedNativeQueryInfo info = queries.get(0);
		assertEquals("findNativeActive", info.name());
		assertEquals("commit", info.flushMode());
		assertTrue(info.cacheable());
		assertEquals("nativeRegion", info.cacheRegion());
		assertEquals(50, info.fetchSize());
		assertEquals(3000, info.timeout());
		assertEquals("Native find active", info.comment());
		assertTrue(info.readOnly());
		assertEquals(2, info.querySpaces().size());
		assertEquals("TEST_ENTITY", info.querySpaces().get(0));
		assertEquals("OTHER_TABLE", info.querySpaces().get(1));
	}

	@Test
	public void testGetNamedNativeQueriesWithResultClass() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findEmployees");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultClass(String.class);
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = new HbmTemplateHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedNativeQueryInfo info = queries.get(0);
		assertEquals(1, info.entityReturns().size());
		assertEquals("java.lang.String", info.entityReturns().get(0).entityClass());
		assertTrue(info.scalarReturns().isEmpty());
	}

	@Test
	public void testGetNamedNativeQueriesWithResultSetMapping() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		// Create @SqlResultSetMapping
		SqlResultSetMappingJpaAnnotation mapping = JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(ctx);
		mapping.name("empMapping");
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(ctx);
		er.entityClass(String.class);
		FieldResultJpaAnnotation fr1 = JpaAnnotations.FIELD_RESULT.createUsage(ctx);
		fr1.name("id");
		fr1.column("EMP_ID");
		FieldResultJpaAnnotation fr2 = JpaAnnotations.FIELD_RESULT.createUsage(ctx);
		fr2.name("name");
		fr2.column("EMP_NAME");
		er.fields(new jakarta.persistence.FieldResult[]{fr1, fr2});
		mapping.entities(new jakarta.persistence.EntityResult[]{er});
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(ctx);
		cr.name("DEPT_NAME");
		mapping.columns(new jakarta.persistence.ColumnResult[]{cr});
		entity.addAnnotationUsage(mapping);
		// Create @NamedNativeQuery referencing the mapping
		NamedNativeQueryAnnotation nnq = HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findWithMapping");
		nnq.query("SELECT e.*, d.name AS DEPT_NAME FROM EMPLOYEE e JOIN DEPT d ON e.dept_id = d.id");
		nnq.resultSetMapping("empMapping");
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = new HbmTemplateHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		HbmTemplateHelper.NamedNativeQueryInfo info = queries.get(0);
		// Entity returns
		assertEquals(1, info.entityReturns().size());
		HbmTemplateHelper.EntityReturnInfo entityReturn = info.entityReturns().get(0);
		assertEquals("java.lang.String", entityReturn.entityClass());
		assertEquals(2, entityReturn.fieldMappings().size());
		assertEquals("id", entityReturn.fieldMappings().get(0).name());
		assertEquals("EMP_ID", entityReturn.fieldMappings().get(0).column());
		assertEquals("name", entityReturn.fieldMappings().get(1).name());
		assertEquals("EMP_NAME", entityReturn.fieldMappings().get(1).column());
		// Scalar returns
		assertEquals(1, info.scalarReturns().size());
		assertEquals("DEPT_NAME", info.scalarReturns().get(0).column());
	}

	@Test
	public void testGetNamedNativeQueriesJpaWithResultClass() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		NamedNativeQueryJpaAnnotation nnq = JpaAnnotations.NAMED_NATIVE_QUERY.createUsage(ctx);
		nnq.name("findJpa");
		nnq.query("SELECT * FROM EMPLOYEE");
		nnq.resultClass(String.class);
		entity.addAnnotationUsage(nnq);
		List<HbmTemplateHelper.NamedNativeQueryInfo> queries = new HbmTemplateHelper(entity).getNamedNativeQueries();
		assertEquals(1, queries.size());
		assertEquals(1, queries.get(0).entityReturns().size());
		assertEquals("java.lang.String", queries.get(0).entityReturns().get(0).entityClass());
	}

	// --- getManyToAnyFields ---

	@Test
	public void testGetManyToAnyFieldsNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getManyToAnyFields().isEmpty());
	}

	@Test
	public void testGetManyToAnyFieldsPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails elementClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"properties", fieldType, false, true, ctx);
		ManyToAnyAnnotation m2a = HibernateAnnotations.MANY_TO_ANY.createUsage(ctx);
		field.addAnnotationUsage(m2a);
		List<FieldDetails> m2aFields = new HbmTemplateHelper(entity).getManyToAnyFields();
		assertEquals(1, m2aFields.size());
		assertEquals("properties", m2aFields.get(0).getName());
	}

	@Test
	public void testGetManyToAnyFieldsExcludedFromBasicFields() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		ClassDetails elementClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"properties", fieldType, false, true, ctx);
		field.addAnnotationUsage(HibernateAnnotations.MANY_TO_ANY.createUsage(ctx));
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(1, helper.getBasicFields().size());
		assertEquals("name", helper.getBasicFields().get(0).getName());
	}

	// --- ManyToAny with join table ---

	@Test
	public void testManyToAnyWithJoinTable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails elementClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Object.class.getName());
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		DynamicFieldDetails field = entity.applyAttribute(
				"properties", fieldType, false, true, ctx);
		field.addAnnotationUsage(HibernateAnnotations.MANY_TO_ANY.createUsage(ctx));
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PROP_ID");
		field.addAnnotationUsage(jc);
		JoinColumnJpaAnnotation joinKeyCol = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		joinKeyCol.name("OWNER_ID");
		JoinTableJpaAnnotation jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("ENTITY_PROPS");
		jt.joinColumns(new jakarta.persistence.JoinColumn[] { joinKeyCol });
		field.addAnnotationUsage(jt);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("PROP_TYPE");
		field.addAnnotationUsage(col);
		AnyKeyJavaClassAnnotation akjc = HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx);
		akjc.value(Long.class);
		field.addAnnotationUsage(akjc);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals("ENTITY_PROPS", helper.getJoinTableName(field));
		assertEquals("OWNER_ID", helper.getJoinTableJoinColumnName(field));
		assertEquals("PROP_TYPE", helper.getColumnName(field));
		assertEquals("PROP_ID", helper.getJoinColumnName(field));
		assertEquals("java.lang.Long", helper.getAnyIdType(field));
		assertEquals("string", helper.getAnyMetaType(field));
	}

	// --- SQL operations ---

	@Test
	public void testGetSQLInsert() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("INSERT INTO T (name) VALUES (?)");
		entity.addAnnotationUsage(si);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getSQLInsert();
		assertNotNull(info);
		assertEquals("INSERT INTO T (name) VALUES (?)", info.sql());
		assertFalse(info.callable());
	}

	@Test
	public void testGetSQLInsertNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(new HbmTemplateHelper(entity).getSQLInsert());
	}

	@Test
	public void testGetSQLDeleteCallable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx);
		sd.sql("{call deleteEntity(?)}");
		sd.callable(true);
		entity.addAnnotationUsage(sd);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getSQLDelete();
		assertNotNull(info);
		assertTrue(info.callable());
	}

	// --- Sort ---

	@Test
	public void testGetSortNatural() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		field.addAnnotationUsage(HibernateAnnotations.SORT_NATURAL.createUsage(ctx));
		assertEquals("natural", new HbmTemplateHelper(entity).getSort(field));
	}

	@Test
	public void testGetSortComparator() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SortComparatorAnnotation sc = HibernateAnnotations.SORT_COMPARATOR.createUsage(ctx);
		sc.value(java.text.Collator.class);
		field.addAnnotationUsage(sc);
		assertEquals("java.text.Collator", new HbmTemplateHelper(entity).getSort(field));
	}

	@Test
	public void testGetSortNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getSort(field));
	}

	// --- Fetch profiles ---

	@Test
	public void testGetFetchProfilesPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		FetchProfileAnnotation fp = HibernateAnnotations.FETCH_PROFILE.createUsage(ctx);
		fp.name("eager-loading");
		fp.fetchOverrides(new org.hibernate.annotations.FetchProfile.FetchOverride[] {});
		entity.addAnnotationUsage(fp);
		List<HbmTemplateHelper.FetchProfileInfo> profiles = new HbmTemplateHelper(entity).getFetchProfiles();
		assertEquals(1, profiles.size());
		assertEquals("eager-loading", profiles.get(0).name());
	}

	@Test
	public void testGetFetchProfilesNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getFetchProfiles().isEmpty());
	}

	// --- Imports ---

	@Test
	public void testGetImportsEmpty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(new HbmTemplateHelper(entity).getImports().isEmpty());
	}

	@Test
	public void testGetImportsWithRename() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, String> imports = Map.of("com.example.Foo", "Bar");
		HbmTemplateHelper helper = new HbmTemplateHelper(entity, null, Collections.emptyMap(), imports);
		List<HbmTemplateHelper.ImportInfo> result = helper.getImports();
		assertEquals(1, result.size());
		assertEquals("com.example.Foo", result.get(0).className());
		assertEquals("Bar", result.get(0).rename());
	}

	@Test
	public void testGetImportsSameNameExcluded() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, String> imports = Map.of("com.example.Foo", "com.example.Foo");
		HbmTemplateHelper helper = new HbmTemplateHelper(entity, null, Collections.emptyMap(), imports);
		assertTrue(helper.getImports().isEmpty());
	}

	@Test
	public void testGetImportsMultiple() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, String> imports = new java.util.LinkedHashMap<>();
		imports.put("com.example.Foo", "FooAlias");
		imports.put("com.example.Bar", "BarAlias");
		imports.put("com.example.Baz", "com.example.Baz"); // same — excluded
		HbmTemplateHelper helper = new HbmTemplateHelper(entity, null, Collections.emptyMap(), imports);
		List<HbmTemplateHelper.ImportInfo> result = helper.getImports();
		assertEquals(2, result.size());
	}

	// --- Field meta-attributes ---

	@Test
	public void testGetFieldMetaAttributesEmpty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertTrue(helper.getFieldMetaAttributes(field).isEmpty());
	}

	@Test
	public void testGetFieldMetaAttributesPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"name", Map.of("default-value", List.of("N/A")));
		HbmTemplateHelper helper = new HbmTemplateHelper(
				entity, null, Collections.emptyMap(), Collections.emptyMap(), fieldMeta);
		Map<String, List<String>> attrs = helper.getFieldMetaAttributes(field);
		assertEquals(1, attrs.size());
		assertEquals(List.of("N/A"), attrs.get("default-value"));
	}

	@Test
	public void testGetFieldMetaAttributeSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"name", Map.of("scope-field", List.of("protected")));
		HbmTemplateHelper helper = new HbmTemplateHelper(
				entity, null, Collections.emptyMap(), Collections.emptyMap(), fieldMeta);
		assertEquals(List.of("protected"), helper.getFieldMetaAttribute(field, "scope-field"));
		assertTrue(helper.getFieldMetaAttribute(field, "nonexistent").isEmpty());
	}

	@Test
	public void testGetFieldMetaAttributesMultipleKeys() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tags", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"tags", Map.of("use-in-tostring", List.of("true"),
						"use-in-equals", List.of("true")));
		HbmTemplateHelper helper = new HbmTemplateHelper(
				entity, null, Collections.emptyMap(), Collections.emptyMap(), fieldMeta);
		assertEquals(List.of("true"), helper.getFieldMetaAttribute(field, "use-in-tostring"));
		assertEquals(List.of("true"), helper.getFieldMetaAttribute(field, "use-in-equals"));
	}

	@Test
	public void testGetFieldMetaAttributesDifferentFields() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails nameField = addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails ageField = addBasicField(entity, "age", int.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"name", Map.of("scope-field", List.of("protected")),
				"age", Map.of("scope-field", List.of("public")));
		HbmTemplateHelper helper = new HbmTemplateHelper(
				entity, null, Collections.emptyMap(), Collections.emptyMap(), fieldMeta);
		assertEquals(List.of("protected"), helper.getFieldMetaAttribute(nameField, "scope-field"));
		assertEquals(List.of("public"), helper.getFieldMetaAttribute(ageField, "scope-field"));
	}

	// --- Collection cache ---

	@Test
	public void testGetCollectionCacheUsage() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		field.addAnnotationUsage(cache);
		assertEquals("read-write", new HbmTemplateHelper(entity).getCollectionCacheUsage(field));
	}

	@Test
	public void testGetCollectionCacheUsageNone() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionCacheUsage(field));
	}

	@Test
	public void testGetCollectionCacheRegion() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_ONLY);
		cache.region("items-cache");
		field.addAnnotationUsage(cache);
		assertEquals("read-only", new HbmTemplateHelper(entity).getCollectionCacheUsage(field));
		assertEquals("items-cache", new HbmTemplateHelper(entity).getCollectionCacheRegion(field));
	}

	@Test
	public void testGetCollectionCacheRegionNull() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		assertNull(new HbmTemplateHelper(entity).getCollectionCacheRegion(field));
	}

	// --- Collection SQL operations ---

	@Test
	public void testGetCollectionSQLInsert() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("INSERT INTO ITEMS (parent_id, item_id) VALUES (?, ?)");
		field.addAnnotationUsage(si);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getCollectionSQLInsert(field);
		assertNotNull(info);
		assertEquals("INSERT INTO ITEMS (parent_id, item_id) VALUES (?, ?)", info.sql());
		assertFalse(info.callable());
	}

	@Test
	public void testGetCollectionSQLUpdate() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx);
		su.sql("UPDATE ITEMS SET parent_id = ? WHERE item_id = ?");
		field.addAnnotationUsage(su);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getCollectionSQLUpdate(field);
		assertNotNull(info);
		assertEquals("UPDATE ITEMS SET parent_id = ? WHERE item_id = ?", info.sql());
	}

	@Test
	public void testGetCollectionSQLDelete() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx);
		sd.sql("DELETE FROM ITEMS WHERE item_id = ?");
		field.addAnnotationUsage(sd);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getCollectionSQLDelete(field);
		assertNotNull(info);
		assertEquals("DELETE FROM ITEMS WHERE item_id = ?", info.sql());
	}

	@Test
	public void testGetCollectionSQLDeleteAll() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SQLDeleteAllAnnotation sda = HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
		sda.sql("DELETE FROM ITEMS WHERE parent_id = ?");
		field.addAnnotationUsage(sda);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getCollectionSQLDeleteAll(field);
		assertNotNull(info);
		assertEquals("DELETE FROM ITEMS WHERE parent_id = ?", info.sql());
	}

	@Test
	public void testGetCollectionSQLDeleteAllCallable() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		SQLDeleteAllAnnotation sda = HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
		sda.sql("{call deleteAllItems(?)}");
		sda.callable(true);
		field.addAnnotationUsage(sda);
		HbmTemplateHelper.CustomSqlInfo info = new HbmTemplateHelper(entity).getCollectionSQLDeleteAll(field);
		assertTrue(info.callable());
	}

	@Test
	public void testGetCollectionSQLOperationsNull() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManySetField(entity, "items", ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertNull(helper.getCollectionSQLInsert(field));
		assertNull(helper.getCollectionSQLUpdate(field));
		assertNull(helper.getCollectionSQLDelete(field));
		assertNull(helper.getCollectionSQLDeleteAll(field));
	}

	// --- Composite join columns ---

	@Test
	public void testGetJoinColumnNamesSingle() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PARENT_ID");
		field.addAnnotationUsage(jc);
		List<String> names = new HbmTemplateHelper(entity).getJoinColumnNames(field);
		assertEquals(1, names.size());
		assertEquals("PARENT_ID", names.get(0));
	}

	@Test
	public void testGetJoinColumnNamesMultiple() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc1.name("PARENT_ID1");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc2.name("PARENT_ID2");
		JoinColumnsJpaAnnotation jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx);
		jcs.value(new jakarta.persistence.JoinColumn[]{jc1, jc2});
		field.addAnnotationUsage(jcs);
		List<String> names = new HbmTemplateHelper(entity).getJoinColumnNames(field);
		assertEquals(2, names.size());
		assertEquals("PARENT_ID1", names.get(0));
		assertEquals("PARENT_ID2", names.get(1));
	}

	@Test
	public void testGetJoinColumnNamesEmpty() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		assertTrue(new HbmTemplateHelper(entity).getJoinColumnNames(field).isEmpty());
	}

	@Test
	public void testGetJoinTableJoinColumnNamesMultiple() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tags", String.class, ctx);
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc1.name("FK1");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc2.name("FK2");
		JoinColumnJpaAnnotation inv1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		inv1.name("INV1");
		JoinColumnJpaAnnotation inv2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		inv2.name("INV2");
		org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation jt =
				JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("TAG_MAP");
		jt.joinColumns(new jakarta.persistence.JoinColumn[]{jc1, jc2});
		jt.inverseJoinColumns(new jakarta.persistence.JoinColumn[]{inv1, inv2});
		field.addAnnotationUsage(jt);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		List<String> joinCols = helper.getJoinTableJoinColumnNames(field);
		assertEquals(2, joinCols.size());
		assertEquals("FK1", joinCols.get(0));
		assertEquals("FK2", joinCols.get(1));
		List<String> invCols = helper.getJoinTableInverseJoinColumnNames(field);
		assertEquals(2, invCols.size());
		assertEquals("INV1", invCols.get(0));
		assertEquals("INV2", invCols.get(1));
	}

	// --- JoinTable schema/catalog ---

	@Test
	public void testGetJoinTableSchemaDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tags", String.class, ctx);
		org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation jt =
				JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("TAG_MAP");
		field.addAnnotationUsage(jt);
		assertNull(new HbmTemplateHelper(entity).getJoinTableSchema(field));
	}

	@Test
	public void testGetJoinTableSchemaPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tags", String.class, ctx);
		org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation jt =
				JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("TAG_MAP");
		jt.schema("HR");
		field.addAnnotationUsage(jt);
		assertEquals("HR", new HbmTemplateHelper(entity).getJoinTableSchema(field));
	}

	@Test
	public void testGetJoinTableCatalogPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tags", String.class, ctx);
		org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation jt =
				JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("TAG_MAP");
		jt.catalog("MY_CATALOG");
		field.addAnnotationUsage(jt);
		assertEquals("MY_CATALOG", new HbmTemplateHelper(entity).getJoinTableCatalog(field));
	}

	// --- ElementCollection table schema/catalog ---

	@Test
	public void testGetElementCollectionTableSchemaDefault() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getElementCollectionTableSchema(field));
	}

	@Test
	public void testGetElementCollectionTableSchemaPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation ct =
				JpaAnnotations.COLLECTION_TABLE.createUsage(ctx);
		ct.name("EMPLOYEE_TAGS");
		ct.schema("HR");
		field.addAnnotationUsage(ct);
		assertEquals("HR", new HbmTemplateHelper(entity).getElementCollectionTableSchema(field));
	}

	@Test
	public void testGetElementCollectionTableCatalogPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addElementCollectionField(entity, "tags", String.class, ctx);
		org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation ct =
				JpaAnnotations.COLLECTION_TABLE.createUsage(ctx);
		ct.name("EMPLOYEE_TAGS");
		ct.catalog("MY_CATALOG");
		field.addAnnotationUsage(ct);
		assertEquals("MY_CATALOG", new HbmTemplateHelper(entity).getElementCollectionTableCatalog(field));
	}

	// --- Composite ID key-many-to-one ---

	@Test
	public void testGetCompositeIdKeyPropertiesNoManyToOne() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		// Create embeddable ID class with two basic fields
		DynamicClassDetails idClass = new DynamicClassDetails(
				"TestId", "com.example.TestId", false, null, null, ctx);
		addBasicField(idClass, "orderId", Long.class, ctx);
		addBasicField(idClass, "lineNumber", Integer.class, ctx);
		// Add @EmbeddedId field
		TypeDetails idType = new ClassTypeDetailsImpl(idClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails cidField = entity.applyAttribute("id", idType, false, false, ctx);
		cidField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(2, helper.getCompositeIdKeyProperties().size());
		assertTrue(helper.getCompositeIdKeyManyToOnes().isEmpty());
	}

	@Test
	public void testGetCompositeIdKeyManyToOnes() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		// Create embeddable ID class
		DynamicClassDetails idClass = new DynamicClassDetails(
				"TestId", "com.example.TestId", false, null, null, ctx);
		// Basic field
		addBasicField(idClass, "orderNumber", Integer.class, ctx);
		// ManyToOne field
		DynamicClassDetails customerClass = new DynamicClassDetails(
				"Customer", "com.example.Customer", false, null, null, ctx);
		TypeDetails customerType = new ClassTypeDetailsImpl(customerClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails m2oField = idClass.applyAttribute("customer", customerType, false, false, ctx);
		m2oField.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("CUSTOMER_ID");
		m2oField.addAnnotationUsage(jc);
		// Add @EmbeddedId field
		TypeDetails idType = new ClassTypeDetailsImpl(idClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails cidField = entity.applyAttribute("id", idType, false, false, ctx);
		cidField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals(1, helper.getCompositeIdKeyProperties().size());
		assertEquals("orderNumber", helper.getCompositeIdKeyProperties().get(0).getName());
		assertEquals(1, helper.getCompositeIdKeyManyToOnes().size());
		assertEquals("customer", helper.getCompositeIdKeyManyToOnes().get(0).getName());
		assertEquals("com.example.Customer", helper.getKeyManyToOneClassName(
				helper.getCompositeIdKeyManyToOnes().get(0)));
		assertEquals("CUSTOMER_ID", helper.getKeyManyToOneColumnName(
				helper.getCompositeIdKeyManyToOnes().get(0)));
	}

	// --- Property ref ---

	@Test
	public void testGetPropertyRefPresent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "department", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("DEPT_CODE");
		jc.referencedColumnName("CODE");
		field.addAnnotationUsage(jc);
		assertEquals("CODE", new HbmTemplateHelper(entity).getPropertyRef(field));
	}

	@Test
	public void testGetPropertyRefAbsent() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "department", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("DEPT_ID");
		field.addAnnotationUsage(jc);
		assertNull(new HbmTemplateHelper(entity).getPropertyRef(field));
	}

	@Test
	public void testGetPropertyRefNoJoinColumn() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(new HbmTemplateHelper(entity).getPropertyRef(field));
	}

	@Test
	public void testGetCompositeIdKeyManyToOnesNoCompositeId() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertTrue(helper.getCompositeIdKeyProperties().isEmpty());
		assertTrue(helper.getCompositeIdKeyManyToOnes().isEmpty());
	}

	// --- Custom type parameters ---

	@Test
	public void testHasTypeParametersTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "salary", java.math.BigDecimal.class, ctx);
		var typeAnn = HibernateAnnotations.TYPE.createUsage(ctx);
		typeAnn.value((Class) org.hibernate.usertype.UserType.class);
		var param = HibernateAnnotations.PARAMETER.createUsage(ctx);
		param.name("currency");
		param.value("USD");
		typeAnn.parameters(new org.hibernate.annotations.Parameter[]{param});
		field.addAnnotationUsage(typeAnn);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertTrue(helper.hasTypeParameters(field));
	}

	@Test
	public void testHasTypeParametersFalse() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertFalse(helper.hasTypeParameters(field));
	}

	@Test
	public void testGetTypeParameters() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "salary", java.math.BigDecimal.class, ctx);
		var typeAnn = HibernateAnnotations.TYPE.createUsage(ctx);
		typeAnn.value((Class) org.hibernate.usertype.UserType.class);
		var param1 = HibernateAnnotations.PARAMETER.createUsage(ctx);
		param1.name("currency");
		param1.value("USD");
		var param2 = HibernateAnnotations.PARAMETER.createUsage(ctx);
		param2.name("precision");
		param2.value("2");
		typeAnn.parameters(new org.hibernate.annotations.Parameter[]{param1, param2});
		field.addAnnotationUsage(typeAnn);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		Map<String, String> params = helper.getTypeParameters(field);
		assertEquals(2, params.size());
		assertEquals("USD", params.get("currency"));
		assertEquals("2", params.get("precision"));
	}

	@Test
	public void testGetHibernateTypeNameWithTypeAnnotation() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "salary", java.math.BigDecimal.class, ctx);
		var typeAnn = HibernateAnnotations.TYPE.createUsage(ctx);
		typeAnn.value((Class) org.hibernate.usertype.UserType.class);
		field.addAnnotationUsage(typeAnn);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals("org.hibernate.usertype.UserType", helper.getHibernateTypeName(field));
	}

	// --- composite-id mapped="true" (@IdClass) ---

	@Test
	public void testHasIdClassTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		var idClassAnn = JpaAnnotations.ID_CLASS.createUsage(ctx);
		idClassAnn.value(Object.class);
		entity.addAnnotationUsage(idClassAnn);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertTrue(helper.hasIdClass());
	}

	@Test
	public void testHasIdClassFalse() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertFalse(helper.hasIdClass());
	}

	@Test
	public void testGetIdClassName() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		var idClassAnn = JpaAnnotations.ID_CLASS.createUsage(ctx);
		idClassAnn.value(java.io.Serializable.class);
		entity.addAnnotationUsage(idClassAnn);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals("java.io.Serializable", helper.getIdClassName());
	}

	@Test
	public void testIdFieldsWithIdClass() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails orderId = addBasicField(entity, "orderId", Long.class, ctx);
		orderId.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		var col1 = JpaAnnotations.COLUMN.createUsage(ctx);
		col1.name("ORDER_ID");
		orderId.addAnnotationUsage(col1);
		DynamicFieldDetails lineNumber = addBasicField(entity, "lineNumber", Integer.class, ctx);
		lineNumber.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		var col2 = JpaAnnotations.COLUMN.createUsage(ctx);
		col2.name("LINE_NUMBER");
		lineNumber.addAnnotationUsage(col2);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		List<FieldDetails> idFields = helper.getIdFields();
		assertEquals(2, idFields.size());
		assertEquals("orderId", idFields.get(0).getName());
		assertEquals("lineNumber", idFields.get(1).getName());
	}

	// --- <map-key-many-to-many> ---

	@Test
	public void testHasMapKeyJoinColumnTrue() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails keyEntity = new DynamicClassDetails(
				"Product", "com.example.Product", false, null, null, ctx);
		DynamicClassDetails valueEntity = new DynamicClassDetails(
				"OrderItem", "com.example.OrderItem", false, null, null, ctx);
		ClassDetails mapClass = ctx.getClassDetailsRegistry().resolveClassDetails(Map.class.getName());
		TypeDetails keyType = new ClassTypeDetailsImpl(keyEntity, TypeDetails.Kind.CLASS);
		TypeDetails valueType = new ClassTypeDetailsImpl(valueEntity, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				mapClass, java.util.List.of(keyType, valueType), null);
		DynamicFieldDetails field = entity.applyAttribute("items", fieldType, false, true, ctx);
		field.addAnnotationUsage(JpaAnnotations.ONE_TO_MANY.createUsage(ctx));
		var mkjc = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
		mkjc.name("PRODUCT_ID");
		field.addAnnotationUsage(mkjc);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertTrue(helper.hasMapKeyJoinColumn(field));
	}

	@Test
	public void testHasMapKeyJoinColumnFalse() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertFalse(helper.hasMapKeyJoinColumn(field));
	}

	@Test
	public void testGetMapKeyJoinColumnName() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails keyEntity = new DynamicClassDetails(
				"Product", "com.example.Product", false, null, null, ctx);
		DynamicClassDetails valueEntity = new DynamicClassDetails(
				"OrderItem", "com.example.OrderItem", false, null, null, ctx);
		ClassDetails mapClass = ctx.getClassDetailsRegistry().resolveClassDetails(Map.class.getName());
		TypeDetails keyType = new ClassTypeDetailsImpl(keyEntity, TypeDetails.Kind.CLASS);
		TypeDetails valueType = new ClassTypeDetailsImpl(valueEntity, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				mapClass, java.util.List.of(keyType, valueType), null);
		DynamicFieldDetails field = entity.applyAttribute("items", fieldType, false, true, ctx);
		field.addAnnotationUsage(JpaAnnotations.ONE_TO_MANY.createUsage(ctx));
		var mkjc = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
		mkjc.name("PRODUCT_ID");
		field.addAnnotationUsage(mkjc);
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals("PRODUCT_ID", helper.getMapKeyJoinColumnName(field));
	}

	@Test
	public void testGetMapKeyEntityClass() {
		ModelsContext ctx = new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails keyEntity = new DynamicClassDetails(
				"Product", "com.example.Product", false, null, null, ctx);
		DynamicClassDetails valueEntity = new DynamicClassDetails(
				"OrderItem", "com.example.OrderItem", false, null, null, ctx);
		ClassDetails mapClass = ctx.getClassDetailsRegistry().resolveClassDetails(Map.class.getName());
		TypeDetails keyType = new ClassTypeDetailsImpl(keyEntity, TypeDetails.Kind.CLASS);
		TypeDetails valueType = new ClassTypeDetailsImpl(valueEntity, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				mapClass, java.util.List.of(keyType, valueType), null);
		DynamicFieldDetails field = entity.applyAttribute("items", fieldType, false, true, ctx);
		field.addAnnotationUsage(JpaAnnotations.ONE_TO_MANY.createUsage(ctx));
		HbmTemplateHelper helper = new HbmTemplateHelper(entity);
		assertEquals("com.example.Product", helper.getMapKeyEntityClass(field));
	}
}
