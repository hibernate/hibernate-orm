/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.UniqueConstraintJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.IndexDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.InheritanceDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;

class EntityAnnotationApplier {

	static void addEntityAnnotation(
			DynamicClassDetails entityClass, ModelsContext modelsContext) {
		EntityJpaAnnotation entityAnnotation =
				JpaAnnotations.ENTITY.createUsage(modelsContext);
		entityClass.addAnnotationUsage(entityAnnotation);
	}

	static void addTableAnnotation(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			ModelsContext modelsContext) {
		TableJpaAnnotation tableAnnotation =
				JpaAnnotations.TABLE.createUsage(modelsContext);
		tableAnnotation.name(tableMetadata.getTableName());
		if (tableMetadata.getSchema() != null) {
			tableAnnotation.schema(tableMetadata.getSchema());
		}
		if (tableMetadata.getCatalog() != null) {
			tableAnnotation.catalog(tableMetadata.getCatalog());
		}
		jakarta.persistence.UniqueConstraint[] uniqueConstraints =
				buildUniqueConstraints(tableMetadata, modelsContext);
		if (uniqueConstraints.length > 0) {
			tableAnnotation.uniqueConstraints(uniqueConstraints);
		}
		entityClass.addAnnotationUsage(tableAnnotation);
	}

	static void addInheritanceAnnotations(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			ModelsContext modelsContext) {
		addInheritanceStrategy(entityClass, tableMetadata, modelsContext);
		addDiscriminatorValue(entityClass, tableMetadata, modelsContext);
		addPrimaryKeyJoinColumn(entityClass, tableMetadata, modelsContext);
	}

	private static jakarta.persistence.UniqueConstraint[] buildUniqueConstraints(
			TableDescriptor tableMetadata, ModelsContext modelsContext) {
		Set<String> pkColumnNames = collectPrimaryKeyColumnNames(tableMetadata);
		List<IndexDescriptor> uniqueIndexes = new ArrayList<>();
		for (IndexDescriptor index : tableMetadata.getIndexes()) {
			if (index.isUnique()
					&& !pkColumnNames.containsAll(index.getColumnNames())) {
				uniqueIndexes.add(index);
			}
		}
		if (uniqueIndexes.isEmpty()) {
			return new jakarta.persistence.UniqueConstraint[0];
		}
		jakarta.persistence.UniqueConstraint[] result =
				new jakarta.persistence.UniqueConstraint[uniqueIndexes.size()];
		for (int i = 0; i < uniqueIndexes.size(); i++) {
			IndexDescriptor idx = uniqueIndexes.get(i);
			UniqueConstraintJpaAnnotation uc =
					JpaAnnotations.UNIQUE_CONSTRAINT.createUsage(modelsContext);
			uc.name(idx.getIndexName());
			uc.columnNames(idx.getColumnNames().toArray(new String[0]));
			result[i] = uc;
		}
		return result;
	}

	private static void addInheritanceStrategy(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			ModelsContext modelsContext) {
		InheritanceDescriptor inheritance = tableMetadata.getInheritance();
		if (inheritance == null) {
			return;
		}
		InheritanceJpaAnnotation inheritanceAnnotation =
				JpaAnnotations.INHERITANCE.createUsage(modelsContext);
		inheritanceAnnotation.strategy(inheritance.getStrategy());
		entityClass.addAnnotationUsage(inheritanceAnnotation);
		if (inheritance.getDiscriminatorColumnName() != null) {
			DiscriminatorColumnJpaAnnotation discColAnnotation =
					JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(modelsContext);
			discColAnnotation.name(inheritance.getDiscriminatorColumnName());
			if (inheritance.getDiscriminatorType() != null) {
				discColAnnotation.discriminatorType(
						inheritance.getDiscriminatorType());
			}
			if (inheritance.getDiscriminatorColumnLength() > 0) {
				discColAnnotation.length(
						inheritance.getDiscriminatorColumnLength());
			}
			entityClass.addAnnotationUsage(discColAnnotation);
		}
	}

	private static void addDiscriminatorValue(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			ModelsContext modelsContext) {
		if (tableMetadata.getDiscriminatorValue() == null) {
			return;
		}
		DiscriminatorValueJpaAnnotation discValAnnotation =
				JpaAnnotations.DISCRIMINATOR_VALUE.createUsage(modelsContext);
		discValAnnotation.value(tableMetadata.getDiscriminatorValue());
		entityClass.addAnnotationUsage(discValAnnotation);
	}

	private static void addPrimaryKeyJoinColumn(
			DynamicClassDetails entityClass,
			TableDescriptor tableMetadata,
			ModelsContext modelsContext) {
		if (tableMetadata.getPrimaryKeyJoinColumnName() == null) {
			return;
		}
		PrimaryKeyJoinColumnJpaAnnotation pkJoinColAnnotation =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(modelsContext);
		pkJoinColAnnotation.name(tableMetadata.getPrimaryKeyJoinColumnName());
		entityClass.addAnnotationUsage(pkJoinColAnnotation);
	}

	static Set<String> collectPrimaryKeyColumnNames(
			TableDescriptor tableMetadata) {
		Set<String> pkColumnNames = new HashSet<>();
		for (ColumnDescriptor col : tableMetadata.getColumns()) {
			if (col.isPrimaryKey()) {
				pkColumnNames.add(col.getColumnName());
			}
		}
		return pkColumnNames;
	}
}
