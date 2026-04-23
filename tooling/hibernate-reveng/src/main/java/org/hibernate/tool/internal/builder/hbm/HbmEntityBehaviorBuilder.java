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

import java.util.List;

import org.hibernate.tool.internal.util.HbmEnumMapper;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLoaderType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CheckAnnotation;
import org.hibernate.boot.models.annotations.internal.CommentAnnotation;
import org.hibernate.boot.models.annotations.internal.HQLSelectAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTablesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.boot.models.annotations.internal.SynchronizeAnnotation;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * Builds entity behavioral annotations, secondary tables, SQL DML,
 * proxy, and loader annotations from hbm.xml elements.
 *
 * @author Koen Aers
 */
class HbmEntityBehaviorBuilder {

	// --- SQL Insert / Update / Delete ---

	static void processSqlStatements(DynamicClassDetails entityClass,
									 JaxbHbmRootEntityType entityType,
									 HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();

		JaxbHbmCustomSqlDmlType sqlInsert = entityType.getSqlInsert();
		if (sqlInsert != null) {
			SQLInsertAnnotation annotation = HibernateAnnotations.SQL_INSERT.createUsage(mc);
			annotation.sql(sqlInsert.getValue());
			annotation.callable(sqlInsert.isCallable());
			entityClass.addAnnotationUsage(annotation);
		}

		JaxbHbmCustomSqlDmlType sqlUpdate = entityType.getSqlUpdate();
		if (sqlUpdate != null) {
			SQLUpdateAnnotation annotation = HibernateAnnotations.SQL_UPDATE.createUsage(mc);
			annotation.sql(sqlUpdate.getValue());
			annotation.callable(sqlUpdate.isCallable());
			entityClass.addAnnotationUsage(annotation);
		}

		JaxbHbmCustomSqlDmlType sqlDelete = entityType.getSqlDelete();
		if (sqlDelete != null) {
			SQLDeleteAnnotation annotation = HibernateAnnotations.SQL_DELETE.createUsage(mc);
			annotation.sql(sqlDelete.getValue());
			annotation.callable(sqlDelete.isCallable());
			entityClass.addAnnotationUsage(annotation);
		}
	}

	// --- Secondary Tables (<join>) ---

	static void processSecondaryTables(DynamicClassDetails entityClass,
									   List<JaxbHbmSecondaryTableType> joins,
									   HbmBuildContext ctx) {
		if (joins == null || joins.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if (joins.size() == 1) {
			SecondaryTableJpaAnnotation stAnnotation =
					JpaAnnotations.SECONDARY_TABLE.createUsage(mc);
			applySecondaryTable(stAnnotation, joins.get(0), mc);
			entityClass.addAnnotationUsage(stAnnotation);
			processJoinAttributes(entityClass, joins.get(0), ctx);
		} else {
			SecondaryTableJpaAnnotation[] stAnnotations =
					new SecondaryTableJpaAnnotation[joins.size()];
			for (int i = 0; i < joins.size(); i++) {
				SecondaryTableJpaAnnotation sta =
						JpaAnnotations.SECONDARY_TABLE.createUsage(mc);
				applySecondaryTable(sta, joins.get(i), mc);
				stAnnotations[i] = sta;
				processJoinAttributes(entityClass, joins.get(i), ctx);
			}
			SecondaryTablesJpaAnnotation container =
					JpaAnnotations.SECONDARY_TABLES.createUsage(mc);
			container.value(stAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void processJoinAttributes(DynamicClassDetails entityClass,
											   JaxbHbmSecondaryTableType join,
											   HbmBuildContext ctx) {
		if (join.getAttributes() != null && !join.getAttributes().isEmpty()) {
			HbmSubclassBuilder.processAttributes(entityClass,
					join.getAttributes(), null, ctx);
		}
	}

	private static void applySecondaryTable(SecondaryTableJpaAnnotation annotation,
											 JaxbHbmSecondaryTableType join,
											 ModelsContext mc) {
		annotation.name(join.getTable());
		if (join.getSchema() != null && !join.getSchema().isEmpty()) {
			annotation.schema(join.getSchema());
		}
		if (join.getCatalog() != null && !join.getCatalog().isEmpty()) {
			annotation.catalog(join.getCatalog());
		}
		JaxbHbmKeyType key = join.getKey();
		if (key != null) {
			applyPrimaryKeyJoinColumns(annotation, key, mc);
		}
	}

	private static void applyPrimaryKeyJoinColumns(SecondaryTableJpaAnnotation annotation,
													JaxbHbmKeyType key,
													ModelsContext mc) {
		String columnAttr = key.getColumnAttribute();
		List<JaxbHbmColumnType> columns = key.getColumn();
		if (columnAttr != null && !columnAttr.isEmpty()) {
			PrimaryKeyJoinColumnJpaAnnotation pkJoinCol =
					JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(mc);
			pkJoinCol.name(columnAttr);
			annotation.pkJoinColumns(new PrimaryKeyJoinColumn[]{pkJoinCol});
		} else if (columns != null && !columns.isEmpty()) {
			PrimaryKeyJoinColumn[] pkJoinCols =
					new PrimaryKeyJoinColumn[columns.size()];
			for (int i = 0; i < columns.size(); i++) {
				PrimaryKeyJoinColumnJpaAnnotation pkJoinCol =
						JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(mc);
				pkJoinCol.name(columns.get(i).getName());
				pkJoinCols[i] = pkJoinCol;
			}
			annotation.pkJoinColumns(pkJoinCols);
		}
	}

	// --- Entity behavioral attributes ---

	static void processEntityBehavior(DynamicClassDetails entityClass,
									  JaxbHbmRootEntityType entityType,
									  HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();
		applyMutability(entityClass, entityType, mc);
		applyDynamicInsertUpdate(entityClass, entityType, mc);
		applyBatchSize(entityClass, entityType, mc);
		applyOptimisticLocking(entityClass, entityType, mc);
		applyWhere(entityClass, entityType, mc);
		applyCheck(entityClass, entityType, mc);
		applyRowId(entityClass, entityType, mc);
		applySubselect(entityClass, entityType, mc);
		applyCache(entityClass, entityType, mc);
		applyComment(entityClass, entityType, mc);
		applySynchronize(entityClass, entityType, mc);
	}

	private static void applyMutability(DynamicClassDetails entityClass,
										 JaxbHbmRootEntityType entityType,
										 ModelsContext mc) {
		if (!entityType.isMutable()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.IMMUTABLE.createUsage(mc));
		}
	}

	private static void applyDynamicInsertUpdate(DynamicClassDetails entityClass,
												  JaxbHbmRootEntityType entityType,
												  ModelsContext mc) {
		if (entityType.isDynamicInsert()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.DYNAMIC_INSERT.createUsage(mc));
		}
		if (entityType.isDynamicUpdate()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.DYNAMIC_UPDATE.createUsage(mc));
		}
	}

	private static void applyBatchSize(DynamicClassDetails entityClass,
										JaxbHbmRootEntityType entityType,
										ModelsContext mc) {
		int batchSize = entityType.getBatchSize();
		if (batchSize > 0) {
			BatchSizeAnnotation bsAnnotation =
					HibernateAnnotations.BATCH_SIZE.createUsage(mc);
			bsAnnotation.size(batchSize);
			entityClass.addAnnotationUsage(bsAnnotation);
		}
	}

	private static void applyOptimisticLocking(DynamicClassDetails entityClass,
												JaxbHbmRootEntityType entityType,
												ModelsContext mc) {
		OptimisticLockStyle lockStyle = entityType.getOptimisticLock();
		if (lockStyle != null && lockStyle != OptimisticLockStyle.VERSION) {
			OptimisticLockingAnnotation olAnnotation =
					HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(mc);
			olAnnotation.type(HbmEnumMapper.mapOptimisticLockType(lockStyle));
			entityClass.addAnnotationUsage(olAnnotation);
		}
	}

	private static void applyWhere(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									ModelsContext mc) {
		String where = entityType.getWhere();
		if (where != null && !where.isEmpty()) {
			SQLRestrictionAnnotation srAnnotation =
					HibernateAnnotations.SQL_RESTRICTION.createUsage(mc);
			srAnnotation.value(where);
			entityClass.addAnnotationUsage(srAnnotation);
		}
	}

	private static void applyCheck(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									ModelsContext mc) {
		String check = entityType.getCheck();
		if (check != null && !check.isEmpty()) {
			CheckAnnotation checkAnnotation =
					HibernateAnnotations.CHECK.createUsage(mc);
			checkAnnotation.constraints(check);
			entityClass.addAnnotationUsage(checkAnnotation);
		}
	}

	private static void applyRowId(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									ModelsContext mc) {
		String rowid = entityType.getRowid();
		if (rowid != null && !rowid.isEmpty()) {
			RowIdAnnotation rowIdAnnotation =
					HibernateAnnotations.ROW_ID.createUsage(mc);
			rowIdAnnotation.value(rowid);
			entityClass.addAnnotationUsage(rowIdAnnotation);
		}
	}

	private static void applySubselect(DynamicClassDetails entityClass,
										JaxbHbmRootEntityType entityType,
										ModelsContext mc) {
		String subselect = entityType.getSubselect();
		if (subselect == null || subselect.isEmpty()) {
			subselect = entityType.getSubselectAttribute();
		}
		if (subselect != null && !subselect.isEmpty()) {
			SubselectAnnotation ssAnnotation =
					HibernateAnnotations.SUBSELECT.createUsage(mc);
			ssAnnotation.value(subselect);
			entityClass.addAnnotationUsage(ssAnnotation);
		}
	}

	private static void applyCache(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									ModelsContext mc) {
		JaxbHbmCacheType cache = entityType.getCache();
		if (cache != null) {
			CacheAnnotation cacheAnnotation =
					HibernateAnnotations.CACHE.createUsage(mc);
			cacheAnnotation.usage(HbmEnumMapper.mapCacheConcurrency(cache.getUsage()));
			String region = cache.getRegion();
			if (region != null && !region.isEmpty()) {
				cacheAnnotation.region(region);
			}
			entityClass.addAnnotationUsage(cacheAnnotation);
		}
	}

	private static void applyComment(DynamicClassDetails entityClass,
									  JaxbHbmRootEntityType entityType,
									  ModelsContext mc) {
		String comment = entityType.getComment();
		if (comment != null && !comment.isEmpty()) {
			CommentAnnotation commentAnnotation =
					HibernateAnnotations.COMMENT.createUsage(mc);
			commentAnnotation.value(comment);
			entityClass.addAnnotationUsage(commentAnnotation);
		}
	}

	private static void applySynchronize(DynamicClassDetails entityClass,
										  JaxbHbmRootEntityType entityType,
										  ModelsContext mc) {
		List<JaxbHbmSynchronizeType> synchronize = entityType.getSynchronize();
		if (synchronize != null && !synchronize.isEmpty()) {
			String[] tables = synchronize.stream()
					.map(JaxbHbmSynchronizeType::getTable)
					.toArray(String[]::new);
			SynchronizeAnnotation syncAnnotation =
					HibernateAnnotations.SYNCHRONIZE.createUsage(mc);
			syncAnnotation.value(tables);
			entityClass.addAnnotationUsage(syncAnnotation);
		}
	}

	// --- Concrete Proxy (proxy/lazy) ---

	static void processProxy(DynamicClassDetails entityClass,
							 JaxbHbmRootEntityType entityType,
							 HbmBuildContext ctx) {
		String proxy = entityType.getProxy();
		Boolean lazy = entityType.isLazy();
		if (proxy != null && !proxy.isEmpty()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.CONCRETE_PROXY.createUsage(
							ctx.getModelsContext()));
			String proxyFqn = HbmTypeResolver.resolveClassName(proxy, ctx.getDefaultPackage());
			if (!proxyFqn.equals(entityClass.getClassName())) {
				ctx.addClassMetaAttribute(entityClass.getClassName(), "implements", proxyFqn);
				ctx.addClassMetaAttribute(entityClass.getClassName(), "hibernate.proxy", proxyFqn);
			}
		} else if (lazy != null && lazy) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.CONCRETE_PROXY.createUsage(
							ctx.getModelsContext()));
		}
	}

	// --- Loader ---

	static void processLoader(DynamicClassDetails entityClass,
							  JaxbHbmLoaderType loader,
							  HbmBuildContext ctx) {
		if (loader == null) {
			return;
		}
		String queryRef = loader.getQueryRef();
		if (queryRef != null && !queryRef.isEmpty()) {
			HQLSelectAnnotation hqlAnnotation =
					HibernateAnnotations.HQL_SELECT.createUsage(ctx.getModelsContext());
			hqlAnnotation.query(queryRef);
			entityClass.addAnnotationUsage(hqlAnnotation);
		}
	}
}
