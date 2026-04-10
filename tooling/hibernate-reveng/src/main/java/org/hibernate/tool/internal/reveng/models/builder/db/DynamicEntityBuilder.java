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
package org.hibernate.tool.internal.reveng.models.builder.db;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.IndexMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.UniqueConstraintJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;

/**
 * Builds dynamic entity ClassDetails with JPA annotations
 * from database metadata using Hibernate Models API.
 * This replaces the legacy org.hibernate.mapping.* approach
 * with a modern annotation-based metadata model.
 *
 * @author Koen Aers
 */
public class DynamicEntityBuilder {

	private final ModelsContext modelsContext;
	private final Map<String, TableMetadata> tableMetadataByClassName = new LinkedHashMap<>();
	private final List<ClassDetails> embeddableClassDetails = new ArrayList<>();
	private boolean preferBasicCompositeIds = true;

	public void setPreferBasicCompositeIds(boolean preferBasicCompositeIds) {
		this.preferBasicCompositeIds = preferBasicCompositeIds;
	}

	public DynamicEntityBuilder() {
		// Initialize the models context
		ClassLoading classLoading = SimpleClassLoading.SIMPLE_CLASS_LOADING;
		this.modelsContext = new BasicModelsContextImpl(classLoading, false, null);
	}

	/**
	 * Creates a ClassDetails representing an entity from database table metadata.
	 *
	 * @param tableMetadata The database table metadata
	 * @return ClassDetails with JPA annotations attached
	 */
	public ClassDetails createEntityFromTable(TableMetadata tableMetadata) {
		String className = qualifiedName(tableMetadata.getEntityPackage(), tableMetadata.getEntityClassName());

		// Resolve parent class if this is a subclass
		ClassDetails superClass = null;
		if (tableMetadata.getParentEntityClassName() != null) {
			String parentClassName = qualifiedName(tableMetadata.getParentEntityPackage(), tableMetadata.getParentEntityClassName());
			superClass = resolveOrCreateClassDetails(
				tableMetadata.getParentEntityClassName(),
				parentClassName
			);
		}

		// Create the dynamic class with both simple name and fully qualified class name
		DynamicClassDetails entityClass = new DynamicClassDetails(
			tableMetadata.getEntityClassName(),
			className,
			Object.class,
			false,
			superClass,
			null,
			modelsContext
		);

		// Add @Entity annotation
		addEntityAnnotation(entityClass, tableMetadata.getEntityClassName());

		// Add @Table annotation
		addTableAnnotation(entityClass, tableMetadata);

		// Add inheritance annotations
		addInheritanceAnnotations(entityClass, tableMetadata);

		// Add fields for each column:
		// - skip FK columns (handled by ManyToOne/OneToOne)
		// - PK columns in composite ID: add WITHOUT @Id (used by non-annotated mode)
		// - Constrained OneToOne PK/FK columns: add WITH @Id (needs @Id + @MapsId)
		boolean hasCompositeId = tableMetadata.getCompositeId() != null;
		Set<String> constrainedOneToOneFkColumns = new HashSet<>();
		for (OneToOneMetadata o2o : tableMetadata.getOneToOnes()) {
			if (o2o.isConstrained()) {
				constrainedOneToOneFkColumns.addAll(o2o.getForeignKeyColumnNames());
			}
		}
		for (ColumnMetadata columnMetadata : tableMetadata.getColumns()) {
			boolean isFk = tableMetadata.isForeignKeyColumn(columnMetadata.getColumnName());
			boolean isPk = columnMetadata.isPrimaryKey();
			boolean isConstrainedO2O = constrainedOneToOneFkColumns.contains(columnMetadata.getColumnName());
			// Skip FK columns, unless it's a constrained one-to-one PK (needs @Id)
			if (isFk && !(isPk && isConstrainedO2O)) continue;
			// PK columns in composite ID: add without @Id annotation
			boolean skipId = hasCompositeId && isPk;
			BasicFieldBuilder.addBasicField(entityClass, columnMetadata, modelsContext, skipId);
		}

		// Detect FK columns that are part of the composite PK (key-many-to-one)
		// Only when preferBasicCompositeIds is false — otherwise keep basic attribute overrides
		Set<String> keyManyToOneFkColumns = new HashSet<>();
		if (tableMetadata.getCompositeId() != null && !preferBasicCompositeIds) {
			Set<String> pkColumnNames = new HashSet<>();
			for (ColumnMetadata col : tableMetadata.getColumns()) {
				if (col.isPrimaryKey()) {
					pkColumnNames.add(col.getColumnName());
				}
			}
			CompositeIdMetadata compositeId = tableMetadata.getCompositeId();
			for (ForeignKeyMetadata fkMetadata : tableMetadata.getForeignKeys()) {
				if (pkColumnNames.contains(fkMetadata.getForeignKeyColumnName())) {
					// This FK column is part of the composite PK — convert to key-many-to-one
					compositeId.addKeyManyToOne(
						fkMetadata.getFieldName(),
						fkMetadata.getForeignKeyColumnName(),
						fkMetadata.getTargetEntityClassName(),
						fkMetadata.getTargetEntityPackage());
					// Remove the basic attribute override for this column
					compositeId.getAttributeOverrides().removeIf(
						ao -> ao.getColumnName().equals(fkMetadata.getForeignKeyColumnName()));
					keyManyToOneFkColumns.add(fkMetadata.getForeignKeyColumnName());
				}
			}
		}

		// Add ManyToOne relationship fields
		for (ForeignKeyMetadata fkMetadata : tableMetadata.getForeignKeys()) {
			String targetClassName = qualifiedName(fkMetadata.getTargetEntityPackage(),
				fkMetadata.getTargetEntityClassName());
			ClassDetails targetClassDetails = resolveOrCreateClassDetails(
				fkMetadata.getTargetEntityClassName(), targetClassName);
			ManyToOneFieldBuilder.buildManyToOneField(
				entityClass, fkMetadata, targetClassDetails, modelsContext);
		}

		// Add OneToMany relationship fields (deduplicate names to avoid collisions)
		for (OneToManyMetadata o2mMetadata : tableMetadata.getOneToManys()) {
			String elementClassName = qualifiedName(o2mMetadata.getElementEntityPackage(),
				o2mMetadata.getElementEntityClassName());
			ClassDetails elementClassDetails = resolveOrCreateClassDetails(
				o2mMetadata.getElementEntityClassName(), elementClassName);
			String uniqueName = makeUniqueFieldName(entityClass, o2mMetadata.getFieldName());
			OneToManyFieldBuilder.buildOneToManyField(
				entityClass, uniqueName, o2mMetadata, elementClassDetails, modelsContext);
		}

		// Add OneToOne relationship fields
		for (OneToOneMetadata o2oMetadata : tableMetadata.getOneToOnes()) {
			String targetClassName = qualifiedName(o2oMetadata.getTargetEntityPackage(),
				o2oMetadata.getTargetEntityClassName());
			ClassDetails targetClassDetails = resolveOrCreateClassDetails(
				o2oMetadata.getTargetEntityClassName(), targetClassName);
			OneToOneFieldBuilder.buildOneToOneField(
				entityClass, o2oMetadata, targetClassDetails, modelsContext);
		}

		// Add ManyToMany relationship fields (deduplicate names to avoid collisions)
		for (ManyToManyMetadata m2mMetadata : tableMetadata.getManyToManys()) {
			String targetClassName = qualifiedName(m2mMetadata.getTargetEntityPackage(),
				m2mMetadata.getTargetEntityClassName());
			ClassDetails targetClassDetails = resolveOrCreateClassDetails(
				m2mMetadata.getTargetEntityClassName(), targetClassName);
			String uniqueName = makeUniqueFieldName(entityClass, m2mMetadata.getFieldName());
			ManyToManyFieldBuilder.buildManyToManyField(
				entityClass, uniqueName, m2mMetadata, targetClassDetails, modelsContext);
		}

		// Add Embedded fields
		for (EmbeddedFieldMetadata embeddedMetadata : tableMetadata.getEmbeddedFields()) {
			String embeddableClassName = qualifiedName(embeddedMetadata.getEmbeddablePackage(),
				embeddedMetadata.getEmbeddableClassName());
			ClassDetails embeddableClassDetails = resolveOrCreateClassDetails(
				embeddedMetadata.getEmbeddableClassName(), embeddableClassName);
			EmbeddedFieldBuilder.buildEmbeddedField(
				entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);
		}

		// Add @EmbeddedId composite key field
		if (tableMetadata.getCompositeId() != null) {
			CompositeIdMetadata compositeId = tableMetadata.getCompositeId();
			String idClassName = qualifiedName(compositeId.getIdClassPackage(), compositeId.getIdClassName());
			ClassDetails idClassDetails = resolveOrCreateClassDetails(
				compositeId.getIdClassName(), idClassName);
			CompositeIdFieldBuilder.buildCompositeIdField(
				entityClass, compositeId, idClassDetails, modelsContext);
			embeddableClassDetails.add(idClassDetails);
		}

		// Register in the context
		registerClassDetails(entityClass);

		// Store the original table metadata for later use (e.g., documentation)
		tableMetadataByClassName.put(className, tableMetadata);

		return entityClass;
	}

	private void addEntityAnnotation(DynamicClassDetails entityClass, String entityName) {
        EntityJpaAnnotation entityAnnotation = JpaAnnotations.ENTITY.createUsage(modelsContext);
		entityAnnotation.name(entityName);
		entityClass.addAnnotationUsage(entityAnnotation);
	}

	private void addTableAnnotation(DynamicClassDetails entityClass, TableMetadata tableMetadata) {
        TableJpaAnnotation tableAnnotation = JpaAnnotations.TABLE.createUsage(modelsContext);
		tableAnnotation.name(tableMetadata.getTableName());

		if (tableMetadata.getSchema() != null) {
			tableAnnotation.schema(tableMetadata.getSchema());
		}
		if (tableMetadata.getCatalog() != null) {
			tableAnnotation.catalog(tableMetadata.getCatalog());
		}

		// Add unique constraints from unique indexes, excluding PK-only indexes
		Set<String> pkColumnNames = new HashSet<>();
		for (ColumnMetadata col : tableMetadata.getColumns()) {
			if (col.isPrimaryKey()) {
				pkColumnNames.add(col.getColumnName());
			}
		}
		List<IndexMetadata> uniqueIndexes = new ArrayList<>();
		for (IndexMetadata index : tableMetadata.getIndexes()) {
			if (index.isUnique() && !pkColumnNames.containsAll(index.getColumnNames())) {
				uniqueIndexes.add(index);
			}
		}
		if (!uniqueIndexes.isEmpty()) {
			jakarta.persistence.UniqueConstraint[] uniqueConstraints =
				new jakarta.persistence.UniqueConstraint[uniqueIndexes.size()];
			for (int i = 0; i < uniqueIndexes.size(); i++) {
				IndexMetadata idx = uniqueIndexes.get(i);
				UniqueConstraintJpaAnnotation uc =
					JpaAnnotations.UNIQUE_CONSTRAINT.createUsage(modelsContext);
				uc.name(idx.getIndexName());
				uc.columnNames(idx.getColumnNames().toArray(new String[0]));
				uniqueConstraints[i] = uc;
			}
			tableAnnotation.uniqueConstraints(uniqueConstraints);
		}

		entityClass.addAnnotationUsage(tableAnnotation);
	}

	private void addInheritanceAnnotations(DynamicClassDetails entityClass, TableMetadata tableMetadata) {

        // Add @Inheritance on root entities
		InheritanceMetadata inheritance = tableMetadata.getInheritance();
		if (inheritance != null) {
			InheritanceJpaAnnotation inheritanceAnnotation = JpaAnnotations.INHERITANCE.createUsage(modelsContext);
			inheritanceAnnotation.strategy(inheritance.getStrategy());
			entityClass.addAnnotationUsage(inheritanceAnnotation);

			// Add @DiscriminatorColumn on root entities with discriminator-based strategies
			if (inheritance.getDiscriminatorColumnName() != null) {
				DiscriminatorColumnJpaAnnotation discColAnnotation =
					JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(modelsContext);
				discColAnnotation.name(inheritance.getDiscriminatorColumnName());
				if (inheritance.getDiscriminatorType() != null) {
					discColAnnotation.discriminatorType(inheritance.getDiscriminatorType());
				}
				if (inheritance.getDiscriminatorColumnLength() > 0) {
					discColAnnotation.length(inheritance.getDiscriminatorColumnLength());
				}
				entityClass.addAnnotationUsage(discColAnnotation);
			}
		}

		// Add @DiscriminatorValue on entities in discriminator-based hierarchies
		if (tableMetadata.getDiscriminatorValue() != null) {
			DiscriminatorValueJpaAnnotation discValAnnotation =
				JpaAnnotations.DISCRIMINATOR_VALUE.createUsage(modelsContext);
			discValAnnotation.value(tableMetadata.getDiscriminatorValue());
			entityClass.addAnnotationUsage(discValAnnotation);
		}

		// Add @PrimaryKeyJoinColumn on subclasses in JOINED strategy
		if (tableMetadata.getPrimaryKeyJoinColumnName() != null) {
			PrimaryKeyJoinColumnJpaAnnotation pkJoinColAnnotation =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(modelsContext);
			pkJoinColAnnotation.name(tableMetadata.getPrimaryKeyJoinColumnName());
			entityClass.addAnnotationUsage(pkJoinColAnnotation);
		}
	}

	private static String qualifiedName(String packageName, String simpleName) {
		if (packageName == null || packageName.isEmpty()) {
			return simpleName;
		}
		return packageName + "." + simpleName;
	}

	/**
	 * Makes a field name unique within the given class by appending a
	 * numeric suffix (_1, _2, ...) when a field with the same name
	 * already exists.
	 */
	private static String makeUniqueFieldName(DynamicClassDetails entityClass, String fieldName) {
		Set<String> existingNames = new HashSet<>();
		for (var field : entityClass.getFields()) {
			existingNames.add(field.getName());
		}
		String candidate = fieldName;
		int cnt = 0;
		while (existingNames.contains(candidate)) {
			cnt++;
			candidate = fieldName + "_" + cnt;
		}
		return candidate;
	}

	/**
	 * Resolves a ClassDetails from the registry, or creates a dynamic placeholder
	 * if the class hasn't been built yet. This handles forward references where
	 * a relationship references an entity that will be built later.
	 */
	private ClassDetails resolveOrCreateClassDetails(String simpleName, String className) {
		MutableClassDetailsRegistry registry =
			(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		return registry.resolveClassDetails(
			className,
			name -> new DynamicClassDetails(simpleName, name, Object.class, false, null, null, modelsContext)
		);
	}

	private void registerClassDetails(ClassDetails classDetails) {
		MutableClassDetailsRegistry registry =
			(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		registry.addClassDetails(classDetails);
	}

	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	/**
	 * Returns the list of embeddable ID classes created during entity building.
	 * These need to be included in the entity list for exporters to render them.
	 */
	public List<ClassDetails> getEmbeddableClassDetails() {
		return embeddableClassDetails;
	}

	/**
	 * Returns the original {@link TableMetadata} that was used to create
	 * the entity with the given fully qualified class name, or {@code null}
	 * if no such entity was created by this builder.
	 */
	public TableMetadata getTableMetadata(String className) {
		return tableMetadataByClassName.get(className);
	}

	/**
	 * Returns an unmodifiable map of all stored table metadata, keyed
	 * by fully qualified entity class name.
	 */
	public Map<String, TableMetadata> getTableMetadataMap() {
		return Collections.unmodifiableMap(tableMetadataByClassName);
	}
}
