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

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AnyAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.DiscriminatorType;

/**
 * Builds {@code @ManyToOne}, {@code @OneToOne}, and {@code @Any} fields from
 * hbm.xml {@code <many-to-one>}, {@code <one-to-one>}, and {@code <any>} elements.
 *
 * @author Koen Aers
 */
public class HbmAssociationBuilder {

	public static void processManyToOne(DynamicClassDetails entityClass,
										 JaxbHbmManyToOneType m2o,
										 String defaultPackage,
										 HbmBuildContext ctx) {
		String name = m2o.getName();
		String targetClassName = m2o.getClazz();
		if (targetClassName == null || targetClassName.isEmpty()) {
			targetClassName = m2o.getEntityName();
		}
		if (targetClassName == null || targetClassName.isEmpty()) {
			// No class or entity-name specified — use the field name capitalized
			targetClassName = name.substring(0, 1).toUpperCase() + name.substring(1);
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		TypeDetails fieldType = new ClassTypeDetailsImpl(
				targetClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				name, fieldType, false, false, ctx.getModelsContext());

		ManyToOneJpaAnnotation m2oAnnotation =
				JpaAnnotations.MANY_TO_ONE.createUsage(ctx.getModelsContext());
		boolean notNull = m2o.isNotNull() != null && m2o.isNotNull();
		if (notNull) {
			m2oAnnotation.optional(false);
		}
		field.addAnnotationUsage(m2oAnnotation);

		boolean insertable = m2o.isInsert();
		boolean updatable = m2o.isUpdate();

		String columnName = m2o.getColumnAttribute();
		if (columnName != null && !columnName.isEmpty()) {
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			jc.name(columnName);
			if (!insertable) jc.insertable(false);
			if (!updatable) jc.updatable(false);
			if (notNull) jc.nullable(false);
			field.addAnnotationUsage(jc);
		} else {
			// Check nested <column> elements from columnOrFormula
			List<JaxbHbmColumnType> columns = new java.util.ArrayList<>();
			for (Object item : m2o.getColumnOrFormula()) {
				if (item instanceof JaxbHbmColumnType col) {
					columns.add(col);
				}
			}
			if (!columns.isEmpty()) {
				if (columns.size() == 1) {
					JoinColumnJpaAnnotation jc =
							JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
					jc.name(columns.get(0).getName());
					if (!insertable) jc.insertable(false);
					if (!updatable) jc.updatable(false);
					if (notNull) jc.nullable(false);
					field.addAnnotationUsage(jc);
				} else {
					jakarta.persistence.JoinColumn[] jcArray =
							new jakarta.persistence.JoinColumn[columns.size()];
					for (int i = 0; i < columns.size(); i++) {
						JoinColumnJpaAnnotation jc =
								JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
						jc.name(columns.get(i).getName());
						if (!insertable) jc.insertable(false);
						if (!updatable) jc.updatable(false);
						if (notNull) jc.nullable(false);
						jcArray[i] = jc;
					}
					JoinColumnsJpaAnnotation jcs =
							JpaAnnotations.JOIN_COLUMNS.createUsage(ctx.getModelsContext());
					jcs.value(jcArray);
					field.addAnnotationUsage(jcs);
				}
			}
		}
	}

	public static void processOneToOne(DynamicClassDetails entityClass,
										JaxbHbmOneToOneType o2o,
										String defaultPackage,
										HbmBuildContext ctx) {
		String name = o2o.getName();
		String targetClassName = o2o.getClazz();
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		TypeDetails fieldType = new ClassTypeDetailsImpl(
				targetClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				name, fieldType, false, false, ctx.getModelsContext());

		OneToOneJpaAnnotation o2oAnnotation =
				JpaAnnotations.ONE_TO_ONE.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2oAnnotation);
	}

	public static void processAny(DynamicClassDetails entityClass,
								   JaxbHbmAnyAssociationType any,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		String name = any.getName();

		// Create field typed as Object (any-typed associations are polymorphic)
		ClassDetails objectClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails("java.lang.Object");
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				objectClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				name, fieldType, false, false, ctx.getModelsContext());

		// @Any
		AnyAnnotation anyAnnotation =
				HibernateAnnotations.ANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(anyAnnotation);

		// @AnyDiscriminator — map meta-type to DiscriminatorType
		String metaType = any.getMetaType();
		if (metaType != null && !metaType.isEmpty()) {
			AnyDiscriminatorAnnotation discAnnotation =
					HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.getModelsContext());
			discAnnotation.value(mapAnyDiscriminatorType(metaType));
			field.addAnnotationUsage(discAnnotation);
		}

		// @AnyDiscriminatorValues from <meta-value> entries
		List<JaxbHbmAnyValueMappingType> metaValues = any.getMetaValue();
		if (metaValues != null && !metaValues.isEmpty()) {
			AnyDiscriminatorValueAnnotation[] values =
					new AnyDiscriminatorValueAnnotation[metaValues.size()];
			for (int i = 0; i < metaValues.size(); i++) {
				JaxbHbmAnyValueMappingType mv = metaValues.get(i);
				AnyDiscriminatorValueAnnotation dvAnnotation =
						HibernateAnnotations.ANY_DISCRIMINATOR_VALUE.createUsage(ctx.getModelsContext());
				dvAnnotation.discriminator(mv.getValue());
				String entityClassName = HbmBuildContext.resolveClassName(mv.getClazz(), defaultPackage);
				try {
					dvAnnotation.entity(Class.forName(entityClassName));
				} catch (ClassNotFoundException e) {
					// Entity class may not be on classpath during reverse engineering;
					// store as Object.class — the discriminator string is the key info
					dvAnnotation.entity(Object.class);
				}
				values[i] = dvAnnotation;
			}
			AnyDiscriminatorValuesAnnotation containerAnnotation =
					HibernateAnnotations.ANY_DISCRIMINATOR_VALUES.createUsage(ctx.getModelsContext());
			containerAnnotation.value(values);
			field.addAnnotationUsage(containerAnnotation);
		}

		// @AnyKeyJavaClass from id-type
		String idType = any.getIdType();
		if (idType != null && !idType.isEmpty()) {
			String javaType = ctx.resolveJavaType(idType);
			try {
				Class<?> keyClass = Class.forName(javaType);
				AnyKeyJavaClassAnnotation keyAnnotation =
						HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx.getModelsContext());
				keyAnnotation.value(keyClass);
				field.addAnnotationUsage(keyAnnotation);
			} catch (ClassNotFoundException e) {
				// Key type not on classpath — skip annotation
			}
		}

		// @JoinColumn from columns (usually the second column is the FK, first is discriminator)
		List<JaxbHbmColumnType> columns = any.getColumn();
		if (columns != null && columns.size() >= 2) {
			JoinColumnJpaAnnotation joinColAnnotation =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			joinColAnnotation.name(columns.get(1).getName());
			field.addAnnotationUsage(joinColAnnotation);
		}
	}

	private static DiscriminatorType mapAnyDiscriminatorType(String metaType) {
		if (metaType == null) {
			return DiscriminatorType.STRING;
		}
		return switch (metaType.toLowerCase()) {
			case "integer", "int", "long", "short" -> DiscriminatorType.INTEGER;
			case "character", "char" -> DiscriminatorType.CHAR;
			default -> DiscriminatorType.STRING;
		};
	}
}
