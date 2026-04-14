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
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
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
			// No class or entity-name specified — try reflection on the owning class
			targetClassName = resolvePropertyTypeByReflection(
					entityClass.getClassName(), name);
			if (targetClassName == null) {
				// Fallback: capitalize the field name
				targetClassName = name.substring(0, 1).toUpperCase() + name.substring(1);
			}
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

		// @Cascade
		HbmCollectionBuilder.applyCascade(field, m2o.getCascade(), ctx);

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
			// Check nested <column> and <formula> elements from columnOrFormula
			List<JaxbHbmColumnType> columns = new java.util.ArrayList<>();
			List<String> formulas = new java.util.ArrayList<>();
			for (Object item : m2o.getColumnOrFormula()) {
				if (item instanceof JaxbHbmColumnType col) {
					columns.add(col);
				} else if (item instanceof String formula) {
					formulas.add(formula);
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
			// Store formulas as field meta attributes for HBM round-trip
			for (String formula : formulas) {
				ctx.addFieldMetaAttribute(entityClass.getClassName(), name,
						"hibernate.formula", formula);
			}
		}
	}

	public static void processOneToOne(DynamicClassDetails entityClass,
										JaxbHbmOneToOneType o2o,
										String defaultPackage,
										HbmBuildContext ctx) {
		String name = o2o.getName();
		String targetClassName = o2o.getClazz();
		if (targetClassName == null || targetClassName.isEmpty()) {
			// No class specified — try reflection on the owning class
			targetClassName = resolvePropertyTypeByReflection(
					entityClass.getClassName(), name);
			if (targetClassName == null) {
				targetClassName = name.substring(0, 1).toUpperCase() + name.substring(1);
			}
		}
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

		// constrained="true" means the entity's PK is also an FK
		if (o2o.isConstrained()) {
			JoinColumnJpaAnnotation jcAnnotation =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			field.addAnnotationUsage(jcAnnotation);
		}

		// access attribute
		HbmCollectionBuilder.applyAccessAnnotation(field, o2o.getAccess(), ctx);
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
			applyAnyDiscriminatorValues(metaValues, field,
					entityClass.getClassName(), defaultPackage, ctx);
		}

		// @AnyKeyJavaClass from id-type
		String idType = any.getIdType();
		if (idType != null && !idType.isEmpty()) {
			String javaType = ctx.resolveJavaType(idType);
			Class<?> keyClass = resolvePrimitiveOrClass(javaType);
			if (keyClass != null) {
				AnyKeyJavaClassAnnotation keyAnnotation =
						HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx.getModelsContext());
				keyAnnotation.value(keyClass);
				field.addAnnotationUsage(keyAnnotation);
			}
		}

		// Columns: first is discriminator (@Column), second is FK (@JoinColumn)
		List<JaxbHbmColumnType> columns = any.getColumn();
		if (columns != null && !columns.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(columns.get(0).getName());
			field.addAnnotationUsage(colAnnotation);
			if (columns.size() >= 2) {
				JoinColumnJpaAnnotation joinColAnnotation =
						JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
				joinColAnnotation.name(columns.get(1).getName());
				field.addAnnotationUsage(joinColAnnotation);
			}
		}

		// @Cascade
		HbmCollectionBuilder.applyCascade(field, any.getCascade(), ctx);

		// @Access
		HbmCollectionBuilder.applyAccessAnnotation(field, any.getAccess(), ctx);
	}

	/**
	 * Adds {@code @AnyDiscriminatorValues} annotations and stores entity class names
	 * as field meta attributes for template rendering.
	 */
	static void applyAnyDiscriminatorValues(List<JaxbHbmAnyValueMappingType> metaValues,
											 DynamicFieldDetails field,
											 String ownerClassName,
											 String defaultPackage,
											 HbmBuildContext ctx) {
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
				dvAnnotation.entity(Object.class);
			}
			values[i] = dvAnnotation;
			// Store entity class name as meta attribute for template rendering
			ctx.addFieldMetaAttribute(ownerClassName, field.getName(),
					"hibernate.any.meta-value:" + mv.getValue(), entityClassName);
		}
		AnyDiscriminatorValuesAnnotation containerAnnotation =
				HibernateAnnotations.ANY_DISCRIMINATOR_VALUES.createUsage(ctx.getModelsContext());
		containerAnnotation.value(values);
		field.addAnnotationUsage(containerAnnotation);
	}

	static Class<?> resolvePrimitiveOrClass(String javaType) {
		return switch (javaType) {
			case "boolean" -> boolean.class;
			case "byte" -> byte.class;
			case "char" -> char.class;
			case "short" -> short.class;
			case "int" -> int.class;
			case "long" -> long.class;
			case "float" -> float.class;
			case "double" -> double.class;
			default -> {
				try {
					yield Class.forName(javaType);
				} catch (ClassNotFoundException e) {
					yield null;
				}
			}
		};
	}

	/**
	 * Try to resolve a many-to-one / one-to-one target type by reflection
	 * on the owning class's getter or field.  Returns the fully-qualified
	 * class name, or {@code null} if the class cannot be loaded or the
	 * property cannot be found.
	 */
	private static String resolvePropertyTypeByReflection(
			String ownerClassName, String propertyName) {
		try {
			Class<?> ownerClass = Class.forName(ownerClassName);
			// Try getter first
			String getterName = "get"
					+ propertyName.substring(0, 1).toUpperCase()
					+ propertyName.substring(1);
			try {
				java.lang.reflect.Method getter = ownerClass.getMethod(getterName);
				return getter.getReturnType().getName();
			} catch (NoSuchMethodException ignored) {}
			// Try "is" getter for booleans
			String isName = "is"
					+ propertyName.substring(0, 1).toUpperCase()
					+ propertyName.substring(1);
			try {
				java.lang.reflect.Method getter = ownerClass.getMethod(isName);
				return getter.getReturnType().getName();
			} catch (NoSuchMethodException ignored) {}
			// Try field directly
			try {
				java.lang.reflect.Field f = ownerClass.getField(propertyName);
				return f.getType().getName();
			} catch (NoSuchFieldException ignored) {}
			// Try declared field
			try {
				java.lang.reflect.Field f = ownerClass.getDeclaredField(propertyName);
				return f.getType().getName();
			} catch (NoSuchFieldException ignored) {}
		} catch (ClassNotFoundException ignored) {}
		return null;
	}

	static DiscriminatorType mapAnyDiscriminatorType(String metaType) {
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
