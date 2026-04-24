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
package org.hibernate.tool.reveng.internal.builder.db;

import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.CompositeIdDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.EmbeddedFieldDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ForeignKeyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ManyToManyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.OneToManyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.OneToOneDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private final Map<String, TableDescriptor> tableMetadataByClassName = new LinkedHashMap<>();
	private final Map<String, Map<String, List<String>>> allClassMetaAttributes = new LinkedHashMap<>();
	private final Map<String, Map<String, Map<String, List<String>>>> allFieldMetaAttributes = new LinkedHashMap<>();
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
	public ClassDetails createEntityFromTable(TableDescriptor tableMetadata) {
		String className = qualifiedName(
				tableMetadata.getEntityPackage(),
				tableMetadata.getEntityClassName());
		DynamicClassDetails entityClass = createEntityClass(className, tableMetadata);
		EntityAnnotationApplier.addEntityAnnotation(entityClass, modelsContext);
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, tableMetadata, modelsContext);
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, tableMetadata, modelsContext);
		buildBasicFields(entityClass, tableMetadata);
		Set<String> keyManyToOneFkColumns =
				handleCompositeKeyForeignKeys(tableMetadata);
		buildRelationshipFields(entityClass, tableMetadata, keyManyToOneFkColumns);
		buildCompositeId(entityClass, tableMetadata);
		registerClassDetails(entityClass);
		tableMetadataByClassName.put(className, tableMetadata);
		extractMetaAttributes(className, tableMetadata);
		return entityClass;
	}

	private DynamicClassDetails createEntityClass(
			String className, TableDescriptor tableMetadata) {
		ClassDetails superClass = null;
		if (tableMetadata.getParentEntityClassName() != null) {
			String parentClassName = qualifiedName(
					tableMetadata.getParentEntityPackage(),
					tableMetadata.getParentEntityClassName());
			superClass = resolveOrCreateClassDetails(
					tableMetadata.getParentEntityClassName(), parentClassName);
		}
		return new DynamicClassDetails(
				className, className, Object.class,
				false, superClass, null, modelsContext);
	}

	private void buildBasicFields(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		boolean hasCompositeId = tableMetadata.getCompositeId() != null;
		Set<String> constrainedO2OCols = collectConstrainedOneToOneColumns(tableMetadata);
		for (ColumnDescriptor col : tableMetadata.getColumns()) {
			boolean isFk = tableMetadata.isForeignKeyColumn(col.getColumnName());
			boolean isPk = col.isPrimaryKey();
			boolean isConstrainedO2O = constrainedO2OCols.contains(col.getColumnName());
			if (isFk && !(isPk && isConstrainedO2O)) continue;
			if (hasCompositeId && isPk) continue;
			BasicFieldBuilder.addBasicField(entityClass, col, modelsContext, false);
		}
	}

	private Set<String> collectConstrainedOneToOneColumns(
			TableDescriptor tableMetadata) {
		Set<String> result = new HashSet<>();
		for (OneToOneDescriptor o2o : tableMetadata.getOneToOnes()) {
			if (o2o.isConstrained()) {
				result.addAll(o2o.getForeignKeyColumnNames());
			}
		}
		return result;
	}

	private Set<String> handleCompositeKeyForeignKeys(
			TableDescriptor tableMetadata) {
		if (tableMetadata.getCompositeId() == null) {
			return Set.of();
		}
		Set<String> pkColumnNames =
				EntityAnnotationApplier.collectPrimaryKeyColumnNames(tableMetadata);
		if (!preferBasicCompositeIds) {
			return convertToKeyManyToOne(tableMetadata, pkColumnNames);
		}
		markCompositeKeyForeignKeys(tableMetadata, pkColumnNames);
		return Set.of();
	}

	private Set<String> convertToKeyManyToOne(
			TableDescriptor tableMetadata, Set<String> pkColumnNames) {
		Set<String> keyManyToOneFkColumns = new HashSet<>();
		CompositeIdDescriptor compositeId = tableMetadata.getCompositeId();
		for (ForeignKeyDescriptor fk : tableMetadata.getForeignKeys()) {
			List<String> fkCols = fk.getForeignKeyColumnNames();
			if (!pkColumnNames.containsAll(fkCols)) continue;
			compositeId.addKeyManyToOne(
					fk.getFieldName(), fkCols,
					fk.getTargetEntityClassName(),
					fk.getTargetEntityPackage());
			for (String fkCol : fkCols) {
				compositeId.getAttributeOverrides().removeIf(
						ao -> ao.getColumnName().equals(fkCol));
				keyManyToOneFkColumns.add(fkCol);
			}
		}
		return keyManyToOneFkColumns;
	}

	private void markCompositeKeyForeignKeys(
			TableDescriptor tableMetadata, Set<String> pkColumnNames) {
		for (ForeignKeyDescriptor fk : tableMetadata.getForeignKeys()) {
			if (pkColumnNames.containsAll(fk.getForeignKeyColumnNames())) {
				fk.partOfCompositeKey(true);
			}
		}
	}

	private void buildRelationshipFields(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			Set<String> keyManyToOneFkColumns) {
		buildManyToOneFields(entityClass, tableMetadata, keyManyToOneFkColumns);
		buildOneToManyFields(entityClass, tableMetadata);
		buildOneToOneFields(entityClass, tableMetadata);
		buildManyToManyFields(entityClass, tableMetadata);
		buildEmbeddedFields(entityClass, tableMetadata);
	}

	private void buildManyToOneFields(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			Set<String> keyManyToOneFkColumns) {
		for (ForeignKeyDescriptor fk : tableMetadata.getForeignKeys()) {
			if (keyManyToOneFkColumns.containsAll(fk.getForeignKeyColumnNames())) {
				continue;
			}
			String targetClassName = qualifiedName(
					fk.getTargetEntityPackage(), fk.getTargetEntityClassName());
			ClassDetails target = resolveOrCreateClassDetails(
					fk.getTargetEntityClassName(), targetClassName);
			ManyToOneFieldBuilder.buildManyToOneField(
					entityClass, fk, target, modelsContext);
		}
	}

	private void buildOneToManyFields(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		for (OneToManyDescriptor o2m : tableMetadata.getOneToManys()) {
			String elementClassName = qualifiedName(
					o2m.getElementEntityPackage(), o2m.getElementEntityClassName());
			ClassDetails element = resolveOrCreateClassDetails(
					o2m.getElementEntityClassName(), elementClassName);
			String uniqueName = makeUniqueFieldName(entityClass, o2m.getFieldName());
			OneToManyFieldBuilder.buildOneToManyField(
					entityClass, uniqueName, o2m, element, modelsContext);
		}
	}

	private void buildOneToOneFields(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		for (OneToOneDescriptor o2o : tableMetadata.getOneToOnes()) {
			String targetClassName = qualifiedName(
					o2o.getTargetEntityPackage(), o2o.getTargetEntityClassName());
			ClassDetails target = resolveOrCreateClassDetails(
					o2o.getTargetEntityClassName(), targetClassName);
			OneToOneFieldBuilder.buildOneToOneField(
					entityClass, o2o, target, modelsContext);
		}
	}

	private void buildManyToManyFields(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		for (ManyToManyDescriptor m2m : tableMetadata.getManyToManys()) {
			String targetClassName = qualifiedName(
					m2m.getTargetEntityPackage(), m2m.getTargetEntityClassName());
			ClassDetails target = resolveOrCreateClassDetails(
					m2m.getTargetEntityClassName(), targetClassName);
			String uniqueName = makeUniqueFieldName(entityClass, m2m.getFieldName());
			ManyToManyFieldBuilder.buildManyToManyField(
					entityClass, uniqueName, m2m, target, modelsContext);
		}
	}

	private void buildEmbeddedFields(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		for (EmbeddedFieldDescriptor emb : tableMetadata.getEmbeddedFields()) {
			String embClassName = qualifiedName(
					emb.getEmbeddablePackage(), emb.getEmbeddableClassName());
			ClassDetails embClass = resolveOrCreateClassDetails(
					emb.getEmbeddableClassName(), embClassName);
			EmbeddedFieldBuilder.buildEmbeddedField(
					entityClass, emb, embClass, modelsContext);
		}
	}

	private void buildCompositeId(
			DynamicClassDetails entityClass, TableDescriptor tableMetadata) {
		if (tableMetadata.getCompositeId() == null) return;
		CompositeIdDescriptor compositeId = tableMetadata.getCompositeId();
		String idClassName = qualifiedName(
				compositeId.getIdClassPackage(), compositeId.getIdClassName());
		ClassDetails idClassDetails = resolveOrCreateClassDetails(
				compositeId.getIdClassName(), idClassName);
		CompositeIdFieldBuilder.buildCompositeIdField(
				entityClass, compositeId, idClassDetails, modelsContext);
		embeddableClassDetails.add(idClassDetails);
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
			name -> new DynamicClassDetails(name, name, Object.class, false, null, null, modelsContext)
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
	 * Returns the original {@link TableDescriptor} that was used to create
	 * the entity with the given fully qualified class name, or {@code null}
	 * if no such entity was created by this builder.
	 */
	public TableDescriptor getTableDescriptor(String className) {
		return tableMetadataByClassName.get(className);
	}

	/**
	 * Returns an unmodifiable map of all stored table metadata, keyed
	 * by fully qualified entity class name.
	 */
	public Map<String, TableDescriptor> getTableDescriptorMap() {
		return Collections.unmodifiableMap(tableMetadataByClassName);
	}

	/**
	 * Returns all class-level meta-attributes, keyed by fully qualified
	 * entity class name, then by attribute name.
	 */
	public Map<String, Map<String, List<String>>> getAllClassMetaAttributes() {
		return allClassMetaAttributes;
	}

	/**
	 * Returns all field-level meta-attributes, keyed by fully qualified
	 * entity class name, then by field name, then by attribute name.
	 */
	public Map<String, Map<String, Map<String, List<String>>>> getAllFieldMetaAttributes() {
		return allFieldMetaAttributes;
	}

	/**
	 * Extracts meta-attributes from {@link TableDescriptor} and its
	 * {@link ColumnDescriptor} entries, populating the class-level and
	 * field-level meta-attribute maps.
	 */
	private void extractMetaAttributes(String className, TableDescriptor tableMetadata) {
		Map<String, List<String>> tableMeta = tableMetadata.getMetaAttributes();
		if (!tableMeta.isEmpty()) {
			allClassMetaAttributes.put(className, tableMeta);
		}
		Map<String, Map<String, List<String>>> fieldMetas = new LinkedHashMap<>();
		for (ColumnDescriptor column : tableMetadata.getColumns()) {
			Map<String, List<String>> colMeta = column.getMetaAttributes();
			if (!colMeta.isEmpty() && column.getFieldName() != null) {
				fieldMetas.put(column.getFieldName(), colMeta);
			}
		}
		if (!fieldMetas.isEmpty()) {
			allFieldMetaAttributes.put(className, fieldMetas);
		}
	}
}
