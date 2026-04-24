/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.db;

import java.util.Collections;
import java.util.List;
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
import org.hibernate.tool.reveng.internal.descriptor.ManyToManyDescriptor;

/**
 * Builds a {@code @ManyToMany} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @ManyToMany},
 * {@code @JoinTable}) based on {@link ManyToManyDescriptor}.
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
			ManyToManyDescriptor m2mMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		buildManyToManyField(entityClass, m2mMetadata.getFieldName(),
			m2mMetadata, targetClassDetails, modelsContext);
	}

	public static void buildManyToManyField(
			DynamicClassDetails entityClass,
			String fieldName,
			ManyToManyDescriptor m2mMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, fieldName, targetClassDetails, modelsContext);
		addManyToManyAnnotation(field, m2mMetadata, modelsContext);
		addJoinTableAnnotation(field, m2mMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			String fieldName,
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
			fieldName,
			fieldType,
			false,
			true,
			modelsContext
		);
	}

	private static void addManyToManyAnnotation(
			MutableAnnotationTarget field,
			ManyToManyDescriptor m2mMetadata,
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
			ManyToManyDescriptor m2mMetadata,
			ModelsContext modelsContext) {
		if (m2mMetadata.getJoinTableName() != null) {
			JoinTableJpaAnnotation joinTableAnnotation =
				JpaAnnotations.JOIN_TABLE.createUsage(modelsContext);
			joinTableAnnotation.name(m2mMetadata.getJoinTableName());
			if (m2mMetadata.getJoinTableSchema() != null) {
				joinTableAnnotation.schema(m2mMetadata.getJoinTableSchema());
			}
			if (m2mMetadata.getJoinTableCatalog() != null) {
				joinTableAnnotation.catalog(m2mMetadata.getJoinTableCatalog());
			}

			List<String> joinColumnNames = m2mMetadata.getJoinColumnNames();
			if (!joinColumnNames.isEmpty()) {
				jakarta.persistence.JoinColumn[] joinColumns =
					new jakarta.persistence.JoinColumn[joinColumnNames.size()];
				for (int i = 0; i < joinColumnNames.size(); i++) {
					JoinColumnJpaAnnotation joinColumn =
						JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
					joinColumn.name(joinColumnNames.get(i));
					joinColumns[i] = joinColumn;
				}
				joinTableAnnotation.joinColumns(joinColumns);
			}

			List<String> inverseJoinColumnNames = m2mMetadata.getInverseJoinColumnNames();
			if (!inverseJoinColumnNames.isEmpty()) {
				jakarta.persistence.JoinColumn[] inverseJoinColumns =
					new jakarta.persistence.JoinColumn[inverseJoinColumnNames.size()];
				for (int i = 0; i < inverseJoinColumnNames.size(); i++) {
					JoinColumnJpaAnnotation inverseJoinColumn =
						JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
					inverseJoinColumn.name(inverseJoinColumnNames.get(i));
					inverseJoinColumns[i] = inverseJoinColumn;
				}
				joinTableAnnotation.inverseJoinColumns(inverseJoinColumns);
			}

			field.addAnnotationUsage(joinTableAnnotation);
		}
	}
}
