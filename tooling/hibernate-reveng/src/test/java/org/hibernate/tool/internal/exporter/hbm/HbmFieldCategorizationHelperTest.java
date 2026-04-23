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
package org.hibernate.tool.internal.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AnyAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdClassJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
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
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HbmFieldCategorizationHelper}.
 *
 * @author Koen Aers
 */
class HbmFieldCategorizationHelperTest {

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

	private DynamicFieldDetails addBasicField(
			DynamicClassDetails entity, String fieldName, Class<?> javaType, ModelsContext ctx) {
		ClassDetails typeClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(javaType.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(typeClass, TypeDetails.Kind.CLASS);
		return entity.applyAttribute(fieldName, fieldType, false, false, ctx);
	}

	private DynamicFieldDetails addCollectionField(
			DynamicClassDetails entity, String fieldName, ClassDetails elementClass, ModelsContext ctx) {
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		return entity.applyAttribute(fieldName, fieldType, false, true, ctx);
	}

	private HbmFieldCategorizationHelper createHelper(DynamicClassDetails entity) {
		return new HbmFieldCategorizationHelper(entity, Collections.emptyMap(), Collections.emptyMap());
	}

	private HbmFieldCategorizationHelper createHelper(
			DynamicClassDetails entity, Map<String, Map<String, List<String>>> fieldMeta) {
		return new HbmFieldCategorizationHelper(entity, fieldMeta, Collections.emptyMap());
	}

	// --- Composite ID ---

	@Test
	void testGetCompositeIdFieldNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper(entity).getCompositeIdField());
	}

	@Test
	void testGetCompositeIdField() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		assertEquals(pkField, createHelper(entity).getCompositeIdField());
	}

	@Test
	void testGetCompositeIdClassName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		assertEquals("com.example.TestPK", createHelper(entity).getCompositeIdClassName());
	}

	@Test
	void testGetCompositeIdClassNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(createHelper(entity).getCompositeIdClassName());
	}

	@Test
	void testHasIdClassFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(createHelper(entity).hasIdClass());
	}

	@Test
	void testHasIdClassTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		IdClassJpaAnnotation idClass = JpaAnnotations.ID_CLASS.createUsage(ctx);
		idClass.value(String.class);
		entity.addAnnotationUsage(idClass);
		assertTrue(createHelper(entity).hasIdClass());
	}

	@Test
	void testGetIdClassName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		IdClassJpaAnnotation idClass = JpaAnnotations.ID_CLASS.createUsage(ctx);
		idClass.value(String.class);
		entity.addAnnotationUsage(idClass);
		assertEquals("java.lang.String", createHelper(entity).getIdClassName());
	}

	@Test
	void testGetIdClassNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertNull(createHelper(entity).getIdClassName());
	}

	@Test
	void testGetCompositeIdAllFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		addBasicField(embeddable, "k2", int.class, ctx);
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		List<FieldDetails> allFields = createHelper(entity).getCompositeIdAllFields();
		assertEquals(2, allFields.size());
	}

	@Test
	void testGetCompositeIdAllFieldsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertTrue(createHelper(entity).getCompositeIdAllFields().isEmpty());
	}

	@Test
	void testGetCompositeIdKeyProperties() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		DynamicFieldDetails m2oField = addBasicField(embeddable, "parent", String.class, ctx);
		m2oField.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		List<FieldDetails> keyProps = createHelper(entity).getCompositeIdKeyProperties();
		assertEquals(1, keyProps.size());
		assertEquals("k1", keyProps.get(0).getName());
	}

	@Test
	void testHasCompositeIdKeyManyToOnes() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		DynamicFieldDetails m2oField = addBasicField(embeddable, "parent", String.class, ctx);
		m2oField.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		assertTrue(createHelper(entity).hasCompositeIdKeyManyToOnes());
	}

	@Test
	void testHasCompositeIdKeyManyToOnesFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertFalse(createHelper(entity).hasCompositeIdKeyManyToOnes());
	}

	@Test
	void testGetCompositeIdKeyManyToOnes() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		DynamicFieldDetails m2oField = addBasicField(embeddable, "parent", String.class, ctx);
		m2oField.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		List<FieldDetails> m2os = createHelper(entity).getCompositeIdKeyManyToOnes();
		assertEquals(1, m2os.size());
		assertEquals("parent", m2os.get(0).getName());
	}

	@Test
	void testGetKeyManyToOneClassName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		assertEquals("java.lang.String", createHelper(entity).getKeyManyToOneClassName(field));
	}

	@Test
	void testGetKeyManyToOneColumnNameFromJoinColumn() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PARENT_ID");
		field.addAnnotationUsage(jc);
		assertEquals("PARENT_ID", createHelper(entity).getKeyManyToOneColumnName(field));
	}

	@Test
	void testGetKeyManyToOneColumnNameFallback() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		assertEquals("parent", createHelper(entity).getKeyManyToOneColumnName(field));
	}

	@Test
	void testGetKeyManyToOneColumnNamesSingleJoinColumn() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("PARENT_ID");
		field.addAnnotationUsage(jc);
		List<String> names = createHelper(entity).getKeyManyToOneColumnNames(field);
		assertEquals(1, names.size());
		assertEquals("PARENT_ID", names.get(0));
	}

	@Test
	void testGetKeyManyToOneColumnNamesMultipleJoinColumns() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc1.name("P_ID1");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc2.name("P_ID2");
		JoinColumnsJpaAnnotation jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx);
		jcs.value(new jakarta.persistence.JoinColumn[]{jc1, jc2});
		field.addAnnotationUsage(jcs);
		List<String> names = createHelper(entity).getKeyManyToOneColumnNames(field);
		assertEquals(2, names.size());
		assertEquals("P_ID1", names.get(0));
		assertEquals("P_ID2", names.get(1));
	}

	@Test
	void testGetKeyManyToOneColumnNamesFallback() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		List<String> names = createHelper(entity).getKeyManyToOneColumnNames(field);
		assertEquals(1, names.size());
		assertEquals("parent", names.get(0));
	}

	// --- Field lists ---

	@Test
	void testGetIdFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails idField = addBasicField(entity, "id", long.class, ctx);
		idField.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		addBasicField(entity, "name", String.class, ctx);
		List<FieldDetails> idFields = createHelper(entity).getIdFields();
		assertEquals(1, idFields.size());
		assertEquals("id", idFields.get(0).getName());
	}

	@Test
	void testGetBasicFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails idField = addBasicField(entity, "id", long.class, ctx);
		idField.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		addBasicField(entity, "name", String.class, ctx);
		addBasicField(entity, "email", String.class, ctx);
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(2, basic.size());
		assertEquals("name", basic.get(0).getName());
		assertEquals("email", basic.get(1).getName());
	}

	@Test
	void testGetBasicFieldsExcludesRelationships() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails m2o = addBasicField(entity, "parent", String.class, ctx);
		m2o.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesVersion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails version = addBasicField(entity, "version", int.class, ctx);
		version.addAnnotationUsage(JpaAnnotations.VERSION.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesNaturalId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails nid = addBasicField(entity, "isbn", String.class, ctx);
		nid.addAnnotationUsage(HibernateAnnotations.NATURAL_ID.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesSecondaryTable() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails secField = addBasicField(entity, "detail", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.table("SECONDARY_TABLE");
		secField.addAnnotationUsage(col);
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesPropertiesGroup() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		addBasicField(entity, "grouped", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("grouped", Map.of("hibernate.properties-group", List.of("myGroup")));
		List<FieldDetails> basic = createHelper(entity, fieldMeta).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesEmbedded() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails emb = addBasicField(entity, "address", String.class, ctx);
		emb.addAnnotationUsage(JpaAnnotations.EMBEDDED.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesAny() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails anyField = addBasicField(entity, "target", String.class, ctx);
		anyField.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesElementCollection() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails ecField = addBasicField(entity, "tags", String.class, ctx);
		ecField.addAnnotationUsage(JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetBasicFieldsExcludesManyToAny() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails m2aField = addBasicField(entity, "targets", String.class, ctx);
		m2aField.addAnnotationUsage(HibernateAnnotations.MANY_TO_ANY.createUsage(ctx));
		List<FieldDetails> basic = createHelper(entity).getBasicFields();
		assertEquals(1, basic.size());
		assertEquals("name", basic.get(0).getName());
	}

	@Test
	void testGetNaturalIdFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		DynamicFieldDetails nid = addBasicField(entity, "isbn", String.class, ctx);
		nid.addAnnotationUsage(HibernateAnnotations.NATURAL_ID.createUsage(ctx));
		List<FieldDetails> naturalIds = createHelper(entity).getNaturalIdFields();
		assertEquals(1, naturalIds.size());
		assertEquals("isbn", naturalIds.get(0).getName());
	}

	@Test
	void testGetNaturalIdFieldsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).getNaturalIdFields().isEmpty());
	}

	@Test
	void testIsNaturalIdMutableFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails nid = addBasicField(entity, "isbn", String.class, ctx);
		NaturalIdAnnotation nidAnn = HibernateAnnotations.NATURAL_ID.createUsage(ctx);
		nidAnn.mutable(false);
		nid.addAnnotationUsage(nidAnn);
		assertFalse(createHelper(entity).isNaturalIdMutable());
	}

	@Test
	void testIsNaturalIdMutableTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails nid = addBasicField(entity, "isbn", String.class, ctx);
		NaturalIdAnnotation nidAnn = HibernateAnnotations.NATURAL_ID.createUsage(ctx);
		nidAnn.mutable(true);
		nid.addAnnotationUsage(nidAnn);
		assertTrue(createHelper(entity).isNaturalIdMutable());
	}

	@Test
	void testIsNaturalIdMutableNoNaturalId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isNaturalIdMutable());
	}

	@Test
	void testGetVersionFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails version = addBasicField(entity, "version", int.class, ctx);
		version.addAnnotationUsage(JpaAnnotations.VERSION.createUsage(ctx));
		addBasicField(entity, "name", String.class, ctx);
		List<FieldDetails> versions = createHelper(entity).getVersionFields();
		assertEquals(1, versions.size());
		assertEquals("version", versions.get(0).getName());
	}

	@Test
	void testGetVersionFieldsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).getVersionFields().isEmpty());
	}

	@Test
	void testGetManyToOneFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails m2o = addBasicField(entity, "parent", String.class, ctx);
		m2o.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		addBasicField(entity, "name", String.class, ctx);
		List<FieldDetails> m2os = createHelper(entity).getManyToOneFields();
		assertEquals(1, m2os.size());
		assertEquals("parent", m2os.get(0).getName());
	}

	@Test
	void testGetManyToOneFieldsExcludesId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails m2o = addBasicField(entity, "parent", String.class, ctx);
		m2o.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		m2o.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		assertTrue(createHelper(entity).getManyToOneFields().isEmpty());
	}

	@Test
	void testGetManyToOneFieldsExcludesPropertiesGroup() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails m2o = addBasicField(entity, "parent", String.class, ctx);
		m2o.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("parent", Map.of("hibernate.properties-group", List.of("myGroup")));
		assertTrue(createHelper(entity, fieldMeta).getManyToOneFields().isEmpty());
	}

	@Test
	void testGetOneToOneFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails o2o = addBasicField(entity, "spouse", String.class, ctx);
		o2o.addAnnotationUsage(JpaAnnotations.ONE_TO_ONE.createUsage(ctx));
		List<FieldDetails> o2os = createHelper(entity).getOneToOneFields();
		assertEquals(1, o2os.size());
		assertEquals("spouse", o2os.get(0).getName());
	}

	@Test
	void testGetOneToOneFieldsExcludesConstrainedWithCompositeId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		DynamicFieldDetails o2o = addBasicField(entity, "spouse", String.class, ctx);
		o2o.addAnnotationUsage(JpaAnnotations.ONE_TO_ONE.createUsage(ctx));
		o2o.addAnnotationUsage(JpaAnnotations.JOIN_COLUMN.createUsage(ctx));
		assertTrue(createHelper(entity).getOneToOneFields().isEmpty());
	}

	@Test
	void testGetConstrainedOneToOneAsM2OFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails embeddable = new DynamicClassDetails(
				"TestPK", "com.example.TestPK", false, null, null, ctx);
		embeddable.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		addBasicField(embeddable, "k1", int.class, ctx);
		TypeDetails pkFieldType = new ClassTypeDetailsImpl(embeddable, TypeDetails.Kind.CLASS);
		DynamicFieldDetails pkField = entity.applyAttribute("id", pkFieldType, false, false, ctx);
		pkField.addAnnotationUsage(JpaAnnotations.EMBEDDED_ID.createUsage(ctx));
		DynamicFieldDetails o2o = addBasicField(entity, "spouse", String.class, ctx);
		o2o.addAnnotationUsage(JpaAnnotations.ONE_TO_ONE.createUsage(ctx));
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx);
		jc.name("SPOUSE_ID");
		o2o.addAnnotationUsage(jc);
		List<FieldDetails> constrained = createHelper(entity).getConstrainedOneToOneAsM2OFields();
		assertEquals(1, constrained.size());
		assertEquals("spouse", constrained.get(0).getName());
	}

	@Test
	void testGetConstrainedOneToOneAsM2OFieldsNoCompositeId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails o2o = addBasicField(entity, "spouse", String.class, ctx);
		o2o.addAnnotationUsage(JpaAnnotations.ONE_TO_ONE.createUsage(ctx));
		o2o.addAnnotationUsage(JpaAnnotations.JOIN_COLUMN.createUsage(ctx));
		assertTrue(createHelper(entity).getConstrainedOneToOneAsM2OFields().isEmpty());
	}

	@Test
	void testGetOneToManyFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails o2m = addCollectionField(entity, "items", itemClass, ctx);
		OneToManyJpaAnnotation o2mAnn = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2mAnn.mappedBy("parent");
		o2m.addAnnotationUsage(o2mAnn);
		List<FieldDetails> o2ms = createHelper(entity).getOneToManyFields();
		assertEquals(1, o2ms.size());
		assertEquals("items", o2ms.get(0).getName());
	}

	@Test
	void testGetManyToManyFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicClassDetails tagClass = new DynamicClassDetails(
				"Tag", "com.example.Tag", false, null, null, ctx);
		DynamicFieldDetails m2m = addCollectionField(entity, "tags", tagClass, ctx);
		m2m.addAnnotationUsage(JpaAnnotations.MANY_TO_MANY.createUsage(ctx));
		List<FieldDetails> m2ms = createHelper(entity).getManyToManyFields();
		assertEquals(1, m2ms.size());
		assertEquals("tags", m2ms.get(0).getName());
	}

	@Test
	void testGetEmbeddedFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails emb = addBasicField(entity, "address", String.class, ctx);
		emb.addAnnotationUsage(JpaAnnotations.EMBEDDED.createUsage(ctx));
		List<FieldDetails> embeddeds = createHelper(entity).getEmbeddedFields();
		assertEquals(1, embeddeds.size());
		assertEquals("address", embeddeds.get(0).getName());
	}

	@Test
	void testGetAnyFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails anyField = addBasicField(entity, "target", String.class, ctx);
		anyField.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx));
		List<FieldDetails> anyFields = createHelper(entity).getAnyFields();
		assertEquals(1, anyFields.size());
		assertEquals("target", anyFields.get(0).getName());
	}

	@Test
	void testGetElementCollectionFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails ecField = addBasicField(entity, "tags", String.class, ctx);
		ecField.addAnnotationUsage(JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx));
		List<FieldDetails> ecFields = createHelper(entity).getElementCollectionFields();
		assertEquals(1, ecFields.size());
		assertEquals("tags", ecFields.get(0).getName());
	}

	@Test
	void testGetManyToAnyFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails m2aField = addBasicField(entity, "targets", String.class, ctx);
		m2aField.addAnnotationUsage(HibernateAnnotations.MANY_TO_ANY.createUsage(ctx));
		List<FieldDetails> m2aFields = createHelper(entity).getManyToAnyFields();
		assertEquals(1, m2aFields.size());
		assertEquals("targets", m2aFields.get(0).getName());
	}

	// --- Dynamic component ---

	@Test
	void testGetDynamicComponentFields() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		addBasicField(entity, "dynComp", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("dynComp", Map.of("hibernate.dynamic-component", List.of("true")));
		List<FieldDetails> dynFields = createHelper(entity, fieldMeta).getDynamicComponentFields();
		assertEquals(1, dynFields.size());
		assertEquals("dynComp", dynFields.get(0).getName());
	}

	@Test
	void testGetDynamicComponentFieldsEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).getDynamicComponentFields().isEmpty());
	}

	@Test
	void testGetDynamicComponentFieldsFalseValue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "dynComp", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("dynComp", Map.of("hibernate.dynamic-component", List.of("false")));
		assertTrue(createHelper(entity, fieldMeta).getDynamicComponentFields().isEmpty());
	}

	@Test
	void testGetDynamicComponentProperties() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		addBasicField(entity, "dynComp", String.class, ctx);
		Map<String, List<String>> dynMeta = new HashMap<>();
		dynMeta.put("hibernate.dynamic-component", List.of("true"));
		dynMeta.put("hibernate.dynamic-component.property:prop1", List.of("java.lang.String"));
		dynMeta.put("hibernate.dynamic-component.property:prop2", List.of("java.lang.Integer"));
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("dynComp", dynMeta);
		FieldDetails field = entity.getFields().get(0);
		List<HbmTemplateHelper.DynamicComponentProperty> props =
				createHelper(entity, fieldMeta).getDynamicComponentProperties(field);
		assertEquals(2, props.size());
		assertTrue(props.stream().anyMatch(p -> "prop1".equals(p.name()) && "java.lang.String".equals(p.type())));
		assertTrue(props.stream().anyMatch(p -> "prop2".equals(p.name()) && "java.lang.Integer".equals(p.type())));
	}

	@Test
	void testGetDynamicComponentPropertiesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "dynComp", String.class, ctx);
		assertTrue(createHelper(entity).getDynamicComponentProperties(field).isEmpty());
	}

	// --- Field type checks ---

	@Test
	void testIsBasicFieldTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertTrue(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsBasicFieldFalseForId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "id", long.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.ID.createUsage(ctx));
		assertFalse(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsBasicFieldFalseForVersion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "version", int.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.VERSION.createUsage(ctx));
		assertFalse(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsBasicFieldFalseForManyToOne() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		assertFalse(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsBasicFieldFalseForEmbedded() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "address", String.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.EMBEDDED.createUsage(ctx));
		assertFalse(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsBasicFieldFalseForAny() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "target", String.class, ctx);
		field.addAnnotationUsage(HibernateAnnotations.ANY.createUsage(ctx));
		assertFalse(createHelper(entity).isBasicField(field));
	}

	@Test
	void testIsManyToOneFieldTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "parent", String.class, ctx);
		field.addAnnotationUsage(JpaAnnotations.MANY_TO_ONE.createUsage(ctx));
		assertTrue(createHelper(entity).isManyToOneField(field));
	}

	@Test
	void testIsManyToOneFieldFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertFalse(createHelper(entity).isManyToOneField(field));
	}
}
