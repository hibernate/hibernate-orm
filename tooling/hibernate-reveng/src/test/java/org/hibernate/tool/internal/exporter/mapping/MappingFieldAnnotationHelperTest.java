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
package org.hibernate.tool.internal.exporter.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.NotFoundAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.GenerationType;

/**
 * Tests for {@link MappingFieldAnnotationHelper}.
 *
 * @author Koen Aers
 */
class MappingFieldAnnotationHelperTest {

	private ModelsContext createContext() {
		return new BasicModelsContextImpl(SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
	}

	private DynamicClassDetails createMinimalEntity(ModelsContext ctx) {
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(ctx));
		return entity;
	}

	private MappingFieldAnnotationHelper createHelper(DynamicClassDetails entity) {
		return new MappingFieldAnnotationHelper(entity);
	}

	private DynamicFieldDetails addBasicField(
			DynamicClassDetails entity, String name, Class<?> type, ModelsContext ctx) {
		ClassDetails typeClass = ctx.getClassDetailsRegistry().resolveClassDetails(type.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(typeClass, TypeDetails.Kind.CLASS);
		return entity.applyAttribute(name, fieldType, false, false, ctx);
	}

	private DynamicFieldDetails addOneToManyField(
			DynamicClassDetails entity, String name, ModelsContext ctx) {
		DynamicClassDetails targetClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(targetClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		return entity.applyAttribute(name, fieldType, false, true, ctx);
	}

	// --- Field categorization ---

	@Test
	void testGetCompositeIdFieldNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCompositeIdField());
	}

	@Test
	void testGetCompositeIdField() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		assertNotNull(createHelper(entity).getCompositeIdField());
	}

	@Test
	void testGetIdFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getIdFields().isEmpty());
	}

	@Test
	void testGetIdFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		assertEquals(1, createHelper(entity).getIdFields().size());
	}

	@Test
	void testGetBasicFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertEquals(1, createHelper(entity).getBasicFields().size());
	}

	@Test
	void testGetBasicFieldsExcludesId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails id = addBasicField(entity, "id", Long.class, ctx);
		id.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		addBasicField(entity, "name", String.class, ctx);
		assertEquals(1, createHelper(entity).getBasicFields().size());
	}

	@Test
	void testGetNaturalIdFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getNaturalIdFields().isEmpty());
	}

	@Test
	void testGetNaturalIdFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "email", String.class, ctx);
		field.addAnnotationUsage(HibernateAnnotations.NATURAL_ID.createUsage(ctx));
		assertEquals(1, createHelper(entity).getNaturalIdFields().size());
	}

	@Test
	void testIsNaturalIdMutableDefault() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isNaturalIdMutable());
	}

	@Test
	void testGetVersionFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getVersionFields().isEmpty());
	}

	@Test
	void testGetVersionFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "version", Integer.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.VERSION.createUsage(ctx));
		assertEquals(1, createHelper(entity).getVersionFields().size());
	}

	@Test
	void testGetManyToOneFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getManyToOneFields().isEmpty());
	}

	@Test
	void testGetManyToOneFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		assertEquals(1, createHelper(entity).getManyToOneFields().size());
	}

	@Test
	void testGetOneToManyFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getOneToManyFields().isEmpty());
	}

	@Test
	void testGetOneToOneFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getOneToOneFields().isEmpty());
	}

	@Test
	void testGetManyToManyFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getManyToManyFields().isEmpty());
	}

	@Test
	void testGetEmbeddedFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getEmbeddedFields().isEmpty());
	}

	@Test
	void testGetAnyFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getAnyFields().isEmpty());
	}

	@Test
	void testGetManyToAnyFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getManyToAnyFields().isEmpty());
	}

	@Test
	void testGetElementCollectionFieldsEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getElementCollectionFields().isEmpty());
	}

	// --- Column attributes ---

	@Test
	void testGetColumnNameDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertEquals("name", createHelper(entity).getColumnName(field));
	}

	@Test
	void testGetColumnNameCustom() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("FULL_NAME");
		field.addAnnotationUsage(col);
		assertEquals("FULL_NAME", createHelper(entity).getColumnName(field));
	}

	@Test
	void testIsNullableDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).isNullable(field));
	}

	@Test
	void testIsNullableFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.nullable(false);
		field.addAnnotationUsage(col);
		assertFalse(createHelper(entity).isNullable(field));
	}

	@Test
	void testIsUniqueDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isUnique(field));
	}

	@Test
	void testIsUniqueTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "email", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.unique(true);
		field.addAnnotationUsage(col);
		assertTrue(createHelper(entity).isUnique(field));
	}

	@Test
	void testGetLengthDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertEquals(0, createHelper(entity).getLength(field));
	}

	@Test
	void testGetLengthCustom() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.length(100);
		field.addAnnotationUsage(col);
		assertEquals(100, createHelper(entity).getLength(field));
	}

	@Test
	void testIsInsertableDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).isInsertable(field));
	}

	@Test
	void testIsUpdatableDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).isUpdatable(field));
	}

	@Test
	void testIsLobFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isLob(field));
	}

	@Test
	void testIsLobTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "content", String.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.LOB.createUsage(ctx));
		assertTrue(createHelper(entity).isLob(field));
	}

	@Test
	void testGetTemporalTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper(entity).getTemporalType(field));
	}

	@Test
	void testGetTemporalType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "created", java.util.Date.class, ctx);
		TemporalJpaAnnotation temporal = JpaAnnotations.TEMPORAL.createUsage(ctx);
		temporal.value(TemporalType.TIMESTAMP);
		field.addAnnotationUsage(temporal);
		assertEquals("TIMESTAMP", createHelper(entity).getTemporalType(field));
	}

	@Test
	void testGetGenerationTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		assertNull(createHelper(entity).getGenerationType(field));
	}

	@Test
	void testGetGenerationType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		GeneratedValueJpaAnnotation gv = JpaAnnotations.GENERATED_VALUE.createUsage(ctx);
		gv.strategy(GenerationType.SEQUENCE);
		field.addAnnotationUsage(gv);
		assertEquals("SEQUENCE", createHelper(entity).getGenerationType(field));
	}

	@Test
	void testGetGeneratorNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		assertNull(createHelper(entity).getGeneratorName(field));
	}

	@Test
	void testGetGeneratorName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		GeneratedValueJpaAnnotation gv = JpaAnnotations.GENERATED_VALUE.createUsage(ctx);
		gv.generator("myGen");
		field.addAnnotationUsage(gv);
		assertEquals("myGen", createHelper(entity).getGeneratorName(field));
	}

	@Test
	void testGetSequenceGeneratorNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		assertNull(createHelper(entity).getSequenceGenerator(field));
	}

	@Test
	void testGetSequenceGenerator() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx);
		sg.name("mySeq");
		sg.sequenceName("MY_SEQ");
		sg.allocationSize(1);
		field.addAnnotationUsage(sg);
		MappingXmlHelper.SequenceGeneratorInfo info = createHelper(entity).getSequenceGenerator(field);
		assertNotNull(info);
		assertEquals("mySeq", info.name());
		assertEquals("MY_SEQ", info.sequenceName());
		assertEquals(1, info.allocationSize());
	}

	@Test
	void testGetTableGeneratorNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", Long.class, ctx);
		assertNull(createHelper(entity).getTableGenerator(field));
	}

	// --- ManyToOne ---

	@Test
	void testGetTargetEntityName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		assertEquals("java.lang.String", createHelper(entity).getTargetEntityName(field));
	}

	@Test
	void testGetManyToOneFetchTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertNull(createHelper(entity).getManyToOneFetchType(field));
	}

	@Test
	void testGetManyToOneFetchTypeLazy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		ManyToOneJpaAnnotation m2o = JpaAnnotations.MANY_TO_ONE.createUsage(ctx);
		m2o.fetch(FetchType.LAZY);
		field.addAnnotationUsage(m2o);
		assertEquals("LAZY", createHelper(entity).getManyToOneFetchType(field));
	}

	@Test
	void testIsManyToOneOptionalDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertTrue(createHelper(entity).isManyToOneOptional(field));
	}

	// --- JoinColumn ---

	@Test
	void testGetJoinColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertNull(createHelper(entity).getJoinColumnName(field));
	}

	@Test
	void testGetJoinColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PARENT_ID");
		field.addAnnotationUsage(jc);
		assertEquals("PARENT_ID", createHelper(entity).getJoinColumnName(field));
	}

	@Test
	void testGetReferencedColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertNull(createHelper(entity).getReferencedColumnName(field));
	}

	@Test
	void testGetJoinColumnsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertTrue(createHelper(entity).getJoinColumns(field).isEmpty());
	}

	@Test
	void testGetJoinColumnsSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PARENT_ID");
		field.addAnnotationUsage(jc);
		List<MappingXmlHelper.JoinColumnInfo> cols = createHelper(entity).getJoinColumns(field);
		assertEquals(1, cols.size());
		assertEquals("PARENT_ID", cols.get(0).name());
	}

	// --- OneToMany ---

	@Test
	void testGetOneToManyTargetEntity() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		field.addAnnotationUsage(JpaAnnotations.ONE_TO_MANY.createUsage(ctx));
		assertEquals("com.example.Item", createHelper(entity).getOneToManyTargetEntity(field));
	}

	@Test
	void testGetOneToManyMappedByNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getOneToManyMappedBy(field));
	}

	@Test
	void testGetOneToManyMappedBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.mappedBy("parent");
		field.addAnnotationUsage(o2m);
		assertEquals("parent", createHelper(entity).getOneToManyMappedBy(field));
	}

	@Test
	void testGetOneToManyFetchTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getOneToManyFetchType(field));
	}

	@Test
	void testIsOneToManyOrphanRemovalDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertFalse(createHelper(entity).isOneToManyOrphanRemoval(field));
	}

	@Test
	void testGetOneToManyCascadeTypesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertTrue(createHelper(entity).getOneToManyCascadeTypes(field).isEmpty());
	}

	@Test
	void testGetOneToManyCascadeTypes() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.cascade(new CascadeType[]{CascadeType.ALL});
		field.addAnnotationUsage(o2m);
		assertEquals(1, createHelper(entity).getOneToManyCascadeTypes(field).size());
		assertEquals(CascadeType.ALL, createHelper(entity).getOneToManyCascadeTypes(field).get(0));
	}

	// --- Ordering ---

	@Test
	void testGetOrderByNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getOrderBy(field));
	}

	@Test
	void testGetOrderBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		OrderByJpaAnnotation ob = JpaAnnotations.ORDER_BY.createUsage(ctx);
		ob.value("name ASC");
		field.addAnnotationUsage(ob);
		assertEquals("name ASC", createHelper(entity).getOrderBy(field));
	}

	@Test
	void testGetOrderColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getOrderColumnName(field));
	}

	@Test
	void testGetOrderColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		OrderColumnJpaAnnotation oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx);
		oc.name("SORT_ORDER");
		field.addAnnotationUsage(oc);
		assertEquals("SORT_ORDER", createHelper(entity).getOrderColumnName(field));
	}

	// --- Map key ---

	@Test
	void testGetMapKeyNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getMapKeyName(field));
	}

	@Test
	void testGetMapKeyName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		MapKeyJpaAnnotation mk = JpaAnnotations.MAP_KEY.createUsage(ctx);
		mk.name("code");
		field.addAnnotationUsage(mk);
		assertEquals("code", createHelper(entity).getMapKeyName(field));
	}

	// --- Fetch mode ---

	@Test
	void testGetFetchModeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertNull(createHelper(entity).getFetchMode(field));
	}

	@Test
	void testGetFetchMode() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(org.hibernate.annotations.FetchMode.JOIN);
		field.addAnnotationUsage(fetch);
		assertEquals("JOIN", createHelper(entity).getFetchMode(field));
	}

	// --- NotFound ---

	@Test
	void testGetNotFoundActionNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		assertNull(createHelper(entity).getNotFoundAction(field));
	}

	@Test
	void testGetNotFoundActionIgnore() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", Object.class, ctx);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx);
		nf.action(org.hibernate.annotations.NotFoundAction.IGNORE);
		field.addAnnotationUsage(nf);
		assertEquals("IGNORE", createHelper(entity).getNotFoundAction(field));
	}

	// --- Convert ---

	@Test
	void testGetConverterClassNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "status", String.class, ctx);
		assertNull(createHelper(entity).getConverterClassName(field));
	}

	// --- OneToOne ---

	@Test
	void testGetOneToOneMappedByNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", Object.class, ctx);
		assertNull(createHelper(entity).getOneToOneMappedBy(field));
	}

	@Test
	void testGetOneToOneMappedBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", Object.class, ctx);
		OneToOneJpaAnnotation o2o = JpaAnnotations.ONE_TO_ONE.createUsage(ctx);
		o2o.mappedBy("owner");
		field.addAnnotationUsage(o2o);
		assertEquals("owner", createHelper(entity).getOneToOneMappedBy(field));
	}

	@Test
	void testGetOneToOneFetchTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", Object.class, ctx);
		assertNull(createHelper(entity).getOneToOneFetchType(field));
	}

	@Test
	void testIsOneToOneOptionalDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", Object.class, ctx);
		assertTrue(createHelper(entity).isOneToOneOptional(field));
	}

	@Test
	void testGetOneToOneCascadeTypesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "detail", Object.class, ctx);
		assertTrue(createHelper(entity).getOneToOneCascadeTypes(field).isEmpty());
	}

	// --- ManyToMany ---

	@Test
	void testGetManyToManyMappedByNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getManyToManyMappedBy(field));
	}

	@Test
	void testGetManyToManyFetchTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getManyToManyFetchType(field));
	}

	@Test
	void testGetManyToManyCascadeTypesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertTrue(createHelper(entity).getManyToManyCascadeTypes(field).isEmpty());
	}

	@Test
	void testGetJoinTableNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getJoinTableName(field));
	}

	@Test
	void testGetJoinTableName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		JoinTableJpaAnnotation jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx);
		jt.name("ENTITY_TAGS");
		field.addAnnotationUsage(jt);
		assertEquals("ENTITY_TAGS", createHelper(entity).getJoinTableName(field));
	}

	@Test
	void testGetJoinTableSchemaNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getJoinTableSchema(field));
	}

	@Test
	void testGetJoinTableCatalogNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getJoinTableCatalog(field));
	}

	// --- Embedded ---

	@Test
	void testGetAttributeOverridesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "address", Object.class, ctx);
		assertTrue(createHelper(entity).getAttributeOverrides(field).isEmpty());
	}

	// --- Property-level attributes ---

	@Test
	void testGetColumnTableNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper(entity).getColumnTable(field));
	}

	@Test
	void testGetFormulaNullDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper(entity).getFormula(field));
	}

	@Test
	void testGetFormula() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "fullName", String.class, ctx);
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx);
		formula.value("first_name || ' ' || last_name");
		field.addAnnotationUsage(formula);
		assertEquals("first_name || ' ' || last_name", createHelper(entity).getFormula(field));
	}

	@Test
	void testIsPropertyLazyDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isPropertyLazy(field));
	}

	@Test
	void testIsPropertyLazyTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "content", String.class, ctx);
		BasicJpaAnnotation basic = JpaAnnotations.BASIC.createUsage(ctx);
		basic.fetch(FetchType.LAZY);
		field.addAnnotationUsage(basic);
		assertTrue(createHelper(entity).isPropertyLazy(field));
	}

	@Test
	void testGetFieldAccessTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper(entity).getFieldAccessType(field));
	}

	@Test
	void testGetFieldAccessTypeProperty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.PROPERTY);
		field.addAnnotationUsage(access);
		assertEquals("PROPERTY", createHelper(entity).getFieldAccessType(field));
	}

	@Test
	void testIsOptimisticLockExcludedDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isOptimisticLockExcluded(field));
	}

	@Test
	void testIsOptimisticLockExcludedTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "counter", Integer.class, ctx);
		OptimisticLockAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(ctx);
		ol.excluded(true);
		field.addAnnotationUsage(ol);
		assertTrue(createHelper(entity).isOptimisticLockExcluded(field));
	}

	// --- Any ---

	@Test
	void testGetAnyDiscriminatorTypeDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "target", Object.class, ctx);
		assertEquals("STRING", createHelper(entity).getAnyDiscriminatorType(field));
	}

	@Test
	void testGetAnyKeyTypeDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "target", Object.class, ctx);
		assertEquals("java.lang.Long", createHelper(entity).getAnyKeyType(field));
	}

	@Test
	void testGetAnyDiscriminatorMappingsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "target", Object.class, ctx);
		assertTrue(createHelper(entity).getAnyDiscriminatorMappings(field).isEmpty());
	}

	// --- ElementCollection ---

	@Test
	void testGetElementCollectionTableNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getElementCollectionTableName(field));
	}

	@Test
	void testGetElementCollectionKeyColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "tags", ctx);
		assertNull(createHelper(entity).getElementCollectionKeyColumnName(field));
	}

	@Test
	void testGetElementCollectionColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "tag", String.class, ctx);
		assertNull(createHelper(entity).getElementCollectionColumnName(field));
	}

	// --- Sort ---

	@Test
	void testIsSortNaturalFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertFalse(createHelper(entity).isSortNatural(field));
	}

	@Test
	void testIsSortNaturalTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		field.addAnnotationUsage(HibernateAnnotations.SORT_NATURAL.createUsage(ctx));
		assertTrue(createHelper(entity).isSortNatural(field));
	}

	@Test
	void testGetSortComparatorClassNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addOneToManyField(entity, "items", ctx);
		assertNull(createHelper(entity).getSortComparatorClass(field));
	}
}
