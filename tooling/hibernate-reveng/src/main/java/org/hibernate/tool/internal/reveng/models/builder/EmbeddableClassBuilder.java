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
import org.hibernate.boot.models.annotations.internal.EmbeddableJpaAnnotation;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;

/**
 * Builds a dynamic {@code @Embeddable} class from {@link EmbeddableMetadata},
 * adding fields for each column and registering it in the models context.
 *
 * @author Koen Aers
 */
public class EmbeddableClassBuilder {

	/**
	 * Creates a ClassDetails representing an embeddable class from metadata.
	 *
	 * @param embeddableMetadata The embeddable class metadata
	 * @param modelsContext      The models context for creating annotations and registration
	 * @return ClassDetails with {@code @Embeddable} annotation attached
	 */
	public static ClassDetails buildEmbeddableClass(
			EmbeddableMetadata embeddableMetadata,
			ModelsContext modelsContext) {
		DynamicClassDetails embeddableClass = createClass(embeddableMetadata, modelsContext);
		addEmbeddableAnnotation(embeddableClass, modelsContext);
		addFields(embeddableClass, embeddableMetadata, modelsContext);
		registerClassDetails(embeddableClass, modelsContext);
		return embeddableClass;
	}

	private static DynamicClassDetails createClass(
			EmbeddableMetadata embeddableMetadata,
			ModelsContext modelsContext) {
		String className = embeddableMetadata.getPackageName()
			+ "." + embeddableMetadata.getClassName();
		return new DynamicClassDetails(
			embeddableMetadata.getClassName(),
			className,
			false,
			null,
			null,
			modelsContext
		);
	}

	private static void addEmbeddableAnnotation(
			MutableAnnotationTarget embeddableClass,
			ModelsContext modelsContext) {
		EmbeddableJpaAnnotation embeddableAnnotation =
			JpaAnnotations.EMBEDDABLE.createUsage(modelsContext);
		embeddableClass.addAnnotationUsage(embeddableAnnotation);
	}

	private static void addFields(
			DynamicClassDetails embeddableClass,
			EmbeddableMetadata embeddableMetadata,
			ModelsContext modelsContext) {
		for (ColumnMetadata columnMetadata : embeddableMetadata.getColumns()) {
			BasicFieldBuilder.addBasicField(embeddableClass, columnMetadata, modelsContext);
		}
	}

	private static void registerClassDetails(
			ClassDetails classDetails,
			ModelsContext modelsContext) {
		MutableClassDetailsRegistry registry =
			(MutableClassDetailsRegistry) modelsContext.getClassDetailsRegistry();
		registry.addClassDetails(classDetails);
	}
}
