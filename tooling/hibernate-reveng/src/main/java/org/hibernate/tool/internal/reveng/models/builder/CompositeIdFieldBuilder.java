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
import org.hibernate.boot.models.annotations.internal.EmbeddedIdJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.metadata.AttributeOverrideMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;

/**
 * Builds an {@code @EmbeddedId} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @EmbeddedId},
 * {@code @AttributeOverrides}) based on {@link CompositeIdMetadata}.
 *
 * @author Koen Aers
 */
public class CompositeIdFieldBuilder {

	/**
	 * Creates an {@code @EmbeddedId} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass    The dynamic class to add the field to
	 * @param compositeId    The composite ID metadata
	 * @param idClassDetails The resolved ClassDetails for the embeddable ID class
	 * @param modelsContext  The models context for creating annotations
	 */
	public static void buildCompositeIdField(
			DynamicClassDetails entityClass,
			CompositeIdMetadata compositeId,
			ClassDetails idClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, compositeId, idClassDetails, modelsContext);
		addEmbeddedIdAnnotation(field, modelsContext);
		addAttributeOverrides(field, compositeId, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			CompositeIdMetadata compositeId,
			ClassDetails idClassDetails,
			ModelsContext modelsContext) {
		TypeDetails fieldType = new ClassTypeDetailsImpl(
			idClassDetails,
			TypeDetails.Kind.CLASS
		);
		return entityClass.applyAttribute(
			compositeId.getFieldName(),
			fieldType,
			false,
			false,
			modelsContext
		);
	}

	private static void addEmbeddedIdAnnotation(
			MutableAnnotationTarget field,
			ModelsContext modelsContext) {
		EmbeddedIdJpaAnnotation embeddedIdAnnotation =
			JpaAnnotations.EMBEDDED_ID.createUsage(modelsContext);
		field.addAnnotationUsage(embeddedIdAnnotation);
	}

	private static void addAttributeOverrides(
			MutableAnnotationTarget field,
			CompositeIdMetadata compositeId,
			ModelsContext modelsContext) {
		List<AttributeOverrideMetadata> overrides = compositeId.getAttributeOverrides();
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
