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
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;

/**
 * Builds a {@code @OneToOne} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @OneToOne},
 * {@code @JoinColumn}) based on {@link OneToOneMetadata}.
 *
 * @author Koen Aers
 */
public class OneToOneFieldBuilder {

	/**
	 * Creates a {@code @OneToOne} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass        The dynamic class to add the field to
	 * @param o2oMetadata        The one-to-one relationship metadata
	 * @param targetClassDetails The resolved ClassDetails for the target entity
	 * @param modelsContext      The models context for creating annotations
	 */
	public static void buildOneToOneField(
			DynamicClassDetails entityClass,
			OneToOneMetadata o2oMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);
		addOneToOneAnnotation(field, o2oMetadata, modelsContext);
		addJoinColumnAnnotation(field, o2oMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			OneToOneMetadata o2oMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		TypeDetails fieldType = new ClassTypeDetailsImpl(
			targetClassDetails,
			TypeDetails.Kind.CLASS
		);
		return entityClass.applyAttribute(
			o2oMetadata.getFieldName(),
			fieldType,
			false,
			false,
			modelsContext
		);
	}

	private static void addOneToOneAnnotation(
			MutableAnnotationTarget field,
			OneToOneMetadata o2oMetadata,
			ModelsContext modelsContext) {
		OneToOneJpaAnnotation oneToOneAnnotation =
			JpaAnnotations.ONE_TO_ONE.createUsage(modelsContext);
		if (o2oMetadata.getMappedBy() != null) {
			oneToOneAnnotation.mappedBy(o2oMetadata.getMappedBy());
		}
		if (o2oMetadata.getFetchType() != null) {
			oneToOneAnnotation.fetch(o2oMetadata.getFetchType());
		}
		if (o2oMetadata.getCascadeTypes() != null) {
			oneToOneAnnotation.cascade(o2oMetadata.getCascadeTypes());
		}
		oneToOneAnnotation.optional(o2oMetadata.isOptional());
		oneToOneAnnotation.orphanRemoval(o2oMetadata.isOrphanRemoval());
		field.addAnnotationUsage(oneToOneAnnotation);
	}

	private static void addJoinColumnAnnotation(
			MutableAnnotationTarget field,
			OneToOneMetadata o2oMetadata,
			ModelsContext modelsContext) {
		if (o2oMetadata.getMappedBy() == null && o2oMetadata.getForeignKeyColumnName() != null) {
			JoinColumnJpaAnnotation joinColumnAnnotation =
				JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
			joinColumnAnnotation.name(o2oMetadata.getForeignKeyColumnName());
			if (o2oMetadata.getReferencedColumnName() != null) {
				joinColumnAnnotation.referencedColumnName(o2oMetadata.getReferencedColumnName());
			}
			joinColumnAnnotation.unique(true);
			joinColumnAnnotation.nullable(o2oMetadata.isOptional());
			field.addAnnotationUsage(joinColumnAnnotation);
		}
	}
}
