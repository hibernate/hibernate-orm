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
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;

import org.hibernate.annotations.FlushModeType;
import org.hibernate.tool.reveng.internal.util.HbmEnumMapper;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
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
import org.hibernate.boot.models.annotations.internal.ParamDefAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingsJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;

/**
 * Builds filter, query, fetch-profile, and result-set-mapping annotations
 * from hbm.xml elements.
 *
 * @author Koen Aers
 */
class HbmFilterAndQueryBuilder {

	// --- Filters ---

	static void processFilters(DynamicClassDetails entityClass,
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

	static void processFilterDefs(DynamicClassDetails entityClass,
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

	static void processFetchProfiles(DynamicClassDetails entityClass,
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

	// --- Named Queries ---

	static void processNamedQueries(DynamicClassDetails entityClass,
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

	// --- Named Native Queries ---

	static void processNamedNativeQueries(DynamicClassDetails entityClass,
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
		applyNativeQueryContent(annotation, query.getContent(), name, entityClass, ctx);
	}

	private static void applyNativeQueryContent(NamedNativeQueryAnnotation annotation,
												 List<Serializable> content,
												 String name,
												 DynamicClassDetails entityClass,
												 HbmBuildContext ctx) {
		List<String> querySpaces = new ArrayList<>();
		for (Serializable item : content) {
			Object value = item instanceof JAXBElement<?> je ? je.getValue() : item;
			if (value instanceof JaxbHbmSynchronizeType sync) {
				querySpaces.add(sync.getTable());
			} else if (value instanceof JaxbHbmNativeQueryReturnType returnType) {
				applyNativeQueryReturn(returnType, name, entityClass, ctx);
			} else if (value instanceof JaxbHbmNativeQueryJoinReturnType joinReturn) {
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".return-join.alias",
						joinReturn.getAlias());
				ctx.addClassMetaAttribute(entityClass.getClassName(),
						"hibernate.sql-query." + name + ".return-join.property",
						joinReturn.getProperty());
			} else if (value instanceof JaxbHbmNativeQueryCollectionLoadReturnType loadCol) {
				applyNativeQueryLoadCollection(loadCol, name, entityClass, ctx);
			}
		}
		if (!querySpaces.isEmpty()) {
			annotation.querySpaces(querySpaces.toArray(new String[0]));
		}
	}

	private static void applyNativeQueryReturn(JaxbHbmNativeQueryReturnType returnType,
												String name,
												DynamicClassDetails entityClass,
												HbmBuildContext ctx) {
		String alias = returnType.getAlias();
		String returnClass = returnType.getClazz();
		if (returnClass != null && !returnClass.isEmpty()) {
			returnClass = HbmTypeResolver.resolveClassName(
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
	}

	private static void applyNativeQueryLoadCollection(
			JaxbHbmNativeQueryCollectionLoadReturnType loadCol,
			String name,
			DynamicClassDetails entityClass,
			HbmBuildContext ctx) {
		ctx.addClassMetaAttribute(entityClass.getClassName(),
				"hibernate.sql-query." + name + ".load-collection.alias",
				loadCol.getAlias());
		String role = loadCol.getRole();
		if (role != null && role.contains(".")) {
			String roleClass = role.substring(0, role.lastIndexOf('.'));
			String roleProperty = role.substring(role.lastIndexOf('.') + 1);
			String resolvedClass = HbmTypeResolver.resolveClassName(
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

	static FlushModeType mapFlushMode(org.hibernate.FlushMode flushMode) {
		return HbmEnumMapper.mapFlushMode(flushMode);
	}

	static String extractQueryString(List<Serializable> content) {
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

	// --- Result Set Mappings ---

	static void processResultSetMappings(DynamicClassDetails entityClass,
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
				entityResults.add(buildEntityResult(returnType, mc));
			} else if (source instanceof JaxbHbmNativeQueryScalarReturnType scalarReturn) {
				columnResults.add(buildColumnResult(scalarReturn, mc, ctx));
			}
		}

		if (!entityResults.isEmpty()) {
			annotation.entities(entityResults.toArray(new EntityResult[0]));
		}
		if (!columnResults.isEmpty()) {
			annotation.columns(columnResults.toArray(new ColumnResult[0]));
		}
	}

	private static EntityResult buildEntityResult(JaxbHbmNativeQueryReturnType returnType,
												   ModelsContext mc) {
		EntityResultJpaAnnotation er = JpaAnnotations.ENTITY_RESULT.createUsage(mc);
		String className = returnType.getClazz();
		if (className != null) {
			try {
				er.entityClass(Class.forName(className));
			} catch (ClassNotFoundException e) {
				er.entityClass(Object.class);
			}
		}
		List<JaxbHbmNativeQueryPropertyReturnType> props = returnType.getReturnProperty();
		if (props != null && !props.isEmpty()) {
			FieldResult[] fieldResults = new FieldResult[props.size()];
			for (int j = 0; j < props.size(); j++) {
				FieldResultJpaAnnotation fr = JpaAnnotations.FIELD_RESULT.createUsage(mc);
				fr.name(props.get(j).getName());
				String col = props.get(j).getColumn();
				if (col != null) {
					fr.column(col);
				}
				fieldResults[j] = fr;
			}
			er.fields(fieldResults);
		}
		return er;
	}

	private static ColumnResult buildColumnResult(JaxbHbmNativeQueryScalarReturnType scalarReturn,
												   ModelsContext mc,
												   HbmBuildContext ctx) {
		ColumnResultJpaAnnotation cr = JpaAnnotations.COLUMN_RESULT.createUsage(mc);
		cr.name(scalarReturn.getColumn());
		String type = scalarReturn.getType();
		if (type != null) {
			try {
				cr.type(Class.forName(ctx.resolveJavaType(type)));
			} catch (ClassNotFoundException e) {
				cr.type(Object.class);
			}
		}
		return cr;
	}

	// --- Mapping-level annotations ---

	static void processMappingLevelAnnotations(DynamicClassDetails entityClass,
											   JaxbHbmHibernateMapping mapping,
											   HbmBuildContext ctx) {
		processFilterDefs(entityClass, mapping.getFilterDef(), ctx);
		processFetchProfiles(entityClass, mapping.getFetchProfile(), ctx);
		processNamedQueries(entityClass, mapping.getQuery(), ctx, true);
		processNamedNativeQueries(entityClass, mapping.getSqlQuery(), ctx, true);
		processResultSetMappings(entityClass, mapping.getResultset(), ctx);
	}
}
