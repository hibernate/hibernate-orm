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

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.reveng.internal.descriptor.ForeignKeyDescriptor;

import java.util.List;

/**
 * Builds a {@code @ManyToOne} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @ManyToOne},
 * {@code @JoinColumn}) based on {@link ForeignKeyDescriptor}.
 *
 * @author Koen Aers
 */
public class ManyToOneFieldBuilder {

	/**
	 * Creates a {@code @ManyToOne} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass        The dynamic class to add the field to
	 * @param fkMetadata         The foreign key metadata
	 * @param targetClassDetails The resolved ClassDetails for the target entity
	 * @param modelsContext      The models context for creating annotations
	 */
	public static void buildManyToOneField(
			DynamicClassDetails entityClass,
			ForeignKeyDescriptor fkMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, fkMetadata, targetClassDetails, modelsContext);
		addManyToOneAnnotation(field, fkMetadata, modelsContext);
		addJoinColumnAnnotation(field, fkMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			ForeignKeyDescriptor fkMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		TypeDetails fieldType = new ClassTypeDetailsImpl(
			targetClassDetails,
			TypeDetails.Kind.CLASS
		);
		return entityClass.applyAttribute(
			fkMetadata.getFieldName(),
			fieldType,
			false,
			false,
			modelsContext
		);
	}

	private static void addManyToOneAnnotation(
			MutableAnnotationTarget field,
			ForeignKeyDescriptor fkMetadata,
			ModelsContext modelsContext) {
		ManyToOneJpaAnnotation manyToOneAnnotation =
			JpaAnnotations.MANY_TO_ONE.createUsage(modelsContext);
		if (fkMetadata.getFetchType() != null) {
			manyToOneAnnotation.fetch(fkMetadata.getFetchType());
		}
		manyToOneAnnotation.optional(fkMetadata.isOptional());
		field.addAnnotationUsage(manyToOneAnnotation);
	}

	private static void addJoinColumnAnnotation(
			MutableAnnotationTarget field,
			ForeignKeyDescriptor fkMetadata,
			ModelsContext modelsContext) {
		List<ForeignKeyDescriptor.JoinColumnPair> joinColumns = fkMetadata.getJoinColumns();
		boolean readOnly = fkMetadata.isPartOfCompositeKey();
		if (joinColumns.size() == 1) {
			ForeignKeyDescriptor.JoinColumnPair jc = joinColumns.get(0);
			JoinColumnJpaAnnotation joinColumnAnnotation =
				JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
			joinColumnAnnotation.name(jc.fkColumnName());
			if (jc.referencedColumnName() != null) {
				joinColumnAnnotation.referencedColumnName(jc.referencedColumnName());
			}
			joinColumnAnnotation.nullable(fkMetadata.isOptional());
			if (readOnly) {
				joinColumnAnnotation.insertable(false);
				joinColumnAnnotation.updatable(false);
			}
			field.addAnnotationUsage(joinColumnAnnotation);
		} else {
			jakarta.persistence.JoinColumn[] jcArray =
				new jakarta.persistence.JoinColumn[joinColumns.size()];
			for (int i = 0; i < joinColumns.size(); i++) {
				ForeignKeyDescriptor.JoinColumnPair jc = joinColumns.get(i);
				JoinColumnJpaAnnotation joinColumnAnnotation =
					JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
				joinColumnAnnotation.name(jc.fkColumnName());
				if (jc.referencedColumnName() != null) {
					joinColumnAnnotation.referencedColumnName(jc.referencedColumnName());
				}
				if (readOnly) {
					joinColumnAnnotation.insertable(false);
					joinColumnAnnotation.updatable(false);
				}
				jcArray[i] = joinColumnAnnotation;
			}
			JoinColumnsJpaAnnotation joinColumnsAnnotation =
				JpaAnnotations.JOIN_COLUMNS.createUsage(modelsContext);
			joinColumnsAnnotation.value(jcArray);
			field.addAnnotationUsage(joinColumnsAnnotation);
		}
	}
}
