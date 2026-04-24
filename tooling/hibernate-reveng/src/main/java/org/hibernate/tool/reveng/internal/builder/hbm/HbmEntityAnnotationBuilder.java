/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLoaderType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;

/**
 * Facade that delegates entity-level annotation building to
 * {@link HbmFilterAndQueryBuilder} and {@link HbmEntityBehaviorBuilder}.
 *
 * @author Koen Aers
 */
public class HbmEntityAnnotationBuilder {

	public static void processFilters(DynamicClassDetails entityClass,
									List<JaxbHbmFilterType> filters,
									HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processFilters(entityClass, filters, ctx);
	}

	public static void processFilterDefs(DynamicClassDetails entityClass,
										List<JaxbHbmFilterDefinitionType> filterDefs,
										HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processFilterDefs(entityClass, filterDefs, ctx);
	}

	public static void processFetchProfiles(DynamicClassDetails entityClass,
											List<JaxbHbmFetchProfileType> fetchProfiles,
											HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processFetchProfiles(entityClass, fetchProfiles, ctx);
	}

	public static void processNamedQueries(DynamicClassDetails entityClass,
											List<JaxbHbmNamedQueryType> queries,
											HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processNamedQueries(entityClass, queries, ctx, false);
	}

	public static void processNamedQueries(DynamicClassDetails entityClass,
											List<JaxbHbmNamedQueryType> queries,
											HbmBuildContext ctx,
											boolean mappingLevel) {
		HbmFilterAndQueryBuilder.processNamedQueries(entityClass, queries, ctx, mappingLevel);
	}

	public static void processNamedNativeQueries(DynamicClassDetails entityClass,
												List<JaxbHbmNamedNativeQueryType> queries,
												HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processNamedNativeQueries(entityClass, queries, ctx, false);
	}

	public static void processNamedNativeQueries(DynamicClassDetails entityClass,
												List<JaxbHbmNamedNativeQueryType> queries,
												HbmBuildContext ctx,
												boolean mappingLevel) {
		HbmFilterAndQueryBuilder.processNamedNativeQueries(entityClass, queries, ctx, mappingLevel);
	}

	public static void processResultSetMappings(DynamicClassDetails entityClass,
												List<JaxbHbmResultSetMappingType> resultsets,
												HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processResultSetMappings(entityClass, resultsets, ctx);
	}

	public static void processMappingLevelAnnotations(DynamicClassDetails entityClass,
													JaxbHbmHibernateMapping mapping,
													HbmBuildContext ctx) {
		HbmFilterAndQueryBuilder.processMappingLevelAnnotations(entityClass, mapping, ctx);
	}

	public static void processSqlStatements(DynamicClassDetails entityClass,
											JaxbHbmRootEntityType entityType,
											HbmBuildContext ctx) {
		HbmEntityBehaviorBuilder.processSqlStatements(entityClass, entityType, ctx);
	}

	public static void processSecondaryTables(DynamicClassDetails entityClass,
											List<JaxbHbmSecondaryTableType> joins,
											HbmBuildContext ctx) {
		HbmEntityBehaviorBuilder.processSecondaryTables(entityClass, joins, ctx);
	}

	public static void processEntityBehavior(DynamicClassDetails entityClass,
											JaxbHbmRootEntityType entityType,
											HbmBuildContext ctx) {
		HbmEntityBehaviorBuilder.processEntityBehavior(entityClass, entityType, ctx);
	}

	public static void processProxy(DynamicClassDetails entityClass,
									JaxbHbmRootEntityType entityType,
									HbmBuildContext ctx) {
		HbmEntityBehaviorBuilder.processProxy(entityClass, entityType, ctx);
	}

	public static void processLoader(DynamicClassDetails entityClass,
									JaxbHbmLoaderType loader,
									HbmBuildContext ctx) {
		HbmEntityBehaviorBuilder.processLoader(entityClass, loader, ctx);
	}
}
