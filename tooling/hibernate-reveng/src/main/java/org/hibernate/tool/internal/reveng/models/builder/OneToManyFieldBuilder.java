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
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ParameterizedTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;

/**
 * Builds a {@code @OneToMany} field on a dynamic class and attaches
 * the appropriate JPA annotations based on {@link OneToManyMetadata}.
 *
 * @author Koen Aers
 */
public class OneToManyFieldBuilder {

	/**
	 * Creates a {@code @OneToMany} field on the given class and attaches
	 * the appropriate JPA annotations.
	 *
	 * @param entityClass         The dynamic class to add the field to
	 * @param o2mMetadata         The one-to-many relationship metadata
	 * @param elementClassDetails The resolved ClassDetails for the element entity
	 * @param modelsContext       The models context for creating annotations
	 */
	public static void buildOneToManyField(
			DynamicClassDetails entityClass,
			OneToManyMetadata o2mMetadata,
			ClassDetails elementClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, o2mMetadata, elementClassDetails, modelsContext);
		addOneToManyAnnotation(field, o2mMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			OneToManyMetadata o2mMetadata,
			ClassDetails elementClassDetails,
			ModelsContext modelsContext) {
		ClassDetails setClassDetails = modelsContext.getClassDetailsRegistry()
			.resolveClassDetails(Set.class.getName());

		TypeDetails elementType = new ClassTypeDetailsImpl(
			elementClassDetails,
			TypeDetails.Kind.CLASS
		);

		TypeDetails fieldType = new ParameterizedTypeDetailsImpl(
			setClassDetails,
			Collections.singletonList(elementType),
			null
		);

		return entityClass.applyAttribute(
			o2mMetadata.getFieldName(),
			fieldType,
			false,
			true,
			modelsContext
		);
	}

	private static void addOneToManyAnnotation(
			MutableAnnotationTarget field,
			OneToManyMetadata o2mMetadata,
			ModelsContext modelsContext) {
		OneToManyJpaAnnotation oneToManyAnnotation =
			JpaAnnotations.ONE_TO_MANY.createUsage(modelsContext);
		oneToManyAnnotation.mappedBy(o2mMetadata.getMappedBy());
		if (o2mMetadata.getFetchType() != null) {
			oneToManyAnnotation.fetch(o2mMetadata.getFetchType());
		}
		if (o2mMetadata.getCascadeTypes() != null) {
			oneToManyAnnotation.cascade(o2mMetadata.getCascadeTypes());
		}
		oneToManyAnnotation.orphanRemoval(o2mMetadata.isOrphanRemoval());
		field.addAnnotationUsage(oneToManyAnnotation);
	}
}
