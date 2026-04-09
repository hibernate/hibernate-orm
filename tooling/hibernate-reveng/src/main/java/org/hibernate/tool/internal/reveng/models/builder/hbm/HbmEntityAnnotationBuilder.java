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
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import java.io.Serializable;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.FetchProfileAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchProfilesAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterDefsAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTablesJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * Adds entity-level annotations from hbm.xml elements:
 * {@code <filter>}, {@code <filter-def>}, {@code <fetch-profile>},
 * {@code <sql-insert>}/{@code <sql-update>}/{@code <sql-delete>},
 * and {@code <join>} (secondary tables).
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

	// --- Mapping-level filter-defs and fetch-profiles ---

	/**
	 * Processes mapping-level {@code <filter-def>} and {@code <fetch-profile>}
	 * elements from the {@code <hibernate-mapping>} root. These are placed on
	 * the first entity in the mapping file.
	 */
	public static void processMappingLevelAnnotations(DynamicClassDetails entityClass,
													   JaxbHbmHibernateMapping mapping,
													   HbmBuildContext ctx) {
		processFilterDefs(entityClass, mapping.getFilterDef(), ctx);
		processFetchProfiles(entityClass, mapping.getFetchProfile(), ctx);
	}
}
