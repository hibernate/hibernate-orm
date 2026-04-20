/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

class HbmCollectionMetadataApplierTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Department", "com.example.Department",
				false, null, null, ctx.getModelsContext());
	}

	private DynamicFieldDetails createOneToManyField(String name) {
		JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName(name);
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		set.setKey(key);
		JaxbHbmOneToManyCollectionElementType o2m = new JaxbHbmOneToManyCollectionElementType();
		o2m.setClazz("Employee");
		set.setOneToMany(o2m);
		HbmCollectionBuilder.processSet(entityClass, set, "com.example", ctx);
		return (DynamicFieldDetails) entityClass.getFields().get(
				entityClass.getFields().size() - 1);
	}

	// --- applyCascade ---

	@Test
	void testApplyCascadeAll() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyCascade(field, "all", ctx);
		Cascade cascade = field.getAnnotationUsage(Cascade.class, ctx.getModelsContext());
		assertNotNull(cascade);
		assertEquals(1, cascade.value().length);
		assertEquals(CascadeType.ALL, cascade.value()[0]);
	}

	@Test
	void testApplyCascadeMultiple() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyCascade(field, "persist, merge", ctx);
		Cascade cascade = field.getAnnotationUsage(Cascade.class, ctx.getModelsContext());
		assertNotNull(cascade);
		assertEquals(2, cascade.value().length);
	}

	@Test
	void testApplyCascadeNone() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyCascade(field, "none", ctx);
		assertNull(field.getAnnotationUsage(Cascade.class, ctx.getModelsContext()));
	}

	@Test
	void testApplyCascadeAllDeleteOrphan() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyCascade(field, "all-delete-orphan", ctx);
		Cascade cascade = field.getAnnotationUsage(Cascade.class, ctx.getModelsContext());
		assertNotNull(cascade);
		assertEquals(2, cascade.value().length);
		assertEquals(CascadeType.ALL, cascade.value()[0]);
		assertEquals(CascadeType.DELETE_ORPHAN, cascade.value()[1]);
	}

	// --- applyAccessAnnotation ---

	@Test
	void testApplyAccessField() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, "field", ctx);
		Access access = field.getAnnotationUsage(Access.class, ctx.getModelsContext());
		assertNotNull(access);
		assertEquals(jakarta.persistence.AccessType.FIELD, access.value());
	}

	@Test
	void testApplyAccessProperty() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, "property", ctx);
		Access access = field.getAnnotationUsage(Access.class, ctx.getModelsContext());
		assertNotNull(access);
		assertEquals(jakarta.persistence.AccessType.PROPERTY, access.value());
	}

	@Test
	void testApplyAccessNull() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, null, ctx);
		assertNull(field.getAnnotationUsage(Access.class, ctx.getModelsContext()));
	}

	// --- applyInverse ---

	@Test
	void testApplyInverseTrue() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyInverse(field, true, entityClass, ctx);
		var fieldMeta = ctx.getFieldMetaAttributes(entityClass.getClassName());
		assertNotNull(fieldMeta.get("employees"));
		assertTrue(fieldMeta.get("employees").containsKey("hibernate.inverse"));
	}

	@Test
	void testApplyInverseFalse() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyInverse(field, false, entityClass, ctx);
		var fieldMeta = ctx.getFieldMetaAttributes(entityClass.getClassName());
		assertTrue(fieldMeta.isEmpty()
				|| !fieldMeta.containsKey("employees")
				|| !fieldMeta.get("employees").containsKey("hibernate.inverse"));
	}

	// --- applyListIndex ---

	@Test
	void testApplyListIndexFromListIndex() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmListIndexType listIndex = new JaxbHbmListIndexType();
		listIndex.setColumnAttribute("POS");
		HbmCollectionMetadataApplier.applyListIndex(field, null, listIndex, ctx);
		OrderColumn oc = field.getAnnotationUsage(OrderColumn.class, ctx.getModelsContext());
		assertNotNull(oc);
		assertEquals("POS", oc.name());
	}

	@Test
	void testApplyListIndexFromIndex() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmIndexType index = new JaxbHbmIndexType();
		index.setColumnAttribute("IDX");
		HbmCollectionMetadataApplier.applyListIndex(field, index, null, ctx);
		OrderColumn oc = field.getAnnotationUsage(OrderColumn.class, ctx.getModelsContext());
		assertNotNull(oc);
		assertEquals("IDX", oc.name());
	}

	// --- applySortAnnotation ---

	@Test
	void testApplySortNatural() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applySortAnnotation(field, "natural", ctx);
		assertNotNull(field.getAnnotationUsage(SortNatural.class, ctx.getModelsContext()));
	}

	@Test
	void testApplySortUnsorted() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applySortAnnotation(field, "unsorted", ctx);
		assertNull(field.getAnnotationUsage(SortNatural.class, ctx.getModelsContext()));
	}

	// --- applyOrderBy ---

	@Test
	void testApplyOrderBy() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyOrderBy(field, "name ASC", ctx);
		SQLOrder order = field.getAnnotationUsage(SQLOrder.class, ctx.getModelsContext());
		assertNotNull(order);
		assertEquals("name ASC", order.value());
	}

	@Test
	void testApplyOrderByNull() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.applyOrderBy(field, null, ctx);
		assertNull(field.getAnnotationUsage(SQLOrder.class, ctx.getModelsContext()));
	}

	// --- addKeyJoinColumns ---

	@Test
	void testAddKeyJoinColumnsSingleAttribute() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("FK_COL");
		HbmCollectionMetadataApplier.addKeyJoinColumns(field, key, ctx);
		JoinColumn jc = field.getAnnotationUsage(JoinColumn.class, ctx.getModelsContext());
		assertNotNull(jc);
		assertEquals("FK_COL", jc.name());
	}

	@Test
	void testAddKeyJoinColumnsMultiple() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		JaxbHbmColumnType col1 = new JaxbHbmColumnType();
		col1.setName("COL_A");
		JaxbHbmColumnType col2 = new JaxbHbmColumnType();
		col2.setName("COL_B");
		key.getColumn().add(col1);
		key.getColumn().add(col2);
		HbmCollectionMetadataApplier.addKeyJoinColumns(field, key, ctx);
		JoinColumns jcs = field.getAnnotationUsage(JoinColumns.class, ctx.getModelsContext());
		assertNotNull(jcs);
		assertEquals(2, jcs.value().length);
	}

	@Test
	void testAddKeyJoinColumnsNull() {
		DynamicFieldDetails field = createOneToManyField("employees");
		HbmCollectionMetadataApplier.addKeyJoinColumns(field, null, ctx);
		// Should not throw, and no annotation added
	}

	// --- addCollectionTableFromKey ---

	@Test
	void testAddCollectionTableFromKey() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("DEPT_ID");
		HbmCollectionMetadataApplier.addCollectionTableFromKey(field, key, ctx);
		CollectionTable ct = field.getAnnotationUsage(
				CollectionTable.class, ctx.getModelsContext());
		assertNotNull(ct);
		assertEquals(1, ct.joinColumns().length);
		assertEquals("DEPT_ID", ct.joinColumns()[0].name());
	}

	// --- applyCommonMetadata (integration) ---

	@Test
	void testApplyCommonMetadataWithAll() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmCacheType cache = new JaxbHbmCacheType();
		cache.setUsage(AccessType.READ_WRITE);
		JaxbHbmFilterType filter = new JaxbHbmFilterType();
		filter.setName("active");
		filter.setCondition("active = 1");
		JaxbHbmCustomSqlDmlType sqlInsert = new JaxbHbmCustomSqlDmlType();
		sqlInsert.setValue("INSERT INTO t VALUES(?)");
		sqlInsert.setCallable(false);

		HbmCollectionMetadataApplier.applyCommonMetadata(field,
				"persist", JaxbHbmFetchStyleWithSubselectEnum.JOIN,
				null, "dept_id > 0", 5, cache,
				List.of(filter), sqlInsert, null, null, null,
				false, false, "dept_id > 0",
				"EMP_TABLE", "HR", "DB", ctx);

		assertNotNull(field.getAnnotationUsage(Cascade.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(Fetch.class, ctx.getModelsContext()));
		assertEquals(FetchMode.JOIN,
				field.getAnnotationUsage(Fetch.class, ctx.getModelsContext()).value());
		assertNotNull(field.getAnnotationUsage(SQLRestriction.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(BatchSize.class, ctx.getModelsContext()));
		assertEquals(5, field.getAnnotationUsage(BatchSize.class, ctx.getModelsContext()).size());
		assertNotNull(field.getAnnotationUsage(Cache.class, ctx.getModelsContext()));
		assertEquals(CacheConcurrencyStrategy.READ_WRITE,
				field.getAnnotationUsage(Cache.class, ctx.getModelsContext()).usage());
		assertNotNull(field.getAnnotationUsage(Filter.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(SQLInsert.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(Immutable.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(OptimisticLock.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(Check.class, ctx.getModelsContext()));
		assertNotNull(field.getAnnotationUsage(JoinTable.class, ctx.getModelsContext()));
		assertEquals("EMP_TABLE",
				field.getAnnotationUsage(JoinTable.class, ctx.getModelsContext()).name());
	}

	@Test
	void testApplyCommonMetadataMultipleFilters() {
		DynamicFieldDetails field = createOneToManyField("employees");
		JaxbHbmFilterType f1 = new JaxbHbmFilterType();
		f1.setName("active");
		f1.setCondition("active = 1");
		JaxbHbmFilterType f2 = new JaxbHbmFilterType();
		f2.setName("dept");
		f2.setCondition("dept_id = :id");

		HbmCollectionMetadataApplier.applyCommonMetadata(field,
				null, null, null, null, 0, null,
				List.of(f1, f2), null, null, null, null,
				true, true, null, null, null, null, ctx);

		Filters filters = field.getAnnotationUsage(Filters.class, ctx.getModelsContext());
		assertNotNull(filters);
		assertEquals(2, filters.value().length);
	}
}
