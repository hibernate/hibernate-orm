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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EmbeddableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedIdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
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
		String name = id.getName() != null ? id.getName() : "id";
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
				String generatorName = resolveGeneratorName(generator);
				GeneratedValueJpaAnnotation genAnnotation =
						JpaAnnotations.GENERATED_VALUE.createUsage(ctx.getModelsContext());
				genAnnotation.strategy(genType);
				if (generatorName != null) {
					genAnnotation.generator(generatorName);
				}
				field.addAnnotationUsage(genAnnotation);
				addGeneratorAnnotation(field, genType, generatorName,
						generator, ctx);
			}

			// Store the original generator class name and params as meta attributes
			// so the HBM exporter can faithfully reproduce them (e.g. "foreign")
			String className = entityClass.getClassName();
			ctx.addFieldMetaAttribute(className, name,
					"hibernate.generator.class", genClass);
			if (generator.getConfigParameters() != null) {
				for (JaxbHbmConfigParameterType param : generator.getConfigParameters()) {
					ctx.addFieldMetaAttribute(className, name,
							"hibernate.generator.param:" + param.getName(),
							param.getValue() != null ? param.getValue() : "");
				}
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

		String compositeIdClassName = compositeId.getClazz();
		if (compositeIdClassName != null && !compositeIdClassName.isEmpty()) {
			// Named composite-id class: create a separate @Embeddable class
			// and add an @EmbeddedId field on the entity
			processNamedCompositeId(entityClass, compositeId, defaultPackage, ctx);
		} else {
			// Inline composite-id: put @Id on each key property directly
			processInlineCompositeId(entityClass, compositeId, defaultPackage, ctx);
		}
	}

	private static void processNamedCompositeId(DynamicClassDetails entityClass,
												 JaxbHbmCompositeIdType compositeId,
												 String defaultPackage,
												 HbmBuildContext ctx) {
		String className = compositeId.getClazz();
		String fullClassName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullClassName);

		// Create the @Embeddable class for the composite key
		DynamicClassDetails embeddableClass = new DynamicClassDetails(
				simpleName, fullClassName, Object.class,
				false, null, null, ctx.getModelsContext());

		EmbeddableJpaAnnotation embeddableAnnotation =
				JpaAnnotations.EMBEDDABLE.createUsage(ctx.getModelsContext());
		embeddableClass.addAnnotationUsage(embeddableAnnotation);

		// Process key properties on the embeddable class
		for (Object keyProp : compositeId.getKeyPropertyOrKeyManyToOne()) {
			if (keyProp instanceof JaxbHbmCompositeKeyBasicAttributeType keyAttr) {
				String name = keyAttr.getName();
				String typeName = keyAttr.getTypeAttribute();
				String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");
				DynamicFieldDetails field = ctx.createField(embeddableClass, name, javaType);
				ctx.addColumnAnnotation(field, keyAttr.getColumn(),
						keyAttr.getColumnAttribute(), name);
			} else if (keyProp instanceof JaxbHbmCompositeKeyManyToOneType keyM2o) {
				String name = keyM2o.getName();
				String targetClassName = keyM2o.getClazz();
				String fullTargetName = HbmBuildContext.resolveClassName(
						targetClassName, defaultPackage);
				ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
						HbmBuildContext.simpleName(fullTargetName), fullTargetName);
				TypeDetails fieldType = new ClassTypeDetailsImpl(
						targetClass, TypeDetails.Kind.CLASS);
				DynamicFieldDetails field = entityClass.applyAttribute(
						name, fieldType, false, false, ctx.getModelsContext());
				ManyToOneJpaAnnotation m2oAnnotation =
						JpaAnnotations.MANY_TO_ONE.createUsage(ctx.getModelsContext());
				field.addAnnotationUsage(m2oAnnotation);
			}
		}

		ctx.registerClassDetails(embeddableClass);
		ctx.addEmbeddableClassDetails(embeddableClass);

		// Add @EmbeddedId field on the owning entity
		String fieldName = compositeId.getName() != null
				? compositeId.getName() : "id";
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				embeddableClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				fieldName, fieldType, false, false, ctx.getModelsContext());
		EmbeddedIdJpaAnnotation embeddedIdAnnotation =
				JpaAnnotations.EMBEDDED_ID.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(embeddedIdAnnotation);
	}

	private static void processInlineCompositeId(DynamicClassDetails entityClass,
												  JaxbHbmCompositeIdType compositeId,
												  String defaultPackage,
												  HbmBuildContext ctx) {
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

	private static String resolveGeneratorName(
			JaxbHbmGeneratorSpecificationType generator) {
		if (generator.getConfigParameters() != null) {
			for (JaxbHbmConfigParameterType param : generator.getConfigParameters()) {
				if ("sequence_name".equals(param.getName())
						|| "table_name".equals(param.getName())) {
					return param.getValue();
				}
			}
		}
		return null;
	}

	private static void addGeneratorAnnotation(
			DynamicFieldDetails field,
			GenerationType genType,
			String generatorName,
			JaxbHbmGeneratorSpecificationType generator,
			HbmBuildContext ctx) {
		if (generatorName == null) {
			return;
		}
		if (genType == GenerationType.SEQUENCE) {
			SequenceGeneratorJpaAnnotation seqGen =
					JpaAnnotations.SEQUENCE_GENERATOR.createUsage(
							ctx.getModelsContext());
			seqGen.name(generatorName);
			seqGen.sequenceName(generatorName);
			field.addAnnotationUsage(seqGen);
		} else if (genType == GenerationType.TABLE) {
			TableGeneratorJpaAnnotation tableGen =
					JpaAnnotations.TABLE_GENERATOR.createUsage(
							ctx.getModelsContext());
			tableGen.name(generatorName);
			tableGen.table(generatorName);
			field.addAnnotationUsage(tableGen);
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
