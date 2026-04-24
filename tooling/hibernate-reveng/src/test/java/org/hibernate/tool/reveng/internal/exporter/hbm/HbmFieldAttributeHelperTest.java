/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.GenerationType;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CommentAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NotFoundAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;

/**
 * Tests for {@link HbmFieldAttributeHelper}.
 *
 * @author Koen Aers
 */
class HbmFieldAttributeHelperTest {

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

	private DynamicFieldDetails addField(
			DynamicClassDetails entity, String fieldName, Class<?> javaType, ModelsContext ctx) {
		ClassDetails typeClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(javaType.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(typeClass, TypeDetails.Kind.CLASS);
		return entity.applyAttribute(fieldName, fieldType, false, false, ctx);
	}

	private HbmFieldAttributeHelper createHelper() {
		return new HbmFieldAttributeHelper(Collections.emptyMap());
	}

	private HbmFieldAttributeHelper createHelper(
			Map<String, Map<String, List<String>>> fieldMeta) {
		return new HbmFieldAttributeHelper(fieldMeta);
	}

	// --- getFormula ---

	@Test
	void testGetFormula() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "total", double.class, ctx);
		FormulaAnnotation formula = HibernateAnnotations.FORMULA.createUsage(ctx);
		formula.value("price * quantity");
		field.addAnnotationUsage(formula);
		assertEquals("price * quantity", createHelper().getFormula(field));
	}

	@Test
	void testGetFormulaNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertNull(createHelper().getFormula(field));
	}

	// --- getAccessType ---

	@Test
	void testGetAccessTypeField() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.FIELD);
		field.addAnnotationUsage(access);
		assertEquals("field", createHelper().getAccessType(field));
	}

	@Test
	void testGetAccessTypeProperty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.PROPERTY);
		field.addAnnotationUsage(access);
		assertEquals("property", createHelper().getAccessType(field));
	}

	@Test
	void testGetAccessTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertNull(createHelper().getAccessType(field));
	}

	// --- getFetchMode ---

	@Test
	void testGetFetchModeJoin() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "parent", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.JOIN);
		field.addAnnotationUsage(fetch);
		assertEquals("join", createHelper().getFetchMode(field));
	}

	@Test
	void testGetFetchModeSelect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "parent", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("select", createHelper().getFetchMode(field));
	}

	@Test
	void testGetFetchModeSubselect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "parent", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SUBSELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("subselect", createHelper().getFetchMode(field));
	}

	@Test
	void testGetFetchModeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertNull(createHelper().getFetchMode(field));
	}

	// --- getNotFoundAction ---

	@Test
	void testGetNotFoundActionIgnore() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "parent", String.class, ctx);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx);
		nf.action(NotFoundAction.IGNORE);
		field.addAnnotationUsage(nf);
		assertEquals("ignore", createHelper().getNotFoundAction(field));
	}

	@Test
	void testGetNotFoundActionException() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "parent", String.class, ctx);
		NotFoundAnnotation nf = HibernateAnnotations.NOT_FOUND.createUsage(ctx);
		nf.action(NotFoundAction.EXCEPTION);
		field.addAnnotationUsage(nf);
		assertNull(createHelper().getNotFoundAction(field));
	}

	@Test
	void testGetNotFoundActionNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertNull(createHelper().getNotFoundAction(field));
	}

	// --- isTimestamp ---

	@Test
	void testIsTimestampDate() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "created", java.util.Date.class, ctx);
		assertTrue(createHelper().isTimestamp(field));
	}

	@Test
	void testIsTimestampSqlTimestamp() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "created", java.sql.Timestamp.class, ctx);
		assertTrue(createHelper().isTimestamp(field));
	}

	@Test
	void testIsTimestampCalendar() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "created", java.util.Calendar.class, ctx);
		assertTrue(createHelper().isTimestamp(field));
	}

	@Test
	void testIsTimestampInstant() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "created", java.time.Instant.class, ctx);
		assertTrue(createHelper().isTimestamp(field));
	}

	@Test
	void testIsTimestampLocalDateTime() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "created", java.time.LocalDateTime.class, ctx);
		assertTrue(createHelper().isTimestamp(field));
	}

	@Test
	void testIsTimestampFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertFalse(createHelper().isTimestamp(field));
	}

	// --- isPropertyUpdatable / isPropertyInsertable ---

	@Test
	void testIsPropertyUpdatableDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertTrue(createHelper().isPropertyUpdatable(field));
	}

	@Test
	void testIsPropertyUpdatableFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.updatable(false);
		field.addAnnotationUsage(col);
		assertFalse(createHelper().isPropertyUpdatable(field));
	}

	@Test
	void testIsPropertyInsertableDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertTrue(createHelper().isPropertyInsertable(field));
	}

	@Test
	void testIsPropertyInsertableFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.insertable(false);
		field.addAnnotationUsage(col);
		assertFalse(createHelper().isPropertyInsertable(field));
	}

	// --- isPropertyLazy ---

	@Test
	void testIsPropertyLazyTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "blob", byte[].class, ctx);
		BasicJpaAnnotation basic = JpaAnnotations.BASIC.createUsage(ctx);
		basic.fetch(FetchType.LAZY);
		field.addAnnotationUsage(basic);
		assertTrue(createHelper().isPropertyLazy(field));
	}

	@Test
	void testIsPropertyLazyFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertFalse(createHelper().isPropertyLazy(field));
	}

	@Test
	void testIsPropertyLazyEager() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		BasicJpaAnnotation basic = JpaAnnotations.BASIC.createUsage(ctx);
		basic.fetch(FetchType.EAGER);
		field.addAnnotationUsage(basic);
		assertFalse(createHelper().isPropertyLazy(field));
	}

	// --- isOptimisticLockExcluded ---

	@Test
	void testIsOptimisticLockExcludedTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "cache", String.class, ctx);
		OptimisticLockAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(ctx);
		ol.excluded(true);
		field.addAnnotationUsage(ol);
		assertTrue(createHelper().isOptimisticLockExcluded(field));
	}

	@Test
	void testIsOptimisticLockExcludedFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertFalse(createHelper().isOptimisticLockExcluded(field));
	}

	// --- getGeneratorParameters ---

	@Test
	void testGetGeneratorParametersSequence() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx);
		sg.sequenceName("MY_SEQ");
		sg.allocationSize(10);
		sg.initialValue(5);
		field.addAnnotationUsage(sg);
		Map<String, String> params = createHelper().getGeneratorParameters(field);
		assertEquals("MY_SEQ", params.get("sequence"));
		assertEquals("10", params.get("increment_size"));
		assertEquals("5", params.get("initial_value"));
	}

	@Test
	void testGetGeneratorParametersSequenceDefaults() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		SequenceGeneratorJpaAnnotation sg = JpaAnnotations.SEQUENCE_GENERATOR.createUsage(ctx);
		sg.sequenceName("MY_SEQ");
		field.addAnnotationUsage(sg);
		Map<String, String> params = createHelper().getGeneratorParameters(field);
		assertEquals("MY_SEQ", params.get("sequence"));
		assertNull(params.get("increment_size"));
		assertNull(params.get("initial_value"));
	}

	@Test
	void testGetGeneratorParametersTable() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		TableGeneratorJpaAnnotation tg = JpaAnnotations.TABLE_GENERATOR.createUsage(ctx);
		tg.table("ID_GEN");
		tg.pkColumnName("GEN_NAME");
		tg.valueColumnName("GEN_VAL");
		tg.pkColumnValue("MY_ENTITY");
		field.addAnnotationUsage(tg);
		Map<String, String> params = createHelper().getGeneratorParameters(field);
		assertEquals("ID_GEN", params.get("table"));
		assertEquals("GEN_NAME", params.get("segment_column_name"));
		assertEquals("GEN_VAL", params.get("value_column_name"));
		assertEquals("MY_ENTITY", params.get("segment_value"));
	}

	@Test
	void testGetGeneratorParametersFromMeta() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		Map<String, List<String>> idMeta = new HashMap<>();
		idMeta.put("hibernate.generator.param:property", List.of("parent"));
		fieldMeta.put("id", idMeta);
		Map<String, String> params = createHelper(fieldMeta).getGeneratorParameters(field);
		assertEquals("parent", params.get("property"));
	}

	@Test
	void testGetGeneratorParametersEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		assertTrue(createHelper().getGeneratorParameters(field).isEmpty());
	}

	// --- getGeneratorClass ---

	@Test
	void testGetGeneratorClassFromMeta() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("id", Map.of("hibernate.generator.class", List.of("foreign")));
		assertEquals("foreign", createHelper(fieldMeta).getGeneratorClass(field));
	}

	@Test
	void testGetGeneratorClassSequence() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		GeneratedValueJpaAnnotation gv = JpaAnnotations.GENERATED_VALUE.createUsage(ctx);
		gv.strategy(GenerationType.SEQUENCE);
		field.addAnnotationUsage(gv);
		assertEquals("sequence", createHelper().getGeneratorClass(field));
	}

	@Test
	void testGetGeneratorClassIdentity() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		GeneratedValueJpaAnnotation gv = JpaAnnotations.GENERATED_VALUE.createUsage(ctx);
		gv.strategy(GenerationType.IDENTITY);
		field.addAnnotationUsage(gv);
		assertEquals("identity", createHelper().getGeneratorClass(field));
	}

	@Test
	void testGetGeneratorClassAssigned() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		assertEquals("assigned", createHelper().getGeneratorClass(field));
	}

	// --- toGeneratorClass ---

	@Test
	void testToGeneratorClassNull() {
		assertEquals("assigned", createHelper().toGeneratorClass(null));
	}

	@Test
	void testToGeneratorClassAuto() {
		assertEquals("native", createHelper().toGeneratorClass(GenerationType.AUTO));
	}

	@Test
	void testToGeneratorClassTable() {
		assertEquals("table", createHelper().toGeneratorClass(GenerationType.TABLE));
	}

	@Test
	void testToGeneratorClassUuid() {
		assertEquals("uuid2", createHelper().toGeneratorClass(GenerationType.UUID));
	}

	// --- getColumnName ---

	@Test
	void testGetColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("USER_NAME");
		field.addAnnotationUsage(col);
		assertEquals("USER_NAME", createHelper().getColumnName(field));
	}

	@Test
	void testGetColumnNameFallback() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertEquals("name", createHelper().getColumnName(field));
	}

	// --- getHibernateTypeName ---

	@Test
	void testGetHibernateTypeNameString() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertEquals("string", createHelper().getHibernateTypeName(field));
	}

	@Test
	void testGetHibernateTypeNameLong() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "id", long.class, ctx);
		assertEquals("long", createHelper().getHibernateTypeName(field));
	}

	@Test
	void testGetHibernateTypeNameFromMeta() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "data", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("data", Map.of("hibernate.type.name", List.of("text")));
		assertEquals("text", createHelper(fieldMeta).getHibernateTypeName(field));
	}

	// --- hasTypeParameters / getTypeParameters ---

	@Test
	void testHasTypeParametersFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertFalse(createHelper().hasTypeParameters(field));
	}

	@Test
	void testHasTypeParametersFromMeta() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "data", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		Map<String, List<String>> dataMeta = new HashMap<>();
		dataMeta.put("hibernate.type.param:enumType", List.of("STRING"));
		fieldMeta.put("data", dataMeta);
		assertTrue(createHelper(fieldMeta).hasTypeParameters(field));
	}

	@Test
	void testGetTypeParametersFromMeta() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "data", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		Map<String, List<String>> dataMeta = new HashMap<>();
		dataMeta.put("hibernate.type.param:enumType", List.of("STRING"));
		dataMeta.put("hibernate.type.param:enumClass", List.of("com.example.Status"));
		fieldMeta.put("data", dataMeta);
		Map<String, String> params = createHelper(fieldMeta).getTypeParameters(field);
		assertEquals("STRING", params.get("enumType"));
		assertEquals("com.example.Status", params.get("enumClass"));
	}

	// --- getColumnAttributes ---

	@Test
	void testGetColumnAttributesEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertEquals("", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesNotNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.nullable(false);
		field.addAnnotationUsage(col);
		assertEquals("not-null=\"true\"", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesUnique() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "email", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.unique(true);
		field.addAnnotationUsage(col);
		assertEquals("unique=\"true\"", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesLength() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.length(100);
		field.addAnnotationUsage(col);
		assertEquals("length=\"100\"", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesDefaultLength() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.length(255);
		field.addAnnotationUsage(col);
		assertEquals("", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesPrecisionScale() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "price", double.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.precision(10);
		col.scale(2);
		field.addAnnotationUsage(col);
		assertEquals("precision=\"10\" scale=\"2\"", createHelper().getColumnAttributes(field));
	}

	@Test
	void testGetColumnAttributesCombined() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "email", String.class, ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.nullable(false);
		col.unique(true);
		col.length(100);
		field.addAnnotationUsage(col);
		String attrs = createHelper().getColumnAttributes(field);
		assertTrue(attrs.contains("not-null=\"true\""));
		assertTrue(attrs.contains("unique=\"true\""));
		assertTrue(attrs.contains("length=\"100\""));
	}

	// --- getColumnComment ---

	@Test
	void testGetColumnComment() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		CommentAnnotation comment = HibernateAnnotations.COMMENT.createUsage(ctx);
		comment.value("The user name");
		field.addAnnotationUsage(comment);
		assertEquals("The user name", createHelper().getColumnComment(field));
	}

	@Test
	void testGetColumnCommentNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		assertNull(createHelper().getColumnComment(field));
	}

	@Test
	void testGetColumnCommentEmpty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addField(entity, "name", String.class, ctx);
		CommentAnnotation comment = HibernateAnnotations.COMMENT.createUsage(ctx);
		comment.value("");
		field.addAnnotationUsage(comment);
		assertNull(createHelper().getColumnComment(field));
	}
}
