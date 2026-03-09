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
package org.hibernate.tool.internal.reveng.models.builder;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.LobJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.VersionJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.GenerationType;

/**
 * Builds a basic (column-mapped) field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @Id}, {@code @GeneratedValue},
 * {@code @Version}, {@code @Basic}, {@code @Temporal}, {@code @Lob},
 * {@code @Column}) based on {@link ColumnMetadata}.
 *
 * @author Koen Aers
 */
public class BasicFieldBuilder {

	/**
	 * Creates a field on the given class from column metadata and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass    The dynamic class to add the field to
	 * @param columnMetadata The column metadata describing the field
	 * @param modelsContext  The models context for resolving types and creating annotations
	 */
	public static void addBasicField(
			DynamicClassDetails entityClass,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(entityClass, columnMetadata, modelsContext);
		addIdAnnotation(field, columnMetadata, modelsContext);
		addVersionAnnotation(field, columnMetadata, modelsContext);
		addBasicAnnotation(field, columnMetadata, modelsContext);
		addTemporalAnnotation(field, columnMetadata, modelsContext);
		addLobAnnotation(field, columnMetadata, modelsContext);
		addColumnAnnotation(field, columnMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		ClassDetails fieldTypeClass = modelsContext.getClassDetailsRegistry()
			.resolveClassDetails(columnMetadata.getJavaType().getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(
			fieldTypeClass,
			TypeDetails.Kind.CLASS
		);
		return entityClass.applyAttribute(
			columnMetadata.getFieldName(),
			fieldType,
			false,  // isArray
			false,  // isPlural
			modelsContext
		);
	}

	private static void addIdAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		if (columnMetadata.isPrimaryKey()) {
			IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(modelsContext);
			field.addAnnotationUsage(idAnnotation);
			GenerationType genType = columnMetadata.getGenerationType();
			if (genType == null && columnMetadata.isAutoIncrement()) {
				genType = GenerationType.IDENTITY;
			}
			if (genType != null) {
				GeneratedValueJpaAnnotation generatedAnnotation =
					JpaAnnotations.GENERATED_VALUE.createUsage(modelsContext);
				generatedAnnotation.strategy(genType);
				field.addAnnotationUsage(generatedAnnotation);
			}
		}
	}

	private static void addVersionAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		if (columnMetadata.isVersion()) {
			VersionJpaAnnotation versionAnnotation = JpaAnnotations.VERSION.createUsage(modelsContext);
			field.addAnnotationUsage(versionAnnotation);
		}
	}

	private static void addBasicAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		if (columnMetadata.getBasicFetchType() != null || columnMetadata.isBasicOptionalSet()) {
			BasicJpaAnnotation basicAnnotation = JpaAnnotations.BASIC.createUsage(modelsContext);
			if (columnMetadata.getBasicFetchType() != null) {
				basicAnnotation.fetch(columnMetadata.getBasicFetchType());
			}
			if (columnMetadata.isBasicOptionalSet()) {
				basicAnnotation.optional(columnMetadata.isBasicOptional());
			}
			field.addAnnotationUsage(basicAnnotation);
		}
	}

	private static void addTemporalAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		if (columnMetadata.getTemporalType() != null) {
			TemporalJpaAnnotation temporalAnnotation = JpaAnnotations.TEMPORAL.createUsage(modelsContext);
			temporalAnnotation.value(columnMetadata.getTemporalType());
			field.addAnnotationUsage(temporalAnnotation);
		}
	}

	private static void addLobAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		if (columnMetadata.isLob()) {
			LobJpaAnnotation lobAnnotation = JpaAnnotations.LOB.createUsage(modelsContext);
			field.addAnnotationUsage(lobAnnotation);
		}
	}

	private static void addColumnAnnotation(
			MutableAnnotationTarget field,
			ColumnMetadata columnMetadata,
			ModelsContext modelsContext) {
		ColumnJpaAnnotation columnAnnotation = JpaAnnotations.COLUMN.createUsage(modelsContext);
		columnAnnotation.name(columnMetadata.getColumnName());
		columnAnnotation.nullable(columnMetadata.isNullable());
		if (columnMetadata.getLength() > 0) {
			columnAnnotation.length(columnMetadata.getLength());
		}
		if (columnMetadata.getPrecision() > 0) {
			columnAnnotation.precision(columnMetadata.getPrecision());
		}
		if (columnMetadata.getScale() > 0) {
			columnAnnotation.scale(columnMetadata.getScale());
		}
		if (columnMetadata.isUnique()) {
			columnAnnotation.unique(true);
		}
		field.addAnnotationUsage(columnAnnotation);
	}
}
