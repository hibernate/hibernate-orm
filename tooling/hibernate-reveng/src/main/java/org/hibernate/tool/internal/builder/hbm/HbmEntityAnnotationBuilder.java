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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLoaderType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CheckAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CommentAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.HQLSelectAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueriesAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueriesAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfilesAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefsAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockingAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.RowIdAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTablesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SubselectAnnotation;
import org.hibernate.boot.models.annotations.internal.SynchronizeAnnotation;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * Adds entity-level annotations from hbm.xml elements and attributes:
 * {@code <filter>}, {@code <filter-def>}, {@code <fetch-profile>},
 * {@code <sql-insert>}/{@code <sql-update>}/{@code <sql-delete>},
 * {@code <join>} (secondary tables), {@code <cache>}, {@code <comment>},
 * {@code <synchronize>}, and behavioral attributes like
 * {@code mutable}, {@code dynamic-insert}, {@code dynamic-update},
 * {@code batch-size}, {@code optimistic-lock}, {@code where},
 * {@code check}, {@code rowid}, {@code subselect}.
 *
 * @author Koen Aers
 */
public class HbmEntityAnnotationBuilder {

	// --- Filters ---

	public static void processFilters(DynamicClassDetails entityClass,
									   List<JaxbHbmFilterType> filters,
									   HbmBuildContext ctx) {
		if (filters == null || filters.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if (filters.size() == 1) {
			FilterAnnotation filterAnnotation = HibernateAnnotations.FILTER.createUsage(mc);
			applyFilter(filterAnnotation, filters.get(0));
			entityClass.addAnnotationUsage(filterAnnotation);
		} else {
			FilterAnnotation[] filterAnnotations = new FilterAnnotation[filters.size()];
			for (int i = 0; i < filters.size(); i++) {
				FilterAnnotation fa = HibernateAnnotations.FILTER.createUsage(mc);
				applyFilter(fa, filters.get(i));
				filterAnnotations[i] = fa;
			}
			FiltersAnnotation container = HibernateAnnotations.FILTERS.createUsage(mc);
			container.value(filterAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void applyFilter(FilterAnnotation annotation, JaxbHbmFilterType filter) {
		annotation.name(filter.getName());
		String condition = filter.getCondition();
		if (condition != null && !condition.isEmpty()) {
			annotation.condition(condition);
		}
	}

	// --- Filter Definitions (mapping-level) ---

	public static void processFilterDefs(DynamicClassDetails entityClass,
										  List<JaxbHbmFilterDefinitionType> filterDefs,
										  HbmBuildContext ctx) {
		if (filterDefs == null || filterDefs.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if (filterDefs.size() == 1) {
			FilterDefAnnotation fdAnnotation = HibernateAnnotations.FILTER_DEF.createUsage(mc);
			applyFilterDef(fdAnnotation, filterDefs.get(0), mc);
			entityClass.addAnnotationUsage(fdAnnotation);
		} else {
			FilterDefAnnotation[] fdAnnotations = new FilterDefAnnotation[filterDefs.size()];
			for (int i = 0; i < filterDefs.size(); i++) {
				FilterDefAnnotation fda = HibernateAnnotations.FILTER_DEF.createUsage(mc);
				applyFilterDef(fda, filterDefs.get(i), mc);
				fdAnnotations[i] = fda;
			}
			FilterDefsAnnotation container = HibernateAnnotations.FILTER_DEFS.createUsage(mc);
			container.value(fdAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void applyFilterDef(FilterDefAnnotation annotation,
										JaxbHbmFilterDefinitionType filterDef,
										ModelsContext mc) {
		annotation.name(filterDef.getName());
		String condition = filterDef.getCondition();
		if (condition != null && !condition.isEmpty()) {
			annotation.defaultCondition(condition);
		}
		// Extract filter parameters from content
		List<Serializable> content = filterDef.getContent();
		if (content != null) {
			List<JaxbHbmFilterParameterType> params = content.stream()
					.filter(c -> c instanceof JaxbHbmFilterParameterType)
					.map(c -> (JaxbHbmFilterParameterType) c)
					.toList();
			if (!params.isEmpty()) {
				ParamDefAnnotation[] paramAnnotations = new ParamDefAnnotation[params.size()];
				for (int i = 0; i < params.size(); i++) {
					JaxbHbmFilterParameterType param = params.get(i);
					ParamDefAnnotation pda = HibernateAnnotations.PARAM_DEF.createUsage(mc);
					pda.name(param.getParameterName());
					String typeName = param.getParameterValueTypeName();
					if (typeName != null) {
						try {
							pda.type(Class.forName(resolveParamType(typeName)));
						} catch (ClassNotFoundException e) {
							pda.type(String.class);
						}
					}
					paramAnnotations[i] = pda;
				}
				annotation.parameters(paramAnnotations);
			}
		}
	}

	private static String resolveParamType(String hbmType) {
		return switch (hbmType.toLowerCase()) {
			case "string" -> "java.lang.String";
			case "integer", "int" -> "java.lang.Integer";
			case "long" -> "java.lang.Long";
			case "boolean" -> "java.lang.Boolean";
			case "double" -> "java.lang.Double";
			case "float" -> "java.lang.Float";
			default -> hbmType.contains(".") ? hbmType : "java.lang.String";
		};
	}

	// --- Fetch Profiles ---

	public static void processFetchProfiles(DynamicClassDetails entityClass,
											 List<JaxbHbmFetchProfileType> fetchProfiles,
											 HbmBuildContext ctx) {
		if (fetchProfiles == null || fetchProfiles.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if (fetchProfiles.size() == 1) {
			FetchProfileAnnotation fpAnnotation =
					HibernateAnnotations.FETCH_PROFILE.createUsage(mc);
			fpAnnotation.name(fetchProfiles.get(0).getName());
			entityClass.addAnnotationUsage(fpAnnotation);
		} else {
			FetchProfileAnnotation[] fpAnnotations =
					new FetchProfileAnnotation[fetchProfiles.size()];
			for (int i = 0; i < fetchProfiles.size(); i++) {
				FetchProfileAnnotation fpa =
						HibernateAnnotations.FETCH_PROFILE.createUsage(mc);
				fpa.name(fetchProfiles.get(i).getName());
				fpAnnotations[i] = fpa;
			}
			FetchProfilesAnnotation container =
					HibernateAnnotations.FETCH_PROFILES.createUsage(mc);
			container.value(fpAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	// --- SQL Insert / Update / Delete ---

	public static void processSqlStatements(DynamicClassDetails entityClass,
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

	public static void processSecondaryTables(DynamicClassDetails entityClass,
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

			// Process attributes in the join
			JaxbHbmSecondaryTableType join = joins.get(0);
			if (join.getAttributes() != null && !join.getAttributes().isEmpty()) {
				HbmSubclassBuilder.processAttributes(entityClass,
						join.getAttributes(), null, ctx);
			}
		} else {
			SecondaryTableJpaAnnotation[] stAnnotations =
					new SecondaryTableJpaAnnotation[joins.size()];
			for (int i = 0; i < joins.size(); i++) {
				SecondaryTableJpaAnnotation sta =
						JpaAnnotations.SECONDARY_TABLE.createUsage(mc);
				applySecondaryTable(sta, joins.get(i), mc);
				stAnnotations[i] = sta;

				// Process attributes in each join
				JaxbHbmSecondaryTableType join = joins.get(i);
				if (join.getAttributes() != null && !join.getAttributes().isEmpty()) {
					HbmSubclassBuilder.processAttributes(entityClass,
							join.getAttributes(), null, ctx);
				}
			}
			SecondaryTablesJpaAnnotation container =
					JpaAnnotations.SECONDARY_TABLES.createUsage(mc);
			container.value(stAnnotations);
			entityClass.addAnnotationUsage(container);
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

		// @PrimaryKeyJoinColumn from <key>
		JaxbHbmKeyType key = join.getKey();
		if (key != null) {
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
	}

	// --- Entity behavioral attributes ---

	/**
	 * Processes entity-level behavioral attributes and elements:
	 * {@code mutable}, {@code dynamic-insert}, {@code dynamic-update},
	 * {@code batch-size}, {@code optimistic-lock}, {@code where},
	 * {@code check}, {@code rowid}, {@code subselect}, {@code <cache>},
	 * {@code <comment>}, and {@code <synchronize>}.
	 */
	public static void processEntityBehavior(DynamicClassDetails entityClass,
											  JaxbHbmRootEntityType entityType,
											  HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();

		// @Immutable (mutable="false")
		if (!entityType.isMutable()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.IMMUTABLE.createUsage(mc));
		}

		// @DynamicInsert
		if (entityType.isDynamicInsert()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.DYNAMIC_INSERT.createUsage(mc));
		}

		// @DynamicUpdate
		if (entityType.isDynamicUpdate()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.DYNAMIC_UPDATE.createUsage(mc));
		}

		// @BatchSize
		int batchSize = entityType.getBatchSize();
		if (batchSize > 0) {
			BatchSizeAnnotation bsAnnotation =
					HibernateAnnotations.BATCH_SIZE.createUsage(mc);
			bsAnnotation.size(batchSize);
			entityClass.addAnnotationUsage(bsAnnotation);
		}

		// @OptimisticLocking
		OptimisticLockStyle lockStyle = entityType.getOptimisticLock();
		if (lockStyle != null && lockStyle != OptimisticLockStyle.VERSION) {
			OptimisticLockingAnnotation olAnnotation =
					HibernateAnnotations.OPTIMISTIC_LOCKING.createUsage(mc);
			olAnnotation.type(mapOptimisticLockType(lockStyle));
			entityClass.addAnnotationUsage(olAnnotation);
		}

		// @SQLRestriction (where="...")
		String where = entityType.getWhere();
		if (where != null && !where.isEmpty()) {
			SQLRestrictionAnnotation srAnnotation =
					HibernateAnnotations.SQL_RESTRICTION.createUsage(mc);
			srAnnotation.value(where);
			entityClass.addAnnotationUsage(srAnnotation);
		}

		// @Check (check="...")
		String check = entityType.getCheck();
		if (check != null && !check.isEmpty()) {
			CheckAnnotation checkAnnotation =
					HibernateAnnotations.CHECK.createUsage(mc);
			checkAnnotation.constraints(check);
			entityClass.addAnnotationUsage(checkAnnotation);
		}

		// @RowId (rowid="...")
		String rowid = entityType.getRowid();
		if (rowid != null && !rowid.isEmpty()) {
			RowIdAnnotation rowIdAnnotation =
					HibernateAnnotations.ROW_ID.createUsage(mc);
			rowIdAnnotation.value(rowid);
			entityClass.addAnnotationUsage(rowIdAnnotation);
		}

		// @Subselect (from element or attribute)
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

		// @Cache
		JaxbHbmCacheType cache = entityType.getCache();
		if (cache != null) {
			CacheAnnotation cacheAnnotation =
					HibernateAnnotations.CACHE.createUsage(mc);
			cacheAnnotation.usage(mapCacheConcurrency(cache.getUsage()));
			String region = cache.getRegion();
			if (region != null && !region.isEmpty()) {
				cacheAnnotation.region(region);
			}
			entityClass.addAnnotationUsage(cacheAnnotation);
		}

		// @Comment
		String comment = entityType.getComment();
		if (comment != null && !comment.isEmpty()) {
			CommentAnnotation commentAnnotation =
					HibernateAnnotations.COMMENT.createUsage(mc);
			commentAnnotation.value(comment);
			entityClass.addAnnotationUsage(commentAnnotation);
		}

		// @Synchronize
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

	private static OptimisticLockType mapOptimisticLockType(OptimisticLockStyle style) {
		return switch (style) {
			case NONE -> OptimisticLockType.NONE;
			case DIRTY -> OptimisticLockType.DIRTY;
			case ALL -> OptimisticLockType.ALL;
			default -> OptimisticLockType.VERSION;
		};
	}

	private static CacheConcurrencyStrategy mapCacheConcurrency(AccessType usage) {
		return switch (usage) {
			case READ_ONLY -> CacheConcurrencyStrategy.READ_ONLY;
			case READ_WRITE -> CacheConcurrencyStrategy.READ_WRITE;
			case NONSTRICT_READ_WRITE -> CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;
			case TRANSACTIONAL -> CacheConcurrencyStrategy.TRANSACTIONAL;
		};
	}

	// --- Named Queries ---

	public static void processNamedQueries(DynamicClassDetails entityClass,
											List<JaxbHbmNamedQueryType> queries,
											HbmBuildContext ctx) {
		processNamedQueries(entityClass, queries, ctx, false);
	}

	public static void processNamedQueries(DynamicClassDetails entityClass,
											List<JaxbHbmNamedQueryType> queries,
											HbmBuildContext ctx,
											boolean mappingLevel) {
		if (queries == null || queries.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		String entityName = entityClass.getClassName();
		if (queries.size() == 1) {
			NamedQueryAnnotation nqAnnotation =
					HibernateAnnotations.NAMED_QUERY.createUsage(mc);
			applyNamedQuery(nqAnnotation, queries.get(0), entityName, mappingLevel);
			entityClass.addAnnotationUsage(nqAnnotation);
		} else {
			NamedQueryAnnotation[] nqAnnotations =
					new NamedQueryAnnotation[queries.size()];
			for (int i = 0; i < queries.size(); i++) {
				NamedQueryAnnotation nqa =
						HibernateAnnotations.NAMED_QUERY.createUsage(mc);
				applyNamedQuery(nqa, queries.get(i), entityName, mappingLevel);
				nqAnnotations[i] = nqa;
			}
			NamedQueriesAnnotation container =
					HibernateAnnotations.NAMED_QUERIES.createUsage(mc);
			container.value(nqAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void applyNamedQuery(NamedQueryAnnotation annotation,
										  JaxbHbmNamedQueryType query,
										  String entityName,
										  boolean mappingLevel) {
		String name = query.getName();
		if (!mappingLevel && entityName != null && !name.contains(".")) {
			name = entityName + "." + name;
		}
		annotation.name(name);
		annotation.query(extractQueryString(query.getContent()));
		if (query.getFlushMode() != null) {
			annotation.flushMode(mapFlushMode(query.getFlushMode()));
		}
		if (query.isCacheable()) {
			annotation.cacheable(true);
		}
		if (query.getCacheRegion() != null && !query.getCacheRegion().isEmpty()) {
			annotation.cacheRegion(query.getCacheRegion());
		}
		if (query.getFetchSize() != null) {
			annotation.fetchSize(query.getFetchSize());
		}
		if (query.getTimeout() != null) {
			annotation.timeout(query.getTimeout().milliseconds() / 1000);
		}
		if (query.getComment() != null && !query.getComment().isEmpty()) {
			annotation.comment(query.getComment());
		}
		if (query.isReadOnly()) {
			annotation.readOnly(true);
		}
	}

	public static void processNamedNativeQueries(DynamicClassDetails entityClass,
												  List<JaxbHbmNamedNativeQueryType> queries,
												  HbmBuildContext ctx) {
		processNamedNativeQueries(entityClass, queries, ctx, false);
	}

	public static void processNamedNativeQueries(DynamicClassDetails entityClass,
												  List<JaxbHbmNamedNativeQueryType> queries,
												  HbmBuildContext ctx,
												  boolean mappingLevel) {
		if (queries == null || queries.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		String entityName = entityClass.getClassName();
		if (queries.size() == 1) {
			NamedNativeQueryAnnotation nqAnnotation =
					HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(mc);
			applyNamedNativeQuery(nqAnnotation, queries.get(0),
					entityName, mappingLevel, entityClass, ctx);
			entityClass.addAnnotationUsage(nqAnnotation);
		} else {
			NamedNativeQueryAnnotation[] nqAnnotations =
					new NamedNativeQueryAnnotation[queries.size()];
			for (int i = 0; i < queries.size(); i++) {
				NamedNativeQueryAnnotation nqa =
						HibernateAnnotations.NAMED_NATIVE_QUERY.createUsage(mc);
				applyNamedNativeQuery(nqa, queries.get(i),
						entityName, mappingLevel, entityClass, ctx);
				nqAnnotations[i] = nqa;
			}
			NamedNativeQueriesAnnotation container =
					HibernateAnnotations.NAMED_NATIVE_QUERIES.createUsage(mc);
			container.value(nqAnnotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void applyNamedNativeQuery(NamedNativeQueryAnnotation annotation,
											   JaxbHbmNamedNativeQueryType query,
											   String entityName,
											   boolean mappingLevel,
											   DynamicClassDetails entityClass,
											   HbmBuildContext ctx) {
		String name = query.getName();
		if (!mappingLevel && entityName != null && !name.contains(".")) {
			name = entityName + "." + name;
		}
		annotation.name(name);
		annotation.query(extractQueryString(query.getContent()));
		if (query.getFlushMode() != null) {
			annotation.flushMode(mapFlushMode(query.getFlushMode()));
		}
		if (query.isCacheable()) {
			annotation.cacheable(true);
		}
		if (query.getCacheRegion() != null && !query.getCacheRegion().isEmpty()) {
			annotation.cacheRegion(query.getCacheRegion());
		}
		if (query.getFetchSize() != null) {
			annotation.fetchSize(query.getFetchSize());
		}
		if (query.getTimeout() != null) {
			annotation.timeout(query.getTimeout().milliseconds() / 1000);
		}
		if (query.getComment() != null && !query.getComment().isEmpty()) {
			annotation.comment(query.getComment());
		}
		if (query.isReadOnly()) {
			annotation.readOnly(true);
		}
		String resultsetRef = query.getResultsetRef();
		if (resultsetRef != null && !resultsetRef.isEmpty()) {
			annotation.resultSetMapping(resultsetRef);
		}
		// Extract synchronize tables, return-join, and load-collection from
		// mixed content. JAXB wraps child elements in JAXBElement for @XmlMixed
		// content, so we must unwrap before checking types.
		List<String> querySpaces = new ArrayList<>();
		for (Serializable item : query.getContent()) {
			Object value = item instanceof JAXBElement<?> je ? je.getValue() : item;
			if (value instanceof JaxbHbmSynchronizeType sync) {
				querySpaces.add(sync.getTable());
			} else if (value instanceof JaxbHbmNativeQueryReturnType returnType) {
				String alias = returnType.getAlias();
				String returnClass = returnType.getClazz();
				if (returnClass != null && !returnClass.isEmpty()) {
					returnClass = HbmBuildContext.resolveClassName(
							returnClass, ctx.getDefaultPackage());
				}
				if (alias != null && !alias.isEmpty()) {
					ctx.addClassMetaAttribute(entityClass.getClassName(),
							"hibernate.sql-query." + name + ".return.alias", alias);
				}
				if (returnClass != null && !returnClass.isEmpty()) {
					ctx.addClassMetaAttribute(entityClass.getClassName(),
							"hibernate.sql-query." + name + ".return.class", returnClass);
				}
			} else if (value instanceof JaxbHbmNativeQueryJoinReturnType joinReturn) {
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".return-join.alias",
						joinReturn.getAlias());
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".return-join.property",
						joinReturn.getProperty());
			} else if (value instanceof JaxbHbmNativeQueryCollectionLoadReturnType loadCol) {
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".load-collection.alias",
						loadCol.getAlias());
				String role = loadCol.getRole();
				if (role != null && role.contains(".")) {
					String roleClass = role.substring(0, role.lastIndexOf('.'));
					String roleProperty = role.substring(role.lastIndexOf('.') + 1);
					String resolvedClass = HbmBuildContext.resolveClassName(
							roleClass, ctx.getDefaultPackage());
					role = resolvedClass + "." + roleProperty;
				}
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".load-collection.role",
						role);
				if (loadCol.getLockMode() != null) {
					ctx.addClassMetaAttribute(entityClass.getClassName(),
							"hibernate.sql-query." + name + ".load-collection.lock-mode",
							loadCol.getLockMode().name().toLowerCase());
				}
			}
		}
		if (!querySpaces.isEmpty()) {
			annotation.querySpaces(querySpaces.toArray(new String[0]));
		}
	}

	private static FlushModeType mapFlushMode(org.hibernate.FlushMode flushMode) {
		return switch (flushMode) {
			case AUTO -> FlushModeType.AUTO;
			case ALWAYS -> FlushModeType.ALWAYS;
			case COMMIT -> FlushModeType.COMMIT;
			case MANUAL -> FlushModeType.MANUAL;
			default -> FlushModeType.PERSISTENCE_CONTEXT;
		};
	}

	/**
	 * Extracts the query string from hbm.xml mixed content.
	 * The content list contains strings (the query text) interspersed
	 * with possible {@code <query-param>} elements.
	 */
	private static String extractQueryString(List<Serializable> content) {
		if (content == null || content.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Serializable item : content) {
			if (item instanceof String s) {
				sb.append(s);
			}
		}
		return sb.toString().trim();
	}

	// --- Concrete Proxy (proxy/lazy) ---

	/**
	 * Adds {@code @ConcreteProxy} when the entity specifies a proxy class
	 * or sets lazy="true".
	 */
	public static void processProxy(DynamicClassDetails entityClass,
									 JaxbHbmRootEntityType entityType,
									 HbmBuildContext ctx) {
		String proxy = entityType.getProxy();
		Boolean lazy = entityType.isLazy();
		if (proxy != null && !proxy.isEmpty()) {
			entityClass.addAnnotationUsage(
					HibernateAnnotations.CONCRETE_PROXY.createUsage(
							ctx.getModelsContext()));
			// Add proxy interface to implements list if it differs from the entity class
			String proxyFqn = HbmBuildContext.resolveClassName(proxy, ctx.getDefaultPackage());
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

	/**
	 * Processes the {@code <loader>} element which references a named query
	 * for custom entity loading. Mapped to {@code @HQLSelect(query=queryRef)}.
	 */
	public static void processLoader(DynamicClassDetails entityClass,
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

	// --- Result Set Mappings ---

	/**
	 * Processes {@code <resultset>} elements into {@code @SqlResultSetMapping}
	 * annotations with {@code @EntityResult} and {@code @ColumnResult}.
	 */
	public static void processResultSetMappings(DynamicClassDetails entityClass,
												 List<JaxbHbmResultSetMappingType> resultsets,
												 HbmBuildContext ctx) {
		if (resultsets == null || resultsets.isEmpty()) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if (resultsets.size() == 1) {
			SqlResultSetMappingJpaAnnotation annotation =
					JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(mc);
			applyResultSetMapping(annotation, resultsets.get(0), ctx);
			entityClass.addAnnotationUsage(annotation);
		} else {
			SqlResultSetMappingJpaAnnotation[] annotations =
					new SqlResultSetMappingJpaAnnotation[resultsets.size()];
			for (int i = 0; i < resultsets.size(); i++) {
				SqlResultSetMappingJpaAnnotation a =
						JpaAnnotations.SQL_RESULT_SET_MAPPING.createUsage(mc);
				applyResultSetMapping(a, resultsets.get(i), ctx);
				annotations[i] = a;
			}
			SqlResultSetMappingsJpaAnnotation container =
					JpaAnnotations.SQL_RESULT_SET_MAPPINGS.createUsage(mc);
			container.value(annotations);
			entityClass.addAnnotationUsage(container);
		}
	}

	private static void applyResultSetMapping(SqlResultSetMappingJpaAnnotation annotation,
											   JaxbHbmResultSetMappingType resultset,
											   HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();
		annotation.name(resultset.getName());

		List<Serializable> sources = resultset.getValueMappingSources();
		if (sources == null || sources.isEmpty()) {
			return;
		}

		List<EntityResult> entityResults = new ArrayList<>();
		List<ColumnResult> columnResults = new ArrayList<>();

		for (Serializable source : sources) {
			if (source instanceof JaxbHbmNativeQueryReturnType returnType) {
				EntityResultJpaAnnotation er =
						JpaAnnotations.ENTITY_RESULT.createUsage(mc);
				String className = returnType.getClazz();
				if (className != null) {
					try {
						er.entityClass(Class.forName(className));
					} catch (ClassNotFoundException e) {
						er.entityClass(Object.class);
					}
				}
				// Field results from return-property elements
				List<JaxbHbmNativeQueryPropertyReturnType> props =
						returnType.getReturnProperty();
				if (props != null && !props.isEmpty()) {
					FieldResult[] fieldResults = new FieldResult[props.size()];
					for (int j = 0; j < props.size(); j++) {
						FieldResultJpaAnnotation fr =
								JpaAnnotations.FIELD_RESULT.createUsage(mc);
						fr.name(props.get(j).getName());
						String col = props.get(j).getColumn();
						if (col != null) {
							fr.column(col);
						}
						fieldResults[j] = fr;
					}
					er.fields(fieldResults);
				}
				entityResults.add(er);
			} else if (source instanceof JaxbHbmNativeQueryScalarReturnType scalarReturn) {
				ColumnResultJpaAnnotation cr =
						JpaAnnotations.COLUMN_RESULT.createUsage(mc);
				cr.name(scalarReturn.getColumn());
				String type = scalarReturn.getType();
				if (type != null) {
					try {
						cr.type(Class.forName(
								ctx.resolveJavaType(type)));
					} catch (ClassNotFoundException e) {
						cr.type(Object.class);
					}
				}
				columnResults.add(cr);
			}
		}

		if (!entityResults.isEmpty()) {
			annotation.entities(entityResults.toArray(new EntityResult[0]));
		}
		if (!columnResults.isEmpty()) {
			annotation.columns(columnResults.toArray(new ColumnResult[0]));
		}
	}

	// --- Mapping-level filter-defs, fetch-profiles, and named queries ---

	/**
	 * Processes mapping-level elements from the {@code <hibernate-mapping>}
	 * root: {@code <filter-def>}, {@code <fetch-profile>}, {@code <query>},
	 * {@code <sql-query>}, and {@code <resultset>}. These are placed on the
	 * first entity in the mapping file.
	 */
	public static void processMappingLevelAnnotations(DynamicClassDetails entityClass,
													   JaxbHbmHibernateMapping mapping,
													   HbmBuildContext ctx) {
		processFilterDefs(entityClass, mapping.getFilterDef(), ctx);
		processFetchProfiles(entityClass, mapping.getFetchProfile(), ctx);
		processNamedQueries(entityClass, mapping.getQuery(), ctx, true);
		processNamedNativeQueries(entityClass, mapping.getSqlQuery(), ctx, true);
		processResultSetMappings(entityClass, mapping.getResultset(), ctx);
	}
}
