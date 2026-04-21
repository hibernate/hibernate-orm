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

import java.util.List;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;

/**
 * Tests for {@link MappingEntityInfoHelper}.
 *
 * @author Koen Aers
 */
class MappingEntityInfoHelperTest {

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

	private MappingEntityInfoHelper createHelper(DynamicClassDetails entity) {
		return new MappingEntityInfoHelper(entity);
	}

	// --- isEmbeddable ---

	@Test
	void testIsEmbeddableFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isEmbeddable());
	}

	@Test
	void testIsEmbeddableTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = new DynamicClassDetails(
				"Address", "com.example.Address", false, null, null, ctx);
		entity.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx));
		assertTrue(createHelper(entity).isEmbeddable());
	}

	// --- getClassName / getPackageName ---

	@Test
	void testGetClassName() {
		ModelsContext ctx = createContext();
		assertEquals("com.example.TestEntity",
				createHelper(createMinimalEntity(ctx)).getClassName());
	}

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

	// --- Entity-level Hibernate extensions ---

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
	void testIsDynamicUpdateDefault() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isDynamicUpdate());
	}

	@Test
	void testIsDynamicUpdateTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_UPDATE.createUsage(ctx));
		assertTrue(createHelper(entity).isDynamicUpdate());
	}

	@Test
	void testIsDynamicInsertDefault() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isDynamicInsert());
	}

	@Test
	void testIsDynamicInsertTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.DYNAMIC_INSERT.createUsage(ctx));
		assertTrue(createHelper(entity).isDynamicInsert());
	}

	@Test
	void testGetBatchSizeDefault() {
		ModelsContext ctx = createContext();
		assertEquals(0, createHelper(createMinimalEntity(ctx)).getBatchSize());
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
	void testGetCacheAccessTypeNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCacheAccessType());
	}

	@Test
	void testGetCacheAccessType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_WRITE);
		entity.addAnnotationUsage(cache);
		assertEquals("READ_WRITE", createHelper(entity).getCacheAccessType());
	}

	@Test
	void testGetCacheAccessTypeNone() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.NONE);
		entity.addAnnotationUsage(cache);
		assertNull(createHelper(entity).getCacheAccessType());
	}

	@Test
	void testGetCacheRegionNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCacheRegion());
	}

	@Test
	void testGetCacheRegion() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		CacheAnnotation cache = HibernateAnnotations.CACHE.createUsage(ctx);
		cache.usage(CacheConcurrencyStrategy.READ_ONLY);
		cache.region("myRegion");
		entity.addAnnotationUsage(cache);
		assertEquals("myRegion", createHelper(entity).getCacheRegion());
	}

	@Test
	void testIsCacheIncludeLazyDefault() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).isCacheIncludeLazy());
	}

	@Test
	void testGetSqlRestrictionNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSqlRestriction());
	}

	@Test
	void testGetSqlRestriction() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SQLRestrictionAnnotation sr = HibernateAnnotations.SQL_RESTRICTION.createUsage(ctx);
		sr.value("active = true");
		entity.addAnnotationUsage(sr);
		assertEquals("active = true", createHelper(entity).getSqlRestriction());
	}

	@Test
	void testGetOptimisticLockModeNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getOptimisticLockMode());
	}

	@Test
	void testGetOptimisticLockMode() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		OptimisticLockingAnnotation ol = HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(ctx);
		ol.type(OptimisticLockType.ALL);
		entity.addAnnotationUsage(ol);
		assertEquals("ALL", createHelper(entity).getOptimisticLockMode());
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
	void testGetRowIdNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getRowId());
	}

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
	void testGetSubselectNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSubselect());
	}

	@Test
	void testGetSubselect() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SubselectAnnotation ss = HibernateAnnotations.SUBSELECT.createUsage(ctx);
		ss.value("select * from t");
		entity.addAnnotationUsage(ss);
		assertEquals("select * from t", createHelper(entity).getSubselect());
	}

	@Test
	void testIsConcreteProxyFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).isConcreteProxy());
	}

	@Test
	void testIsConcreteProxyTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		entity.addAnnotationUsage(HibernateAnnotations.CONCRETE_PROXY.createUsage(ctx));
		assertTrue(createHelper(entity).isConcreteProxy());
	}

	@Test
	void testGetClassAccessTypeNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getClassAccessType());
	}

	@Test
	void testGetClassAccessTypeProperty() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.PROPERTY);
		entity.addAnnotationUsage(access);
		assertEquals("PROPERTY", createHelper(entity).getClassAccessType());
	}

	@Test
	void testGetClassAccessTypeField() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		AccessJpaAnnotation access = JpaAnnotations.ACCESS.createUsage(ctx);
		access.value(AccessType.FIELD);
		entity.addAnnotationUsage(access);
		assertNull(createHelper(entity).getClassAccessType());
	}

	// --- Table ---

	@Test
	void testGetTableNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getTableName());
	}

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
	void testGetSchemaNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getSchema());
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
	void testGetCatalogNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getCatalog());
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

	// --- Inheritance ---

	@Test
	void testHasInheritanceFalse() {
		ModelsContext ctx = createContext();
		assertFalse(createHelper(createMinimalEntity(ctx)).hasInheritance());
	}

	@Test
	void testHasInheritanceTrue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		InheritanceJpaAnnotation inh = JpaAnnotations.INHERITANCE.createUsage(ctx);
		inh.strategy(InheritanceType.JOINED);
		entity.addAnnotationUsage(inh);
		assertTrue(createHelper(entity).hasInheritance());
	}

	@Test
	void testGetInheritanceStrategy() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		InheritanceJpaAnnotation inh = JpaAnnotations.INHERITANCE.createUsage(ctx);
		inh.strategy(InheritanceType.TABLE_PER_CLASS);
		entity.addAnnotationUsage(inh);
		assertEquals("TABLE_PER_CLASS", createHelper(entity).getInheritanceStrategy());
	}

	@Test
	void testGetInheritanceStrategyNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getInheritanceStrategy());
	}

	@Test
	void testGetDiscriminatorColumnName() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc = JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
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
	void testGetDiscriminatorType() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc = JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.name("DTYPE");
		dc.discriminatorType(DiscriminatorType.INTEGER);
		entity.addAnnotationUsage(dc);
		assertEquals("INTEGER", createHelper(entity).getDiscriminatorType());
	}

	@Test
	void testGetDiscriminatorTypeNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getDiscriminatorType());
	}

	@Test
	void testGetDiscriminatorColumnLength() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorColumnJpaAnnotation dc = JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx);
		dc.name("DTYPE");
		dc.length(50);
		entity.addAnnotationUsage(dc);
		assertEquals(50, createHelper(entity).getDiscriminatorColumnLength());
	}

	@Test
	void testGetDiscriminatorColumnLengthDefault() {
		ModelsContext ctx = createContext();
		assertEquals(0, createHelper(createMinimalEntity(ctx)).getDiscriminatorColumnLength());
	}

	@Test
	void testGetDiscriminatorValue() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		DiscriminatorValueJpaAnnotation dv = JpaAnnotations.DISCRIMINATOR_VALUE.createUsage(ctx);
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
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("PARENT_ID");
		entity.addAnnotationUsage(pkjc);
		assertEquals("PARENT_ID", createHelper(entity).getPrimaryKeyJoinColumnName());
	}

	@Test
	void testGetPrimaryKeyJoinColumnNameNull() {
		ModelsContext ctx = createContext();
		assertNull(createHelper(createMinimalEntity(ctx)).getPrimaryKeyJoinColumnName());
	}

	@Test
	void testGetPrimaryKeyJoinColumnNames() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("PARENT_ID");
		entity.addAnnotationUsage(pkjc);
		List<String> names = createHelper(entity).getPrimaryKeyJoinColumnNames();
		assertEquals(1, names.size());
		assertEquals("PARENT_ID", names.get(0));
	}

	@Test
	void testGetPrimaryKeyJoinColumnNamesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getPrimaryKeyJoinColumnNames().isEmpty());
	}

	// --- Secondary tables ---

	@Test
	void testGetSecondaryTablesEmpty() {
		ModelsContext ctx = createContext();
		assertTrue(createHelper(createMinimalEntity(ctx)).getSecondaryTables().isEmpty());
	}

	@Test
	void testGetSecondaryTables() {
		ModelsContext ctx = createContext();
		DynamicClassDetails entity = createMinimalEntity(ctx);
		SecondaryTableJpaAnnotation st = JpaAnnotations.SECONDARY_TABLE.createUsage(ctx);
		st.name("DETAILS");
		PrimaryKeyJoinColumnJpaAnnotation pkjc = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx);
		pkjc.name("DETAIL_ID");
		st.pkJoinColumns(new jakarta.persistence.PrimaryKeyJoinColumn[]{pkjc});
		entity.addAnnotationUsage(st);
		List<MappingXmlHelper.SecondaryTableInfo> tables = createHelper(entity).getSecondaryTables();
		assertEquals(1, tables.size());
		assertEquals("DETAILS", tables.get(0).tableName());
		assertEquals(1, tables.get(0).keyColumns().size());
		assertEquals("DETAIL_ID", tables.get(0).keyColumns().get(0));
	}
}
