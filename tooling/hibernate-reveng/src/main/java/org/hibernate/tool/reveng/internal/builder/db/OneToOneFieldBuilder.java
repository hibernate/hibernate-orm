/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.builder.db;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.reveng.internal.descriptor.OneToOneDescriptor;

import java.util.List;

/**
 * Builds a {@code @OneToOne} field on a dynamic class and attaches
 * the appropriate JPA annotations ({@code @OneToOne},
 * {@code @JoinColumn}) based on {@link OneToOneDescriptor}.
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
			OneToOneDescriptor o2oMetadata,
			ClassDetails targetClassDetails,
			ModelsContext modelsContext) {
		DynamicFieldDetails field = createField(
			entityClass, o2oMetadata, targetClassDetails, modelsContext);
		addOneToOneAnnotation(field, o2oMetadata, modelsContext);
		addJoinColumnAnnotation(field, o2oMetadata, modelsContext);
		addMapsIdAnnotation(field, o2oMetadata, modelsContext);
	}

	private static DynamicFieldDetails createField(
			DynamicClassDetails entityClass,
			OneToOneDescriptor o2oMetadata,
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
			OneToOneDescriptor o2oMetadata,
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
			OneToOneDescriptor o2oMetadata,
			ModelsContext modelsContext) {
		List<OneToOneDescriptor.JoinColumnPair> joinColumns = o2oMetadata.getJoinColumns();
		if (o2oMetadata.getMappedBy() != null || joinColumns.isEmpty()) {
			return;
		}
		if (joinColumns.size() == 1) {
			OneToOneDescriptor.JoinColumnPair jc = joinColumns.get(0);
			JoinColumnJpaAnnotation joinColumnAnnotation =
				JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
			joinColumnAnnotation.name(jc.fkColumnName());
			if (jc.referencedColumnName() != null) {
				joinColumnAnnotation.referencedColumnName(jc.referencedColumnName());
			}
			joinColumnAnnotation.unique(true);
			joinColumnAnnotation.nullable(o2oMetadata.isOptional());
			field.addAnnotationUsage(joinColumnAnnotation);
		}
		else {
			jakarta.persistence.JoinColumn[] jcArray =
				new jakarta.persistence.JoinColumn[joinColumns.size()];
			for (int i = 0; i < joinColumns.size(); i++) {
				OneToOneDescriptor.JoinColumnPair jc = joinColumns.get(i);
				JoinColumnJpaAnnotation joinColumnAnnotation =
					JpaAnnotations.JOIN_COLUMN.createUsage(modelsContext);
				joinColumnAnnotation.name(jc.fkColumnName());
				if (jc.referencedColumnName() != null) {
					joinColumnAnnotation.referencedColumnName(jc.referencedColumnName());
				}
				jcArray[i] = joinColumnAnnotation;
			}
			JoinColumnsJpaAnnotation joinColumnsAnnotation =
				JpaAnnotations.JOIN_COLUMNS.createUsage(modelsContext);
			joinColumnsAnnotation.value(jcArray);
			field.addAnnotationUsage(joinColumnsAnnotation);
		}
	}

	private static void addMapsIdAnnotation(
			MutableAnnotationTarget field,
			OneToOneDescriptor o2oMetadata,
			ModelsContext modelsContext) {
		if (o2oMetadata.isConstrained()) {
			field.addAnnotationUsage(JpaAnnotations.MAPS_ID.createUsage(modelsContext));
		}
	}
}
