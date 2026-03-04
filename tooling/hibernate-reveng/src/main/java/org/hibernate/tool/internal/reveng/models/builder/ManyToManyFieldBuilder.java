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

import java.util.Collections;
import java.util.Set;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;

/**
 * Builds a {@code @ManyToMany} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @ManyToMany},
 * {@code @JoinTable}) based on {@link ManyToManyMetadata}.
 *
 * @author Koen Aers
 */
public class ManyToManyFieldBuilder {

	/**
	 * Creates a {@code @ManyToMany} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass        The dynamic class to add the field to
	 * @param m2mMetadata        The many-to-many relationship metadata
	 * @param targetClassDetails The resolved ClassDetails for the target entity
	 * @param modelsContext      The models context for creating annotations
	 */
	public static void buildManyToManyField(
			DynamicClassDetails entityClass,
			ManyToManyMetadata m2mMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, m2mMetadata, targetClassDetails, modelsContext);
		addManyToManyAnnotation(field, m2mMetadata, modelsContext);
		addJoinTableAnnotation(field, m2mMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			ManyToManyMetadata m2mMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		ClassDetails setClassDetails = modelsContext.getClassDetailsRegistry()
			.resolveClassDetails(Set.class.getName());

		TypeDetails elementType = new ClassTypeDetailsImpl(
			targetClassDetails,
			TypeDetails.Kind.CLASS
		);

		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
			setClassDetails,
			Collections.singletonList(elementType),
			null
		);

		return entityClass.applyAttribute(
			m2mMetadata.getFieldName(),
			fieldType,
			false,
			true,
			modelsContext
		);
	}

	private static void addManyToManyAnnotation(
			MutableAnnotationTarget field,
			ManyToManyMetadata m2mMetadata,
			ModelsContext modelsContext) {
		ManyToManyJpaAnnotation manyToManyAnnotation =
			JpaAnnotations.MANY_TO_MANY.createUsage(modelsContext);
		if (m2mMetadata.getMappedBy() != null) {
			manyToManyAnnotation.mappedBy(m2mMetadata.getMappedBy());
		}
		if (m2mMetadata.getFetchType() != null) {
			manyToManyAnnotation.fetch(m2mMetadata.getFetchType());
		}
		if (m2mMetadata.getCascadeTypes() != null) {
			manyToManyAnnotation.cascade(m2mMetadata.getCascadeTypes());
		}
		field.addAnnotationUsage(manyToManyAnnotation);
	}

	private static void addJoinTableAnnotation(
			MutableAnnotationTarget field,
			ManyToManyMetadata m2mMetadata,
			ModelsContext modelsContext) {
		if (m2mMetadata.getMappedBy() == null && m2mMetadata.getJoinTableName() != null) {
			JoinTableJpaAnnotation joinTableAnnotation =
				JpaAnnotations.JOIN_TABLE.createUsage(modelsContext);
			joinTableAnnotation.name(m2mMetadata.getJoinTableName());

			if (m2mMetadata.getJoinColumnName() != null) {
				JoinColumnJpaAnnotation joinColumn =
					JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
				joinColumn.name(m2mMetadata.getJoinColumnName());
				joinTableAnnotation.joinColumns(
					new jakarta.persistence.JoinColumn[]{ joinColumn });
			}

			if (m2mMetadata.getInverseJoinColumnName() != null) {
				JoinColumnJpaAnnotation inverseJoinColumn =
					JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
				inverseJoinColumn.name(m2mMetadata.getInverseJoinColumnName());
				joinTableAnnotation.inverseJoinColumns(
					new jakarta.persistence.JoinColumn[]{ inverseJoinColumn });
			}

			field.addAnnotationUsage(joinTableAnnotation);
		}
	}
}
