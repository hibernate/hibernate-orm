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
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CascadeAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionIdAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
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

/**
 * Tests for {@link HbmCollectionAttributeHelper}.
 *
 * @author Koen Aers
 */
class HbmCollectionAttributeHelperTest {

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

	private DynamicFieldDetails addSetField(
			DynamicClassDetails entity, String fieldName,
			ClassDetails elementClass, ModelsContext ctx) {
		ClassDetails setClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Set.class.getName());
		TypeDetails elementType = new ClassTypeDetailsImpl(elementClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				setClass, Collections.singletonList(elementType), null);
		return entity.applyAttribute(fieldName, fieldType, false, true, ctx);
	}

	private DynamicFieldDetails addMapField(
			DynamicClassDetails entity, String fieldName,
			ClassDetails keyClass, ClassDetails valueClass, ModelsContext ctx) {
		ClassDetails mapClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(Map.class.getName());
		TypeDetails keyType = new ClassTypeDetailsImpl(keyClass, TypeDetails.Kind.CLASS);
		TypeDetails valueType = new ClassTypeDetailsImpl(valueClass, TypeDetails.Kind.CLASS);
		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
				mapClass, List.of(keyType, valueType), null);
		return entity.applyAttribute(fieldName, fieldType, false, true, ctx);
	}

	private HbmCollectionAttributeHelper createHelper() {
		return new HbmCollectionAttributeHelper(Collections.emptyMap());
	}

	private HbmCollectionAttributeHelper createHelper(
			Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		return new HbmCollectionAttributeHelper(fieldMetaAttributes);
	}

	// --- getCollectionTag ---

	@Test
	void testGetCollectionTagDefaultSet() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		assertEquals("set", createHelper().getCollectionTag(field));
	}

	@Test
	void testGetCollectionTagFromMetaAttribute() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("items", Map.of("hibernate.collection.tag", List.of("array")));
		assertEquals("array", createHelper(fieldMeta).getCollectionTag(field));
	}

	@Test
	void testGetCollectionTagBag() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		field.addAnnotationUsage(HibernateAnnotations.BAG.createUsage(ctx));
		assertEquals("bag", createHelper().getCollectionTag(field));
	}

	@Test
	void testGetCollectionTagList() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		var oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx);
		oc.name("POS");
		field.addAnnotationUsage(oc);
		assertEquals("list", createHelper().getCollectionTag(field));
	}

	@Test
	void testGetCollectionTagIdBag() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		CollectionIdAnnotation cid = HibernateAnnotations.COLLECTION_ID.createUsage(ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("CID");
		cid.column(col);
		cid.generator("increment");
		field.addAnnotationUsage(cid);
		assertEquals("idbag", createHelper().getCollectionTag(field));
	}

	@Test
	void testGetCollectionTagMap() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails keyClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(String.class.getName());
		ClassDetails valueClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addMapField(entity, "items", keyClass, valueClass, ctx);
		assertEquals("map", createHelper().getCollectionTag(field));
	}

	// --- isCollectionInverse ---

	@Test
	void testIsCollectionInverseOneToManyMappedBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.mappedBy("parent");
		field.addAnnotationUsage(o2m);
		assertTrue(createHelper().isCollectionInverse(field));
	}

	@Test
	void testIsCollectionInverseManyToManyMappedBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx);
		m2m.mappedBy("owners");
		field.addAnnotationUsage(m2m);
		assertTrue(createHelper().isCollectionInverse(field));
	}

	@Test
	void testIsCollectionInverseFromMetaAttribute() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("items", Map.of("hibernate.inverse", List.of("true")));
		assertTrue(createHelper(fieldMeta).isCollectionInverse(field));
	}

	@Test
	void testIsCollectionInverseFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		field.addAnnotationUsage(o2m);
		assertFalse(createHelper().isCollectionInverse(field));
	}

	// --- getCollectionLazy ---

	@Test
	void testGetCollectionLazyOneToManyEager() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.fetch(FetchType.EAGER);
		field.addAnnotationUsage(o2m);
		assertEquals("false", createHelper().getCollectionLazy(field));
	}

	@Test
	void testGetCollectionLazyManyToManyEager() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx);
		m2m.fetch(FetchType.EAGER);
		field.addAnnotationUsage(m2m);
		assertEquals("false", createHelper().getCollectionLazy(field));
	}

	@Test
	void testGetCollectionLazyElementCollectionEager() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(String.class.getName());
		DynamicFieldDetails field = addSetField(entity, "tags", itemClass, ctx);
		ElementCollectionJpaAnnotation ec = JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx);
		ec.fetch(FetchType.EAGER);
		field.addAnnotationUsage(ec);
		assertEquals("false", createHelper().getCollectionLazy(field));
	}

	@Test
	void testGetCollectionLazyFromMetaAttribute() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("items", Map.of("hibernate.lazy", List.of("extra")));
		assertEquals("extra", createHelper(fieldMeta).getCollectionLazy(field));
	}

	@Test
	void testGetCollectionLazyDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionLazy(field));
	}

	// --- getCollectionFetchMode ---

	@Test
	void testGetCollectionFetchModeJoin() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.JOIN);
		field.addAnnotationUsage(fetch);
		assertEquals("join", createHelper().getCollectionFetchMode(field));
	}

	@Test
	void testGetCollectionFetchModeSelect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("select", createHelper().getCollectionFetchMode(field));
	}

	@Test
	void testGetCollectionFetchModeSubselect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		FetchAnnotation fetch = HibernateAnnotations.FETCH.createUsage(ctx);
		fetch.value(FetchMode.SUBSELECT);
		field.addAnnotationUsage(fetch);
		assertEquals("subselect", createHelper().getCollectionFetchMode(field));
	}

	@Test
	void testGetCollectionFetchModeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionFetchMode(field));
	}

	// --- getCollectionBatchSize ---

	@Test
	void testGetCollectionBatchSize() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx);
		bs.size(25);
		field.addAnnotationUsage(bs);
		assertEquals(25, createHelper().getCollectionBatchSize(field));
	}

	@Test
	void testGetCollectionBatchSizeDefault() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertEquals(0, createHelper().getCollectionBatchSize(field));
	}

	// --- getCollectionCascadeString ---

	@Test
	void testGetCollectionCascadeStringFromMetaAttribute() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("items", Map.of("hibernate.cascade", List.of("all-delete-orphan")));
		assertEquals("all-delete-orphan", createHelper(fieldMeta).getCollectionCascadeString(field));
	}

	@Test
	void testGetCollectionCascadeStringOneToMany() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx);
		o2m.cascade(new CascadeType[] { CascadeType.ALL });
		field.addAnnotationUsage(o2m);
		assertEquals("all", createHelper().getCollectionCascadeString(field));
	}

	@Test
	void testGetCollectionCascadeStringManyToMany() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx);
		m2m.cascade(new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE });
		field.addAnnotationUsage(m2m);
		assertEquals("persist, merge", createHelper().getCollectionCascadeString(field));
	}

	@Test
	void testGetCollectionCascadeStringHibernateCascade() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CascadeAnnotation cascade = HibernateAnnotations.CASCADE.createUsage(ctx);
		cascade.value(new org.hibernate.annotations.CascadeType[] {
				org.hibernate.annotations.CascadeType.ALL,
				org.hibernate.annotations.CascadeType.DELETE_ORPHAN
		});
		field.addAnnotationUsage(cascade);
		assertEquals("all, delete-orphan", createHelper().getCollectionCascadeString(field));
	}

	@Test
	void testGetCollectionCascadeStringNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionCascadeString(field));
	}

	// --- getCollectionOrderBy ---

	@Test
	void testGetCollectionOrderBy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		OrderByJpaAnnotation ob = JpaAnnotations.ORDER_BY.createUsage(ctx);
		ob.value("name ASC");
		field.addAnnotationUsage(ob);
		assertEquals("name ASC", createHelper().getCollectionOrderBy(field));
	}

	@Test
	void testGetCollectionOrderByNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionOrderBy(field));
	}

	// --- getCollectionCacheUsage / getCollectionCacheRegion ---

	@Test
	void testGetCollectionCacheUsage() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		field.addAnnotationUsage(cache);
		assertEquals("read-write", createHelper().getCollectionCacheUsage(field));
	}

	@Test
	void testGetCollectionCacheUsageNone() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONE);
		field.addAnnotationUsage(cache);
		assertNull(createHelper().getCollectionCacheUsage(field));
	}

	@Test
	void testGetCollectionCacheUsageNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionCacheUsage(field));
	}

	@Test
	void testGetCollectionCacheRegion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		cache.region("myRegion");
		field.addAnnotationUsage(cache);
		assertEquals("myRegion", createHelper().getCollectionCacheRegion(field));
	}

	@Test
	void testGetCollectionCacheRegionNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionCacheRegion(field));
	}

	// --- getCollectionFilters ---

	@Test
	void testGetCollectionFiltersSingle() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		FilterAnnotation filter = HibernateAnnotations.FILTER.createUsage(ctx);
		filter.name("activeFilter");
		filter.condition("active = true");
		field.addAnnotationUsage(filter);
		List<HbmTemplateHelper.FilterInfo> filters = createHelper().getCollectionFilters(field);
		assertEquals(1, filters.size());
		assertEquals("activeFilter", filters.get(0).name());
		assertEquals("active = true", filters.get(0).condition());
	}

	@Test
	void testGetCollectionFiltersMultiple() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		FilterAnnotation f1 = HibernateAnnotations.FILTER.createUsage(ctx);
		f1.name("f1");
		f1.condition("c1");
		FilterAnnotation f2 = HibernateAnnotations.FILTER.createUsage(ctx);
		f2.name("f2");
		f2.condition("c2");
		FiltersAnnotation filters = HibernateAnnotations.FILTERS.createUsage(ctx);
		filters.value(new Filter[] { f1, f2 });
		field.addAnnotationUsage(filters);
		List<HbmTemplateHelper.FilterInfo> result = createHelper().getCollectionFilters(field);
		assertEquals(2, result.size());
		assertEquals("f1", result.get(0).name());
		assertEquals("f2", result.get(1).name());
	}

	@Test
	void testGetCollectionFiltersNone() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertTrue(createHelper().getCollectionFilters(field).isEmpty());
	}

	// --- getListIndexColumnName ---

	@Test
	void testGetListIndexColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var oc = JpaAnnotations.ORDER_COLUMN.createUsage(ctx);
		oc.name("POS");
		field.addAnnotationUsage(oc);
		assertEquals("POS", createHelper().getListIndexColumnName(field));
	}

	@Test
	void testGetListIndexColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getListIndexColumnName(field));
	}

	// --- Map-specific ---

	@Test
	void testGetMapKeyColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var mkc = JpaAnnotations.MAP_KEY_COLUMN.createUsage(ctx);
		mkc.name("MAP_KEY");
		field.addAnnotationUsage(mkc);
		assertEquals("MAP_KEY", createHelper().getMapKeyColumnName(field));
	}

	@Test
	void testGetMapKeyColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getMapKeyColumnName(field));
	}

	@Test
	void testGetMapKeyType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails keyClass = ctx.getClassDetailsRegistry()
				.resolveClassDetails(String.class.getName());
		ClassDetails valueClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addMapField(entity, "items", keyClass, valueClass, ctx);
		assertEquals("string", createHelper().getMapKeyType(field));
	}

	@Test
	void testHasMapKeyJoinColumnTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var mkjc = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
		mkjc.name("PRODUCT_ID");
		field.addAnnotationUsage(mkjc);
		assertTrue(createHelper().hasMapKeyJoinColumn(field));
	}

	@Test
	void testHasMapKeyJoinColumnFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertFalse(createHelper().hasMapKeyJoinColumn(field));
	}

	@Test
	void testGetMapKeyJoinColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var mkjc = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage(ctx);
		mkjc.name("PRODUCT_ID");
		field.addAnnotationUsage(mkjc);
		assertEquals("PRODUCT_ID", createHelper().getMapKeyJoinColumnName(field));
	}

	@Test
	void testGetMapKeyEntityClass() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails keyClass = new DynamicClassDetails(
				"Product", "com.example.Product", false, null, null, ctx);
		ClassDetails valueClass = new DynamicClassDetails(
				"OrderItem", "com.example.OrderItem", false, null, null, ctx);
		DynamicFieldDetails field = addMapField(entity, "items", keyClass, valueClass, ctx);
		assertEquals("com.example.Product", createHelper().getMapKeyEntityClass(field));
	}

	// --- IdBag-specific ---

	@Test
	void testGetCollectionIdColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CollectionIdAnnotation cid = HibernateAnnotations.COLLECTION_ID.createUsage(ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("CID");
		cid.column(col);
		cid.generator("increment");
		field.addAnnotationUsage(cid);
		assertEquals("CID", createHelper().getCollectionIdColumnName(field));
	}

	@Test
	void testGetCollectionIdColumnNameNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionIdColumnName(field));
	}

	@Test
	void testGetCollectionIdGenerator() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		CollectionIdAnnotation cid = HibernateAnnotations.COLLECTION_ID.createUsage(ctx);
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx);
		col.name("CID");
		cid.column(col);
		cid.generator("increment");
		field.addAnnotationUsage(cid);
		assertEquals("increment", createHelper().getCollectionIdGenerator(field));
	}

	@Test
	void testGetCollectionIdGeneratorNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionIdGenerator(field));
	}

	// --- getCollectionElementType ---

	@Test
	void testGetCollectionElementType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		ClassDetails itemClass = new DynamicClassDetails(
				"Item", "com.example.Item", false, null, null, ctx);
		DynamicFieldDetails field = addSetField(entity, "items", itemClass, ctx);
		assertEquals("com.example.Item", createHelper().getCollectionElementType(field));
	}

	@Test
	void testGetCollectionElementTypeNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "name", String.class, ctx);
		assertNull(createHelper().getCollectionElementType(field));
	}

	// --- Collection SQL operations ---

	@Test
	void testGetCollectionSQLInsert() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		SQLInsertAnnotation si = HibernateAnnotations.SQL_INSERT.createUsage(ctx);
		si.sql("INSERT INTO items VALUES(?)");
		si.callable(false);
		field.addAnnotationUsage(si);
		HbmTemplateHelper.CustomSqlInfo info = createHelper().getCollectionSQLInsert(field);
		assertNotNull(info);
		assertEquals("INSERT INTO items VALUES(?)", info.sql());
		assertFalse(info.callable());
	}

	@Test
	void testGetCollectionSQLInsertNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getCollectionSQLInsert(field));
	}

	@Test
	void testGetCollectionSQLUpdate() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		SQLUpdateAnnotation su = HibernateAnnotations.SQL_UPDATE.createUsage(ctx);
		su.sql("UPDATE items SET val=?");
		su.callable(true);
		field.addAnnotationUsage(su);
		HbmTemplateHelper.CustomSqlInfo info = createHelper().getCollectionSQLUpdate(field);
		assertNotNull(info);
		assertEquals("UPDATE items SET val=?", info.sql());
		assertTrue(info.callable());
	}

	@Test
	void testGetCollectionSQLDelete() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		SQLDeleteAnnotation sd = HibernateAnnotations.SQL_DELETE.createUsage(ctx);
		sd.sql("DELETE FROM items WHERE id=?");
		sd.callable(false);
		field.addAnnotationUsage(sd);
		HbmTemplateHelper.CustomSqlInfo info = createHelper().getCollectionSQLDelete(field);
		assertNotNull(info);
		assertEquals("DELETE FROM items WHERE id=?", info.sql());
	}

	@Test
	void testGetCollectionSQLDeleteAll() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		SQLDeleteAllAnnotation sda = HibernateAnnotations.SQL_DELETE_ALL.createUsage(ctx);
		sda.sql("DELETE FROM items WHERE parent_id=?");
		sda.callable(false);
		field.addAnnotationUsage(sda);
		HbmTemplateHelper.CustomSqlInfo info = createHelper().getCollectionSQLDeleteAll(field);
		assertNotNull(info);
		assertEquals("DELETE FROM items WHERE parent_id=?", info.sql());
	}

	// --- getSort ---

	@Test
	void testGetSortNatural() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		field.addAnnotationUsage(HibernateAnnotations.SORT_NATURAL.createUsage(ctx));
		assertEquals("natural", createHelper().getSort(field));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testGetSortComparator() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		var sc = HibernateAnnotations.SORT_COMPARATOR.createUsage(ctx);
		sc.value((Class) java.text.Collator.class);
		field.addAnnotationUsage(sc);
		assertEquals("java.text.Collator", createHelper().getSort(field));
	}

	@Test
	void testGetSortNull() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DynamicFieldDetails field = addBasicField(entity, "items", String.class, ctx);
		assertNull(createHelper().getSort(field));
	}
}
