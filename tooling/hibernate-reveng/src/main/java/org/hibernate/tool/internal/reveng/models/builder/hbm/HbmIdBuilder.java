/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.GenerationType;

/**
 * Builds {@code @Id}, {@code @GeneratedValue}, and {@code @EmbeddedId}
 * fields from hbm.xml {@code <id>} and {@code <composite-id>} elements.
 *
 * @author Koen Aers
 */
public class HbmIdBuilder {

	public static void processId(DynamicClassDetails entityClass,
								  JaxbHbmSimpleIdType id,
								  HbmBuildContext ctx) {
		if (id == null) {
			return;
		}
		String name = id.getName();
		String typeName = resolveIdType(id);
		String javaType = ctx.resolveJavaType(typeName);

		DynamicFieldDetails field = ctx.createField(entityClass, name, javaType);

		// @Id
		IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(idAnnotation);

		// @GeneratedValue
		JaxbHbmGeneratorSpecificationType generator = id.getGenerator();
		if (generator != null) {
			String genClass = generator.getClazz();
			GenerationType genType = ctx.mapGeneratorClass(genClass);
			if (genType != null) {
				GeneratedValueJpaAnnotation genAnnotation =
						JpaAnnotations.GENERATED_VALUE.createUsage(ctx.getModelsContext());
				genAnnotation.strategy(genType);
				field.addAnnotationUsage(genAnnotation);
			}
		}

		// @Column
		ctx.addColumnAnnotation(field, id.getColumn(), id.getColumnAttribute(), name);
	}

	public static void processCompositeId(DynamicClassDetails entityClass,
										   JaxbHbmCompositeIdType compositeId,
										   String defaultPackage,
										   HbmBuildContext ctx) {
		if (compositeId == null) {
			return;
		}
		for (Object keyProp : compositeId.getKeyPropertyOrKeyManyToOne()) {
			if (keyProp instanceof JaxbHbmCompositeKeyBasicAttributeType keyAttr) {
				String name = keyAttr.getName();
				String typeName = keyAttr.getTypeAttribute();
				String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");

				DynamicFieldDetails field = ctx.createField(entityClass, name, javaType);
				IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(ctx.getModelsContext());
				field.addAnnotationUsage(idAnnotation);
				ctx.addColumnAnnotation(field, keyAttr.getColumn(), keyAttr.getColumnAttribute(), name);
			} else if (keyProp instanceof JaxbHbmCompositeKeyManyToOneType keyM2o) {
				String name = keyM2o.getName();
				String targetClassName = keyM2o.getClazz();
				String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
				ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
						HbmBuildContext.simpleName(fullTargetName), fullTargetName);

				TypeDetails fieldType = new ClassTypeDetailsImpl(
						targetClass, TypeDetails.Kind.CLASS);
				DynamicFieldDetails field = entityClass.applyAttribute(
						name, fieldType, false, false, ctx.getModelsContext());

				IdJpaAnnotation idAnnotation = JpaAnnotations.ID.createUsage(ctx.getModelsContext());
				field.addAnnotationUsage(idAnnotation);
				ManyToOneJpaAnnotation m2oAnnotation =
						JpaAnnotations.MANY_TO_ONE.createUsage(ctx.getModelsContext());
				field.addAnnotationUsage(m2oAnnotation);
			}
		}
	}

	private static String resolveIdType(JaxbHbmSimpleIdType id) {
		String typeAttr = id.getTypeAttribute();
		if (typeAttr != null && !typeAttr.isEmpty()) {
			return typeAttr;
		}
		if (id.getType() != null && id.getType().getName() != null) {
			return id.getType().getName();
		}
		return "long";
	}
}
