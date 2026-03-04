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

import java.util.List;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AttributeOverrideJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeOverridesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.metadata.AttributeOverrideMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;

/**
 * Builds an {@code @Embedded} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @Embedded},
 * {@code @AttributeOverrides}) based on {@link EmbeddedFieldMetadata}.
 *
 * @author Koen Aers
 */
public class EmbeddedFieldBuilder {

	/**
	 * Creates an {@code @Embedded} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass            The dynamic class to add the field to
	 * @param embeddedMetadata       The embedded field metadata
	 * @param embeddableClassDetails The resolved ClassDetails for the embeddable class
	 * @param modelsContext          The models context for creating annotations
	 */
	public static void buildEmbeddedField(
			DynamicClassDetails entityClass,
			EmbeddedFieldMetadata embeddedMetadata,
			ClassDetails embeddableClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, embeddedMetadata, embeddableClassDetails, modelsContext);
		addEmbeddedAnnotation(field, modelsContext);
		addAttributeOverrides(field, embeddedMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			EmbeddedFieldMetadata embeddedMetadata,
			ClassDetails embeddableClassDetails,
			ModelsContext modelsContext) {
		TypeDetails fieldType = new ClassTypeDetailsImpl(
			embeddableClassDetails,
			TypeDetails.Kind.CLASS
		);
		return entityClass.applyAttribute(
			embeddedMetadata.getFieldName(),
			fieldType,
			false,
			false,
			modelsContext
		);
	}

	private static void addEmbeddedAnnotation(
			MutableAnnotationTarget field,
			ModelsContext modelsContext) {
		EmbeddedJpaAnnotation embeddedAnnotation =
			JpaAnnotations.EMBEDDED.createUsage(modelsContext);
		field.addAnnotationUsage(embeddedAnnotation);
	}

	private static void addAttributeOverrides(
			MutableAnnotationTarget field,
			EmbeddedFieldMetadata embeddedMetadata,
			ModelsContext modelsContext) {
		List<AttributeOverrideMetadata> overrides = embeddedMetadata.getAttributeOverrides();
		if (!overrides.isEmpty()) {
			jakarta.persistence.AttributeOverride[] overrideArray =
				new jakarta.persistence.AttributeOverride[overrides.size()];

			for (int i = 0; i < overrides.size(); i++) {
				AttributeOverrideJpaAnnotation overrideAnnotation =
					JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage(modelsContext);
				overrideAnnotation.name(overrides.get(i).getFieldName());

				ColumnJpaAnnotation columnAnnotation =
					JpaAnnotations.COLUMN.createUsage(modelsContext);
				columnAnnotation.name(overrides.get(i).getColumnName());
				overrideAnnotation.column(columnAnnotation);

				overrideArray[i] = overrideAnnotation;
			}

			AttributeOverridesJpaAnnotation overridesContainer =
				JpaAnnotations.ATTRIBUTE_OVERRIDES.createUsage(modelsContext);
			overridesContainer.value(overrideArray);
			field.addAnnotationUsage(overridesContainer);
		}
	}
}
