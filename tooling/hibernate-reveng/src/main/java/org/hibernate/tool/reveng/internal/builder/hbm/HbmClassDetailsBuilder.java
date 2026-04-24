/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	public Map<String, List<String>> getClassMetaAttributes(String className) {
		return ctx.getClassMetaAttributes(className);
	}

	public Map<String, Map<String, List<String>>> getFieldMetaAttributes(String className) {
		return ctx.getFieldMetaAttributes(className);
	}

	public Map<String, Map<String, List<String>>> getAllClassMetaAttributes() {
		return ctx.getAllClassMetaAttributes();
	}

	public Map<String, Map<String, Map<String, List<String>>>> getAllFieldMetaAttributes() {
		return ctx.getAllFieldMetaAttributes();
	}

	/**
	 * Parses the given hbm.xml files using {@link MappingBinder} and builds
	 * {@link ClassDetails} with JPA annotations for each entity.
	 */
	public List<ClassDetails> buildFromFiles(File... hbmFiles) {
		return buildFromFilesAndResources(hbmFiles, new String[0]);
	}

	/**
	 * Parses the given hbm.xml files and classpath resources using
	 * {@link MappingBinder} and builds {@link ClassDetails} with JPA
	 * annotations for each entity.
	 */
	public List<ClassDetails> buildFromFilesAndResources(
			File[] hbmFiles, String[] resourcePaths) {
		List<ClassDetails> entities = new ArrayList<>();
		for (File file : hbmFiles) {
			processMapping(parseHbmXml(file), entities);
		}
		for (String resource : resourcePaths) {
			processMapping(parseHbmXmlResource(resource), entities);
		}
		// Include subclass entities so they get exported as separate files.
		entities.addAll(ctx.getSubclassEntityDetails());
		// Include embeddable classes (from <component> elements) so they
		// get exported as separate .java files by the entity exporter.
		entities.addAll(ctx.getEmbeddableClassDetails());
		return entities;
	}

	private void processMapping(JaxbHbmHibernateMapping mapping,
								List<ClassDetails> entities) {
		String packageName = mapping.getPackage();
		ctx.setDefaultPackage(packageName);
		List<JaxbHbmRootEntityType> rootEntities = mapping.getClazz();
		for (JaxbHbmRootEntityType entityType : rootEntities) {
			entities.add(buildEntity(entityType, packageName));
		}
		if (!rootEntities.isEmpty()) {
			DynamicClassDetails firstEntity = (DynamicClassDetails) entities.get(
					entities.size() - rootEntities.size());
			HbmEntityAnnotationBuilder.processMappingLevelAnnotations(
					firstEntity, mapping, ctx);
		}
		HbmSubclassBuilder.processTopLevelSubclasses(mapping, packageName, ctx);
	}

	private JaxbHbmHibernateMapping parseHbmXml(File hbmFile) {
		Origin origin = new Origin(SourceType.FILE, hbmFile.getAbsolutePath());
		try (FileInputStream stream = new FileInputStream(hbmFile)) {
			Binding<JaxbHbmHibernateMapping> binding = mappingBinder.bind(stream, origin);
			return binding.getRoot();
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to parse hbm.xml file: " + hbmFile.getAbsolutePath(), e);
		}
	}

	private JaxbHbmHibernateMapping parseHbmXmlResource(String resourcePath) {
		// Strip leading slash — ClassLoader.getResourceAsStream expects
		// paths without it (e.g. "org/..." not "/org/...")
		String normalized = resourcePath.startsWith("/")
				? resourcePath.substring(1) : resourcePath;
		Origin origin = new Origin(SourceType.RESOURCE, normalized);
		try (InputStream stream = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream(normalized)) {
			if (stream == null) {
				throw new RuntimeException(
						"Classpath resource not found: " + resourcePath);
			}
			Binding<JaxbHbmHibernateMapping> binding =
					mappingBinder.bind(stream, origin);
			return binding.getRoot();
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to parse hbm.xml resource: " + resourcePath, e);
		}
	}

	private ClassDetails buildEntity(JaxbHbmRootEntityType entityType, String defaultPackage) {
		String className = entityType.getName();
		String fullName = HbmTypeResolver.resolveClassName(className, defaultPackage);
		String simpleName = HbmTypeResolver.simpleName(fullName);

		// When entity-name differs from class name, use entity-name as ClassDetails identity
		String entityName = entityType.getEntityName();
		boolean hasEntityName = entityName != null && !entityName.isEmpty()
				&& !entityName.equals(simpleName) && !entityName.equals(fullName);
		String identityName = hasEntityName
				? HbmTypeResolver.resolveClassName(entityName, defaultPackage) : fullName;
		String identitySimpleName = hasEntityName
				? HbmTypeResolver.simpleName(identityName) : simpleName;

		boolean isAbstract = entityType.isAbstract() != null && entityType.isAbstract();
		DynamicClassDetails entityClass = new DynamicClassDetails(
				identitySimpleName, identityName, Object.class,
				isAbstract, null, null, ctx.getModelsContext());

		// @Entity
		EntityJpaAnnotation entityAnnotation =
				JpaAnnotations.ENTITY.createUsage(ctx.getModelsContext());
		entityAnnotation.name(hasEntityName ? entityName : simpleName);
		entityClass.addAnnotationUsage(entityAnnotation);

		// When entity-name differs, store real Java class name for HBM output
		if (hasEntityName) {
			ctx.addClassMetaAttribute(identityName, "hibernate.class-name", fullName);
		}

		// @Table — default to unqualified class name when not specified
		String tableName = entityType.getTable();
		if (tableName == null || tableName.isEmpty()) {
			tableName = simpleName;
		}
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

		// Entity-level meta attributes
		ctx.extractClassMetaAttributes(identityName, entityType);

		// Entity-level comment
		if (entityType.getComment() != null && !entityType.getComment().isEmpty()) {
			ctx.addClassMetaAttribute(identityName, "hibernate.comment", entityType.getComment());
		}

		// Id / Composite Id
		HbmIdBuilder.processId(entityClass, entityType.getId(), ctx);
		HbmIdBuilder.processCompositeId(entityClass, entityType.getCompositeId(),
				defaultPackage, ctx);
		// Id meta attributes
		if (entityType.getId() != null) {
			ctx.extractFieldMetaAttributes(identityName,
					entityType.getId().getName(), entityType.getId());
		}

		// Version / Timestamp
		HbmPropertyBuilder.processVersion(entityClass, entityType.getVersion(), ctx);
		HbmPropertyBuilder.processTimestamp(entityClass, entityType.getTimestamp(), ctx);
		if (entityType.getVersion() != null) {
			ctx.extractFieldMetaAttributes(identityName,
					entityType.getVersion().getName(), entityType.getVersion());
		}
		if (entityType.getTimestamp() != null) {
			ctx.extractFieldMetaAttributes(identityName,
					entityType.getTimestamp().getName(), entityType.getTimestamp());
		}

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

		// Discriminator column (for single-table inheritance, even when subclasses are top-level)
		if (entityType.getDiscriminator() != null) {
			HbmSubclassBuilder.addDiscriminatorColumnIfAbsent(entityClass, entityType, ctx);
		}

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
