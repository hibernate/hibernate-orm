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

import jakarta.persistence.DiscriminatorType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.junit.jupiter.api.Test;

import jakarta.persistence.InheritanceType;

/**
 * Tests for {@link HbmClassInfoHelper}.
 *
 * @author Koen Aers
 */
class HbmClassInfoHelperTest {

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

	private HbmClassInfoHelper createHelper(DynamicClassDetails entity) {
		return new HbmClassInfoHelper(entity, null, Collections.emptyMap(), Collections.emptyMap());
	}

	private HbmClassInfoHelper createHelper(DynamicClassDetails entity, String comment) {
		return new HbmClassInfoHelper(entity, comment, Collections.emptyMap(), Collections.emptyMap());
	}

	private HbmClassInfoHelper createHelper(DynamicClassDetails entity,
											 Map<String, List<String>> metaAttributes) {
		return new HbmClassInfoHelper(entity, null, metaAttributes, Collections.emptyMap());
	}

	// --- getClassName ---

	@Test
	void testGetClassName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		assertEquals("com.example.TestEntity", createHelper(entity).getClassName());
	}

	@Test
	void testGetClassNameFromMetaAttribute() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, List<String>> meta = Map.of(
				"hibernate.class-name", List.of("com.real.RealEntity"));
		assertEquals("com.real.RealEntity", createHelper(entity, meta).getClassName());
	}

	@Test
	void testGetClassNameStripsLeadingDot() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", ".com.example.TestEntity",
				false, null, null, ctx);
		assertEquals("com.example.TestEntity", createHelper(entity).getClassName());
	}

	// --- getPackageName ---

	@Test
	void testGetPackageName() {
		ModelsContext ctx = createContext();
		assertEquals("com.example",
				createHelper(createMinimalEntity(ctx)).getPackageName());
	}

	@Test
	void testGetPackageNameNoPackage() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = new DynamicClassDetails(
				"Simple", "Simple", false, null, null, ctx);
		assertNull(createHelper(entity).getPackageName());
	}

	// --- Table ---

	@Test
	void testGetTableName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		TableJpaAnnotation table = JpaAnnotations.TABLE.createUsage(ctx);
		table.name("MY_TABLE");
		entity.addAnnotationUsage(table);
		assertEquals("MY_TABLE", createHelper(entity).getTableName());
	}

	@Test
	void testGetTableNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getTableName());
	}

	@Test
	void testGetSchema() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		TableJpaAnnotation table = JpaAnnotations.TABLE.createUsage(ctx);
		table.name("T");
		table.schema("MY_SCHEMA");
		entity.addAnnotationUsage(table);
		assertEquals("MY_SCHEMA", createHelper(entity).getSchema());
	}

	@Test
	void testGetSchemaNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSchema());
	}

	@Test
	void testGetCatalog() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		TableJpaAnnotation table = JpaAnnotations.TABLE.createUsage(ctx);
		table.name("T");
		table.catalog("MY_CATALOG");
		entity.addAnnotationUsage(table);
		assertEquals("MY_CATALOG", createHelper(entity).getCatalog());
	}

	@Test
	void testGetCatalogNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCatalog());
	}

	// --- getComment ---

	@Test
	void testGetComment() {
		ModelsContext ctx = createContext();
		assertEquals("my comment",
				createHelper(createMinimalEntity(ctx), "my comment").getComment());
	}

	@Test
	void testGetCommentNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getComment());
	}

	// --- Class-level attributes ---

	@Test
	void testIsMutableDefault() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).isMutable());
	}

	@Test
	void testIsMutableFalse() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.IMMUTABLE.createUsage(ctx));
		assertFalse(createHelper(entity).isMutable());
	}

	@Test
	void testIsDynamicUpdate() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_UPDATE.createUsage(ctx));
		assertTrue(createHelper(entity).isDynamicUpdate());
	}

	@Test
	void testIsDynamicUpdateFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isDynamicUpdate());
	}

	@Test
	void testIsDynamicInsert() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx));
		assertTrue(createHelper(entity).isDynamicInsert());
	}

	@Test
	void testIsDynamicInsertFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isDynamicInsert());
	}

	@Test
	void testGetBatchSize() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		BatchSizeAnnotation bs = HibernateAnnotations.BATCH_SIZE.createUsage(ctx);
		bs.size(25);
		entity.addAnnotationUsage(bs);
		assertEquals(25, createHelper(entity).getBatchSize());
	}

	@Test
	void testGetBatchSizeDefault() {
		ModelsContext ctx = createContext();
		assertEquals(0, createHelper(createMinimalEntity(ctx)).getBatchSize());
	}

	// --- Cache ---

	@Test
	void testGetCacheUsageReadWrite() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		entity.addAnnotationUsage(cache);
		assertEquals("read-write", createHelper(entity).getCacheUsage());
	}

	@Test
	void testGetCacheUsageNonStrictReadWrite() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONSTRICT_READ_WRITE);
		entity.addAnnotationUsage(cache);
		assertEquals("nonstrict-read-write", createHelper(entity).getCacheUsage());
	}

	@Test
	void testGetCacheUsageNone() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONE);
		entity.addAnnotationUsage(cache);
		assertNull(createHelper(entity).getCacheUsage());
	}

	@Test
	void testGetCacheUsageNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCacheUsage());
	}

	@Test
	void testGetCacheRegion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		cache.region("myRegion");
		entity.addAnnotationUsage(cache);
		assertEquals("myRegion", createHelper(entity).getCacheRegion());
	}

	@Test
	void testGetCacheRegionNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCacheRegion());
	}

	@Test
	void testGetCacheIncludeNonLazy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		cache.includeLazy(false);
		entity.addAnnotationUsage(cache);
		assertEquals("non-lazy", createHelper(entity).getCacheInclude());
	}

	@Test
	void testGetCacheIncludeDefault() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCacheInclude());
	}

	// --- Where / abstract / optimistic lock ---

	@Test
	void testGetWhere() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLRestrictionAnnotation sr = HibernateAnnotations.SQL_RESTRICTION.createUsage(ctx);
		sr.value("active = true");
		entity.addAnnotationUsage(sr);
		assertEquals("active = true", createHelper(entity).getWhere());
	}

	@Test
	void testGetWhereNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getWhere());
	}

	@Test
	void testIsAbstractFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isAbstract());
	}

	@Test
	void testGetOptimisticLockMode() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.ALL);
		entity.addAnnotationUsage(ol);
		assertEquals("all", createHelper(entity).getOptimisticLockMode());
	}

	@Test
	void testGetOptimisticLockModeVersion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.VERSION);
		entity.addAnnotationUsage(ol);
		assertNull(createHelper(entity).getOptimisticLockMode());
	}

	@Test
	void testGetOptimisticLockModeDefault() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getOptimisticLockMode());
	}

	// --- RowId / Subselect ---

	@Test
	void testGetRowId() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		RowIdAnnotation rid = HibernateAnnotations.ROW_ID.createUsage(ctx);
		rid.value("ROWID");
		entity.addAnnotationUsage(rid);
		assertEquals("ROWID", createHelper(entity).getRowId());
	}

	@Test
	void testGetRowIdNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getRowId());
	}

	@Test
	void testGetSubselect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SubselectAnnotation ss = HibernateAnnotations.SUBSELECT.createUsage(ctx);
		ss.value("select * from view_x");
		entity.addAnnotationUsage(ss);
		assertEquals("select * from view_x", createHelper(entity).getSubselect());
	}

	@Test
	void testGetSubselectNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSubselect());
	}

	// --- Proxy ---

	@Test
	void testIsConcreteProxyTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.CONCRETE_PROXY.createUsage(ctx));
		assertTrue(createHelper(entity).isConcreteProxy());
	}

	@Test
	void testIsConcreteProxyFalseWhenProxySet() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.CONCRETE_PROXY.createUsage(ctx));
		Map<String, List<String>> meta = Map.of(
				"hibernate.proxy", List.of("com.example.TestEntityProxy"));
		assertFalse(createHelper(entity, meta).isConcreteProxy());
	}

	@Test
	void testIsConcreteProxyFalseNoAnnotation() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isConcreteProxy());
	}

	@Test
	void testGetProxy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		Map<String, List<String>> meta = Map.of(
				"hibernate.proxy", List.of("com.example.MyProxy"));
		assertEquals("com.example.MyProxy", createHelper(entity, meta).getProxy());
	}

	@Test
	void testGetProxyNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getProxy());
	}

	// --- EntityName ---

	@Test
	void testGetEntityNameCustom() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = new DynamicClassDetails(
				"TestEntity", "com.example.TestEntity",
				false, null, null, ctx);
		EntityJpaAnnotation entityAnn = JpaAnnotations.ENTITY.createUsage(ctx);
		entityAnn.name("CustomName");
		entity.addAnnotationUsage(entityAnn);
		assertEquals("CustomName", createHelper(entity).getEntityName());
	}

	@Test
	void testGetEntityNameMatchesSimpleName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		EntityJpaAnnotation entityAnn = JpaAnnotations.ENTITY.createUsage(ctx);
		entityAnn.name("TestEntity");
		entity.addAnnotationUsage(entityAnn);
		assertNull(createHelper(entity).getEntityName());
	}

	@Test
	void testGetEntityNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getEntityName());
	}

	// --- Inheritance ---

	@Test
	void testIsSubclassFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isSubclass());
	}

	@Test
	void testIsSubclassTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails parent = createMinimalEntity(ctx);
		DynamicClassDetails child = new DynamicClassDetails(
				"Child", "com.example.Child", Object.class,
				false, parent, null, ctx);
		assertTrue(createHelper(child).isSubclass());
	}

	@Test
	void testGetParentClassName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails parent = createMinimalEntity(ctx);
		DynamicClassDetails child = new DynamicClassDetails(
				"Child", "com.example.Child", Object.class,
				false, parent, null, ctx);
		assertEquals("com.example.TestEntity", createHelper(child).getParentClassName());
	}

	@Test
	void testGetParentClassNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getParentClassName());
	}

	@Test
	void testGetClassTagClass() {
		ModelsContext ctx = createContext();
		assertEquals("class", createHelper(createMinimalEntity(ctx)).getClassTag());
	}

	@Test
	void testGetClassTagSubclass() {
		ModelsContext ctx = createContext();
		DynamicClassDetails parent = createMinimalEntity(ctx);
		DynamicClassDetails child = new DynamicClassDetails(
				"Child", "com.example.Child", Object.class,
				false, parent, null, ctx);
		assertEquals("subclass", createHelper(child).getClassTag());
	}

	@Test
	void testGetClassTagJoinedSubclass() {
		ModelsContext ctx = createContext();
		DynamicClassDetails parent = createMinimalEntity(ctx);
		DynamicClassDetails child = new DynamicClassDetails(
				"Child", "com.example.Child", Object.class,
				false, parent, null, ctx);
		PrimaryKeyJoinColumnJpaAnnotation pkjc =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("PARENT_ID");
		child.addAnnotationUsage(pkjc);
		assertEquals("joined-subclass", createHelper(child).getClassTag());
	}

	@Test
	void testGetClassTagUnionSubclass() {
		ModelsContext ctx = createContext();
		DynamicClassDetails parent = createMinimalEntity(ctx);
		DynamicClassDetails child = new DynamicClassDetails(
				"Child", "com.example.Child", Object.class,
				false, parent, null, ctx);
		InheritanceJpaAnnotation inh = JpaAnnotations.INHERITANCE.createUsage(ctx);
		inh.strategy(InheritanceType.TABLE_PER_CLASS);
		child.addAnnotationUsage(inh);
		assertEquals("union-subclass", createHelper(child).getClassTag());
	}

	// --- Discriminator ---

	@Test
	void testNeedsDiscriminatorTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.name("DTYPE");
		entity.addAnnotationUsage(dc);
		assertTrue(createHelper(entity).needsDiscriminator());
	}

	@Test
	void testNeedsDiscriminatorFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).needsDiscriminator());
	}

	@Test
	void testGetDiscriminatorColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.name("DTYPE");
		entity.addAnnotationUsage(dc);
		assertEquals("DTYPE", createHelper(entity).getDiscriminatorColumnName());
	}

	@Test
	void testGetDiscriminatorColumnNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getDiscriminatorColumnName());
	}

	@Test
	void testGetDiscriminatorTypeNameString() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.discriminatorType(DiscriminatorType.STRING);
		entity.addAnnotationUsage(dc);
		assertEquals("string", createHelper(entity).getDiscriminatorTypeName());
	}

	@Test
	void testGetDiscriminatorTypeNameInteger() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.discriminatorType(DiscriminatorType.INTEGER);
		entity.addAnnotationUsage(dc);
		assertEquals("integer", createHelper(entity).getDiscriminatorTypeName());
	}

	@Test
	void testGetDiscriminatorTypeNameChar() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.discriminatorType(DiscriminatorType.CHAR);
		entity.addAnnotationUsage(dc);
		assertEquals("character", createHelper(entity).getDiscriminatorTypeName());
	}

	@Test
	void testGetDiscriminatorTypeNameDefault() {
		ModelsContext ctx = createContext();
		assertEquals("string",
				createHelper(createMinimalEntity(ctx)).getDiscriminatorTypeName());
	}

	@Test
	void testGetDiscriminatorColumnLength() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.length(50);
		entity.addAnnotationUsage(dc);
		assertEquals(50, createHelper(entity).getDiscriminatorColumnLength());
	}

	@Test
	void testGetDiscriminatorColumnLengthDefault31() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.length(31);
		entity.addAnnotationUsage(dc);
		assertEquals(0, createHelper(entity).getDiscriminatorColumnLength());
	}

	@Test
	void testGetDiscriminatorColumnLengthNoAnnotation() {
		ModelsContext ctx = createContext();
		assertEquals(0,
				createHelper(createMinimalEntity(ctx)).getDiscriminatorColumnLength());
	}

	@Test
	void testGetDiscriminatorValue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorValueJpaAnnotation dv =
				JpaAnnotations.DISCRIMINATOR_VALUE.createUsage(ctx);
		dv.value("TEST");
		entity.addAnnotationUsage(dv);
		assertEquals("TEST", createHelper(entity).getDiscriminatorValue());
	}

	@Test
	void testGetDiscriminatorValueNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getDiscriminatorValue());
	}

	@Test
	void testGetPrimaryKeyJoinColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		PrimaryKeyJoinColumnJpaAnnotation pkjc =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("PARENT_ID");
		entity.addAnnotationUsage(pkjc);
		assertEquals("PARENT_ID", createHelper(entity).getPrimaryKeyJoinColumnName());
	}

	@Test
	void testGetPrimaryKeyJoinColumnNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getPrimaryKeyJoinColumnName());
	}
}
