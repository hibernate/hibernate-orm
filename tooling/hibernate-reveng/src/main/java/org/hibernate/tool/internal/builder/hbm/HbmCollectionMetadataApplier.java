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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CascadeType;
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
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CascadeAnnotation;
import org.hibernate.boot.models.annotations.internal.CheckAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.annotations.internal.SortNaturalAnnotation;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.tool.internal.util.HbmEnumMapper;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ModelsContext;

class HbmCollectionMetadataApplier {

	static void applyCommonMetadata(DynamicFieldDetails field,
									 String cascade,
									 JaxbHbmFetchStyleWithSubselectEnum fetch,
									 JaxbHbmLazyWithExtraEnum lazy,
									 String where,
									 int batchSize,
									 JaxbHbmCacheType cache,
									 List<JaxbHbmFilterType> filters,
									 JaxbHbmCustomSqlDmlType sqlInsert,
									 JaxbHbmCustomSqlDmlType sqlUpdate,
									 JaxbHbmCustomSqlDmlType sqlDelete,
									 JaxbHbmCustomSqlDmlType sqlDeleteAll,
									 boolean mutable,
									 boolean optimisticLock,
									 String check,
									 String table,
									 String schema,
									 String catalog,
									 HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();
		applyCascade(field, cascade, ctx);
		applyLazy(field, lazy, ctx);
		applyFetch(field, fetch, mc);
		applyWhere(field, where, mc);
		applyBatchSize(field, batchSize, mc);
		applyCache(field, cache, mc);
		applyFilters(field, filters, mc);
		applySqlDml(field, sqlInsert, sqlUpdate, sqlDelete, sqlDeleteAll, mc);
		applyMutable(field, mutable, mc);
		applyOptimisticLock(field, optimisticLock, mc);
		applyCheck(field, check, mc);
		applyCollectionTable(field, table, schema, catalog, mc);
	}

	static void applyInverse(DynamicFieldDetails field, boolean inverse,
							  DynamicClassDetails entityClass,
							  HbmBuildContext ctx) {
		if (inverse) {
			ctx.addFieldMetaAttribute(entityClass.getClassName(), field.getName(),
					"hibernate.inverse", "true");
		}
	}

	static void applyCascade(DynamicFieldDetails field, String cascade,
							  HbmBuildContext ctx) {
		if (cascade != null && !cascade.isEmpty() && !"none".equals(cascade)) {
			CascadeType[] cascadeTypes = parseCascade(cascade);
			if (cascadeTypes.length > 0) {
				CascadeAnnotation cascadeAnnotation =
						HibernateAnnotations.CASCADE.createUsage(ctx.getModelsContext());
				cascadeAnnotation.value(cascadeTypes);
				field.addAnnotationUsage(cascadeAnnotation);
			}
			ctx.addFieldMetaAttribute(
					((DynamicClassDetails) field.getDeclaringType()).getClassName(),
					field.getName(), "hibernate.cascade", cascade.trim());
		}
	}

	static void applyAccessAnnotation(DynamicFieldDetails field,
									   String access,
									   HbmBuildContext ctx) {
		if (access == null || access.isEmpty()) {
			return;
		}
		AccessJpaAnnotation accessAnnotation =
				JpaAnnotations.ACCESS.createUsage(ctx.getModelsContext());
		if ("field".equals(access)) {
			accessAnnotation.value(jakarta.persistence.AccessType.FIELD);
		} else if ("property".equals(access)) {
			accessAnnotation.value(jakarta.persistence.AccessType.PROPERTY);
		} else {
			accessAnnotation.value(jakarta.persistence.AccessType.FIELD);
		}
		field.addAnnotationUsage(accessAnnotation);
	}

	static void applyListIndex(DynamicFieldDetails field,
								JaxbHbmIndexType index,
								JaxbHbmListIndexType listIndex,
								HbmBuildContext ctx) {
		String columnName = null;
		if (listIndex != null) {
			columnName = listIndex.getColumnAttribute();
			if ((columnName == null || columnName.isEmpty())
					&& listIndex.getColumn() != null) {
				columnName = listIndex.getColumn().getName();
			}
		} else if (index != null) {
			columnName = index.getColumnAttribute();
		}
		if (columnName != null && !columnName.isEmpty()) {
			OrderColumnJpaAnnotation ocAnnotation =
					JpaAnnotations.ORDER_COLUMN.createUsage(ctx.getModelsContext());
			ocAnnotation.name(columnName);
			field.addAnnotationUsage(ocAnnotation);
		}
	}

	static void applySortAnnotation(DynamicFieldDetails field,
									 String sort,
									 HbmBuildContext ctx) {
		if (sort == null || sort.isEmpty() || "unsorted".equals(sort)) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if ("natural".equals(sort)) {
			SortNaturalAnnotation sortAnnotation =
					HibernateAnnotations.SORT_NATURAL.createUsage(mc);
			field.addAnnotationUsage(sortAnnotation);
		} else {
			SortComparatorAnnotation sortAnnotation =
					HibernateAnnotations.SORT_COMPARATOR.createUsage(mc);
			try {
				sortAnnotation.value(
						(Class<? extends java.util.Comparator<?>>) Class.forName(sort));
			} catch (ClassNotFoundException e) {
				return;
			}
			field.addAnnotationUsage(sortAnnotation);
		}
	}

	static void applyOrderBy(DynamicFieldDetails field,
							   String orderBy,
							   HbmBuildContext ctx) {
		if (orderBy == null || orderBy.isEmpty()) {
			return;
		}
		var sqlOrderAnnotation =
				HibernateAnnotations.SQL_ORDER.createUsage(ctx.getModelsContext());
		sqlOrderAnnotation.value(orderBy);
		field.addAnnotationUsage(sqlOrderAnnotation);
	}

	// --- Join column helpers ---

	static void addKeyJoinColumns(DynamicFieldDetails field,
								   JaxbHbmKeyType key,
								   HbmBuildContext ctx) {
		if (key == null) {
			return;
		}
		if (key.getColumnAttribute() != null) {
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			jc.name(key.getColumnAttribute());
			field.addAnnotationUsage(jc);
			return;
		}
		List<JaxbHbmColumnType> columns = key.getColumn();
		if (columns != null && !columns.isEmpty()) {
			if (columns.size() == 1) {
				JoinColumnJpaAnnotation jc =
						JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
				jc.name(columns.get(0).getName());
				field.addAnnotationUsage(jc);
			} else {
				jakarta.persistence.JoinColumn[] jcArray =
						new jakarta.persistence.JoinColumn[columns.size()];
				for (int i = 0; i < columns.size(); i++) {
					JoinColumnJpaAnnotation jc =
							JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
					jc.name(columns.get(i).getName());
					jcArray[i] = jc;
				}
				JoinColumnsJpaAnnotation jcs =
						JpaAnnotations.JOIN_COLUMNS.createUsage(ctx.getModelsContext());
				jcs.value(jcArray);
				field.addAnnotationUsage(jcs);
			}
		}
	}

	static void addManyToManyJoinTable(DynamicFieldDetails field,
										JaxbHbmKeyType key,
										JaxbHbmManyToManyCollectionElementType manyToMany,
										HbmBuildContext ctx) {
		if (key == null) {
			return;
		}
		JoinTableJpaAnnotation jtAnnotation =
				JpaAnnotations.JOIN_TABLE.createUsage(ctx.getModelsContext());

		String keyColumn = key.getColumnAttribute();
		if (keyColumn != null && !keyColumn.isEmpty()) {
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			jc.name(keyColumn);
			jtAnnotation.joinColumns(new jakarta.persistence.JoinColumn[]{jc});
		} else if (key.getColumn() != null && !key.getColumn().isEmpty()) {
			List<JaxbHbmColumnType> keyCols = key.getColumn();
			jakarta.persistence.JoinColumn[] jcArray =
					new jakarta.persistence.JoinColumn[keyCols.size()];
			for (int i = 0; i < keyCols.size(); i++) {
				JoinColumnJpaAnnotation jc =
						JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
				jc.name(keyCols.get(i).getName());
				jcArray[i] = jc;
			}
			jtAnnotation.joinColumns(jcArray);
		}

		String m2mColumn = manyToMany.getColumnAttribute();
		if (m2mColumn != null && !m2mColumn.isEmpty()) {
			JoinColumnJpaAnnotation ijc =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			ijc.name(m2mColumn);
			jtAnnotation.inverseJoinColumns(new jakarta.persistence.JoinColumn[]{ijc});
		} else {
			List<JaxbHbmColumnType> columns = new ArrayList<>();
			List<String> formulas = new ArrayList<>();
			for (Object item : manyToMany.getColumnOrFormula()) {
				if (item instanceof JaxbHbmColumnType col) {
					columns.add(col);
				} else if (item instanceof String formula) {
					formulas.add(formula);
				}
			}
			if (!columns.isEmpty()) {
				jakarta.persistence.JoinColumn[] ijcArray =
						new jakarta.persistence.JoinColumn[columns.size()];
				for (int i = 0; i < columns.size(); i++) {
					JoinColumnJpaAnnotation ijc =
							JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
					ijc.name(columns.get(i).getName());
					ijcArray[i] = ijc;
				}
				jtAnnotation.inverseJoinColumns(ijcArray);
			}
			DynamicClassDetails owner = (DynamicClassDetails) field.getDeclaringType();
			for (String formula : formulas) {
				ctx.addFieldMetaAttribute(owner.getClassName(), field.getName(),
						"hibernate.formula", formula);
			}
		}

		field.addAnnotationUsage(jtAnnotation);
	}

	static void addCollectionTableFromKey(DynamicFieldDetails field,
										   JaxbHbmKeyType key,
										   HbmBuildContext ctx) {
		if (key == null) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		String columnName = key.getColumnAttribute();
		if (columnName == null || columnName.isEmpty()) {
			List<JaxbHbmColumnType> columns = key.getColumn();
			if (columns != null && !columns.isEmpty()) {
				columnName = columns.get(0).getName();
			}
		}
		if (columnName != null && !columnName.isEmpty()) {
			CollectionTableJpaAnnotation ctAnnotation =
					JpaAnnotations.COLLECTION_TABLE.createUsage(mc);
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(mc);
			jc.name(columnName);
			ctAnnotation.joinColumns(new jakarta.persistence.JoinColumn[] { jc });
			field.addAnnotationUsage(ctAnnotation);
		}
	}

	// --- Private helpers for applyCommonMetadata ---

	private static void applyLazy(DynamicFieldDetails field,
								   JaxbHbmLazyWithExtraEnum lazy,
								   HbmBuildContext ctx) {
		if (lazy == null) {
			return;
		}
		if (lazy == JaxbHbmLazyWithExtraEnum.FALSE) {
			ManyToManyJpaAnnotation m2m = (ManyToManyJpaAnnotation)
					field.getDirectAnnotationUsage(jakarta.persistence.ManyToMany.class);
			if (m2m != null) {
				m2m.fetch(jakarta.persistence.FetchType.EAGER);
			}
			OneToManyJpaAnnotation o2m = (OneToManyJpaAnnotation)
					field.getDirectAnnotationUsage(jakarta.persistence.OneToMany.class);
			if (o2m != null) {
				o2m.fetch(jakarta.persistence.FetchType.EAGER);
			}
		} else if (lazy == JaxbHbmLazyWithExtraEnum.EXTRA) {
			DynamicClassDetails owner = (DynamicClassDetails) field.getDeclaringType();
			ctx.addFieldMetaAttribute(owner.getClassName(), field.getName(),
					"hibernate.lazy", "extra");
		}
	}

	private static void applyFetch(DynamicFieldDetails field,
									JaxbHbmFetchStyleWithSubselectEnum fetch,
									ModelsContext mc) {
		if (fetch != null) {
			FetchAnnotation fetchAnnotation =
					HibernateAnnotations.FETCH.createUsage(mc);
			fetchAnnotation.value(HbmEnumMapper.mapFetchMode(fetch));
			field.addAnnotationUsage(fetchAnnotation);
		}
	}

	private static void applyWhere(DynamicFieldDetails field,
									String where, ModelsContext mc) {
		if (where != null && !where.isEmpty()) {
			SQLRestrictionAnnotation srAnnotation =
					HibernateAnnotations.SQL_RESTRICTION.createUsage(mc);
			srAnnotation.value(where);
			field.addAnnotationUsage(srAnnotation);
		}
	}

	private static void applyBatchSize(DynamicFieldDetails field,
										int batchSize, ModelsContext mc) {
		if (batchSize > 0) {
			BatchSizeAnnotation bsAnnotation =
					HibernateAnnotations.BATCH_SIZE.createUsage(mc);
			bsAnnotation.size(batchSize);
			field.addAnnotationUsage(bsAnnotation);
		}
	}

	private static void applyCache(DynamicFieldDetails field,
									JaxbHbmCacheType cache, ModelsContext mc) {
		if (cache != null) {
			CacheAnnotation cacheAnnotation =
					HibernateAnnotations.CACHE.createUsage(mc);
			cacheAnnotation.usage(HbmEnumMapper.mapCacheConcurrency(cache.getUsage()));
			String region = cache.getRegion();
			if (region != null && !region.isEmpty()) {
				cacheAnnotation.region(region);
			}
			field.addAnnotationUsage(cacheAnnotation);
		}
	}

	private static void applyFilters(DynamicFieldDetails field,
									  List<JaxbHbmFilterType> filters,
									  ModelsContext mc) {
		if (filters == null || filters.isEmpty()) {
			return;
		}
		if (filters.size() == 1) {
			FilterAnnotation fa = HibernateAnnotations.FILTER.createUsage(mc);
			applyFilter(fa, filters.get(0));
			field.addAnnotationUsage(fa);
		} else {
			FilterAnnotation[] filterAnnotations = new FilterAnnotation[filters.size()];
			for (int i = 0; i < filters.size(); i++) {
				FilterAnnotation fa = HibernateAnnotations.FILTER.createUsage(mc);
				applyFilter(fa, filters.get(i));
				filterAnnotations[i] = fa;
			}
			FiltersAnnotation container = HibernateAnnotations.FILTERS.createUsage(mc);
			container.value(filterAnnotations);
			field.addAnnotationUsage(container);
		}
	}

	private static void applyFilter(FilterAnnotation annotation,
									  JaxbHbmFilterType filter) {
		annotation.name(filter.getName());
		String condition = filter.getCondition();
		if (condition != null && !condition.isEmpty()) {
			annotation.condition(condition);
		}
	}

	private static void applySqlDml(DynamicFieldDetails field,
									 JaxbHbmCustomSqlDmlType sqlInsert,
									 JaxbHbmCustomSqlDmlType sqlUpdate,
									 JaxbHbmCustomSqlDmlType sqlDelete,
									 JaxbHbmCustomSqlDmlType sqlDeleteAll,
									 ModelsContext mc) {
		if (sqlInsert != null) {
			SQLInsertAnnotation annotation = HibernateAnnotations.SQL_INSERT.createUsage(mc);
			annotation.sql(sqlInsert.getValue());
			annotation.callable(sqlInsert.isCallable());
			field.addAnnotationUsage(annotation);
		}
		if (sqlUpdate != null) {
			SQLUpdateAnnotation annotation = HibernateAnnotations.SQL_UPDATE.createUsage(mc);
			annotation.sql(sqlUpdate.getValue());
			annotation.callable(sqlUpdate.isCallable());
			field.addAnnotationUsage(annotation);
		}
		if (sqlDelete != null) {
			SQLDeleteAnnotation annotation = HibernateAnnotations.SQL_DELETE.createUsage(mc);
			annotation.sql(sqlDelete.getValue());
			annotation.callable(sqlDelete.isCallable());
			field.addAnnotationUsage(annotation);
		}
		if (sqlDeleteAll != null) {
			SQLDeleteAllAnnotation annotation =
					HibernateAnnotations.SQL_DELETE_ALL.createUsage(mc);
			annotation.sql(sqlDeleteAll.getValue());
			annotation.callable(sqlDeleteAll.isCallable());
			field.addAnnotationUsage(annotation);
		}
	}

	private static void applyMutable(DynamicFieldDetails field,
									  boolean mutable, ModelsContext mc) {
		if (!mutable) {
			field.addAnnotationUsage(
					HibernateAnnotations.IMMUTABLE.createUsage(mc));
		}
	}

	private static void applyOptimisticLock(DynamicFieldDetails field,
											 boolean optimisticLock,
											 ModelsContext mc) {
		if (!optimisticLock) {
			OptimisticLockAnnotation olAnnotation =
					HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(mc);
			olAnnotation.excluded(true);
			field.addAnnotationUsage(olAnnotation);
		}
	}

	private static void applyCheck(DynamicFieldDetails field,
									String check, ModelsContext mc) {
		if (check != null && !check.isEmpty()) {
			CheckAnnotation checkAnnotation =
					HibernateAnnotations.CHECK.createUsage(mc);
			checkAnnotation.constraints(check);
			field.addAnnotationUsage(checkAnnotation);
		}
	}

	private static void applyCollectionTable(DynamicFieldDetails field,
											  String table, String schema,
											  String catalog, ModelsContext mc) {
		if (table == null || table.isEmpty()) {
			return;
		}
		JoinTableJpaAnnotation jtAnnotation = (JoinTableJpaAnnotation)
				field.getDirectAnnotationUsage(jakarta.persistence.JoinTable.class);
		if (jtAnnotation != null) {
			jtAnnotation.name(table);
		} else {
			jtAnnotation = JpaAnnotations.JOIN_TABLE.createUsage(mc);
			jtAnnotation.name(table);
			field.addAnnotationUsage(jtAnnotation);
		}
		if (schema != null && !schema.isEmpty()) {
			jtAnnotation.schema(schema);
		}
		if (catalog != null && !catalog.isEmpty()) {
			jtAnnotation.catalog(catalog);
		}
	}

	// --- Mapping helpers ---

	private static CascadeType[] parseCascade(String cascade) {
		List<CascadeType> types = new ArrayList<>();
		for (String part : cascade.split(",")) {
			String trimmed = part.trim().toLowerCase();
			CascadeType ct = mapCascadeType(trimmed);
			if (ct != null) {
				types.add(ct);
				if ("all-delete-orphan".equals(trimmed)) {
					types.add(CascadeType.DELETE_ORPHAN);
				}
			}
		}
		return types.toArray(new CascadeType[0]);
	}

	private static CascadeType mapCascadeType(String value) {
		return switch (value) {
			case "all", "all-delete-orphan" -> CascadeType.ALL;
			case "save-update" -> CascadeType.PERSIST;
			case "delete", "remove" -> CascadeType.REMOVE;
			case "persist" -> CascadeType.PERSIST;
			case "merge" -> CascadeType.MERGE;
			case "refresh" -> CascadeType.REFRESH;
			case "lock" -> CascadeType.LOCK;
			case "replicate" -> CascadeType.REPLICATE;
			case "evict", "detach" -> CascadeType.DETACH;
			case "delete-orphan" -> CascadeType.DELETE_ORPHAN;
			default -> null;
		};
	}

}
