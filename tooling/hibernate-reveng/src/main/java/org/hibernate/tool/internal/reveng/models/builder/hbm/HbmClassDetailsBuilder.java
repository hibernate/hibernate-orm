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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

/**
 * Builds dynamic entity {@link ClassDetails} with JPA annotations
 * from hbm.xml files using Hibernate ORM's JAXB-based
 * {@link MappingBinder} for parsing.
 * <p>
 * This is a thin orchestrator that delegates to specialised builders:
 * <ul>
 *   <li>{@link HbmIdBuilder} — {@code <id>} and {@code <composite-id>}</li>
 *   <li>{@link HbmPropertyBuilder} — {@code <property>}, {@code <version>}, {@code <timestamp>}</li>
 *   <li>{@link HbmAssociationBuilder} — {@code <many-to-one>}, {@code <one-to-one>}</li>
 *   <li>{@link HbmCollectionBuilder} — {@code <set>}, {@code <list>}, {@code <bag>}, {@code <map>}</li>
 *   <li>{@link HbmComponentBuilder} — {@code <component>}</li>
 *   <li>{@link HbmSubclassBuilder} — {@code <subclass>}, {@code <joined-subclass>}, {@code <union-subclass>}</li>
 *   <li>{@link HbmEntityAnnotationBuilder} — {@code <filter>}, {@code <filter-def>}, {@code <fetch-profile>},
 *       {@code <sql-insert>}/{@code <sql-update>}/{@code <sql-delete>}, {@code <join>}</li>
 * </ul>
 *
 * @author Koen Aers
 */
public class HbmClassDetailsBuilder {

	private final HbmBuildContext ctx;
	private final MappingBinder mappingBinder;

	public HbmClassDetailsBuilder() {
		this.ctx = new HbmBuildContext();
		this.mappingBinder = new MappingBinder(
				MappingBinder.class.getClassLoader()::getResourceAsStream,
				UnsupportedFeatureHandling.ERROR);
	}

	public ModelsContext getModelsContext() {
		return ctx.getModelsContext();
	}

	/**
	 * Parses the given hbm.xml files using {@link MappingBinder} and builds
	 * {@link ClassDetails} with JPA annotations for each entity.
	 */
	public List<ClassDetails> buildFromFiles(File... hbmFiles) {
		List<ClassDetails> entities = new ArrayList<>();
		for (File file : hbmFiles) {
			JaxbHbmHibernateMapping mapping = parseHbmXml(file);
			String packageName = mapping.getPackage();
			ctx.setDefaultPackage(packageName);
			List<JaxbHbmRootEntityType> rootEntities = mapping.getClazz();
			for (JaxbHbmRootEntityType entityType : rootEntities) {
				entities.add(buildEntity(entityType, packageName));
			}
			// Mapping-level <filter-def> and <fetch-profile> go on first entity
			if (!rootEntities.isEmpty()) {
				DynamicClassDetails firstEntity = (DynamicClassDetails) entities.get(
						entities.size() - rootEntities.size());
				HbmEntityAnnotationBuilder.processMappingLevelAnnotations(
						firstEntity, mapping, ctx);
			}
		}
		return entities;
	}

	private JaxbHbmHibernateMapping parseHbmXml(File hbmFile) {
		Origin origin = new Origin(SourceType.FILE, hbmFile.getAbsolutePath());
		try (FileInputStream stream = new FileInputStream(hbmFile)) {
			Binding<JaxbHbmHibernateMapping> binding = mappingBinder.bind(stream, origin);
			return binding.getRoot();
		} catch (IOException e) {
			throw new RuntimeException(
					"Failed to parse hbm.xml file: " + hbmFile.getAbsolutePath(), e);
		}
	}

	private ClassDetails buildEntity(JaxbHbmRootEntityType entityType, String defaultPackage) {
		String className = entityType.getName();
		String fullName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullName);

		DynamicClassDetails entityClass = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, null, null, ctx.getModelsContext());

		// @Entity
		EntityJpaAnnotation entityAnnotation =
				JpaAnnotations.ENTITY.createUsage(ctx.getModelsContext());
		entityAnnotation.name(simpleName);
		entityClass.addAnnotationUsage(entityAnnotation);

		// @Table
		String tableName = entityType.getTable();
		if (tableName != null && !tableName.isEmpty()) {
			TableJpaAnnotation tableAnnotation =
					JpaAnnotations.TABLE.createUsage(ctx.getModelsContext());
			tableAnnotation.name(tableName);
			String schema = entityType.getSchema();
			if (schema != null && !schema.isEmpty()) {
				tableAnnotation.schema(schema);
			}
			String catalog = entityType.getCatalog();
			if (catalog != null && !catalog.isEmpty()) {
				tableAnnotation.catalog(catalog);
			}
			entityClass.addAnnotationUsage(tableAnnotation);
		}

		// Id / Composite Id
		HbmIdBuilder.processId(entityClass, entityType.getId(), ctx);
		HbmIdBuilder.processCompositeId(entityClass, entityType.getCompositeId(),
				defaultPackage, ctx);

		// Version / Timestamp
		HbmPropertyBuilder.processVersion(entityClass, entityType.getVersion(), ctx);
		HbmPropertyBuilder.processTimestamp(entityClass, entityType.getTimestamp(), ctx);

		// Natural-id — process attributes, then mark them with @NaturalId
		if (entityType.getNaturalId() != null) {
			int fieldCountBefore = entityClass.getFields().size();
			HbmSubclassBuilder.processAttributes(entityClass,
					entityType.getNaturalId().getAttributes(), defaultPackage, ctx);
			boolean mutable = entityType.getNaturalId().isMutable();
			HbmPropertyBuilder.markNaturalIdFields(entityClass,
					fieldCountBefore, mutable, ctx);
		}

		// Attributes (properties, associations, collections, components)
		HbmSubclassBuilder.processAttributes(entityClass,
				entityType.getAttributes(), defaultPackage, ctx);

		// Subclasses (inheritance)
		HbmSubclassBuilder.processSubclasses(entityClass, entityType, defaultPackage, ctx);

		// Entity-level annotations: filters, fetch-profiles, SQL DML, secondary tables
		HbmEntityAnnotationBuilder.processFilters(entityClass, entityType.getFilter(), ctx);
		HbmEntityAnnotationBuilder.processFetchProfiles(entityClass,
				entityType.getFetchProfile(), ctx);
		HbmEntityAnnotationBuilder.processSqlStatements(entityClass, entityType, ctx);
		HbmEntityAnnotationBuilder.processSecondaryTables(entityClass,
				entityType.getJoin(), ctx);

		// Entity behavioral attributes: cache, immutable, dynamic-insert/update, etc.
		HbmEntityAnnotationBuilder.processEntityBehavior(entityClass, entityType, ctx);

		// Proxy / lazy loading
		HbmEntityAnnotationBuilder.processProxy(entityClass, entityType, ctx);

		// Custom loader
		HbmEntityAnnotationBuilder.processLoader(entityClass, entityType.getLoader(), ctx);

		// Result set mappings (entity-level)
		HbmEntityAnnotationBuilder.processResultSetMappings(entityClass,
				entityType.getResultset(), ctx);

		// Named queries (entity-level)
		HbmEntityAnnotationBuilder.processNamedQueries(entityClass,
				entityType.getQuery(), ctx);
		HbmEntityAnnotationBuilder.processNamedNativeQueries(entityClass,
				entityType.getSqlQuery(), ctx);

		ctx.registerClassDetails(entityClass);
		return entityClass;
	}
}
