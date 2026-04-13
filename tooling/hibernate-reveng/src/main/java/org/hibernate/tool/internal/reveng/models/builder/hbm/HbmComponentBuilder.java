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

import java.io.Serializable;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNestedCompositeElementType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EmbeddableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedJpaAnnotation;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Builds {@code @Embeddable} classes and {@code @Embedded} fields from
 * hbm.xml {@code <component>} and {@code <dynamic-component>} elements.
 *
 * @author Koen Aers
 */
public class HbmComponentBuilder {

	public static void processComponent(DynamicClassDetails entityClass,
										 JaxbHbmCompositeAttributeType component,
										 String defaultPackage,
										 HbmBuildContext ctx) {
		String fieldName = component.getName();
		String componentClassName = component.getClazz();
		if (componentClassName == null || componentClassName.isEmpty()) {
			// No class specified — synthesize from entity + field name
			String entitySimple = HbmBuildContext.simpleName(entityClass.getClassName());
			componentClassName = entitySimple + fieldName.substring(0, 1).toUpperCase()
					+ fieldName.substring(1);
		}
		String fullClassName = HbmBuildContext.resolveClassName(componentClassName, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullClassName);

		// Create the @Embeddable class
		DynamicClassDetails embeddableClass = new DynamicClassDetails(
				simpleName, fullClassName, Object.class,
				false, null, null, ctx.getModelsContext());

		EmbeddableJpaAnnotation embeddableAnnotation =
				JpaAnnotations.EMBEDDABLE.createUsage(ctx.getModelsContext());
		embeddableClass.addAnnotationUsage(embeddableAnnotation);

		// Process nested attributes of the component
		for (Serializable attribute : component.getAttributes()) {
			if (attribute instanceof JaxbHbmBasicAttributeType basicAttr) {
				HbmPropertyBuilder.processProperty(embeddableClass, basicAttr, ctx);
			} else if (attribute instanceof JaxbHbmManyToOneType m2o) {
				HbmAssociationBuilder.processManyToOne(embeddableClass, m2o, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmCompositeAttributeType nestedComponent) {
				processComponent(embeddableClass, nestedComponent, defaultPackage, ctx);
			}
		}

		ctx.registerClassDetails(embeddableClass);
		ctx.addEmbeddableClassDetails(embeddableClass);

		// Add @Embedded field on the owning entity
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				embeddableClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = entityClass.applyAttribute(
				fieldName, fieldType, false, false, ctx.getModelsContext());

		EmbeddedJpaAnnotation embeddedAnnotation =
				JpaAnnotations.EMBEDDED.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(embeddedAnnotation);
	}

	/**
	 * Builds an {@code @Embeddable} class from a {@code <composite-element>} inside
	 * a collection. Unlike {@code processComponent}, this does NOT create an
	 * {@code @Embedded} field — the caller creates an {@code @ElementCollection} field.
	 */
	public static void buildEmbeddableFromCompositeElement(
			DynamicClassDetails embeddableClass,
			JaxbHbmCompositeCollectionElementType compositeElement,
			String defaultPackage,
			HbmBuildContext ctx) {
		if (!embeddableClass.hasDirectAnnotationUsage(jakarta.persistence.Embeddable.class)) {
			EmbeddableJpaAnnotation embeddableAnnotation =
					JpaAnnotations.EMBEDDABLE.createUsage(ctx.getModelsContext());
			embeddableClass.addAnnotationUsage(embeddableAnnotation);
		}

		// Process attributes
		for (Serializable attribute : compositeElement.getAttributes()) {
			if (attribute instanceof JaxbHbmBasicAttributeType basicAttr) {
				HbmPropertyBuilder.processProperty(embeddableClass, basicAttr, ctx);
			} else if (attribute instanceof JaxbHbmManyToOneType m2o) {
				HbmAssociationBuilder.processManyToOne(embeddableClass, m2o, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmNestedCompositeElementType nested) {
				processNestedCompositeElement(embeddableClass, nested, defaultPackage, ctx);
			}
		}

		ctx.registerClassDetails(embeddableClass);
		ctx.addEmbeddableClassDetails(embeddableClass);
	}

	private static void processNestedCompositeElement(
			DynamicClassDetails parentClass,
			JaxbHbmNestedCompositeElementType nested,
			String defaultPackage,
			HbmBuildContext ctx) {
		String fieldName = nested.getName();
		String className = nested.getClazz();
		if (className == null) {
			return;
		}
		String fullName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullName);

		DynamicClassDetails nestedClass = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, null, null, ctx.getModelsContext());

		EmbeddableJpaAnnotation embeddableAnnotation =
				JpaAnnotations.EMBEDDABLE.createUsage(ctx.getModelsContext());
		nestedClass.addAnnotationUsage(embeddableAnnotation);

		for (Serializable attribute : nested.getAttributes()) {
			if (attribute instanceof JaxbHbmBasicAttributeType basicAttr) {
				HbmPropertyBuilder.processProperty(nestedClass, basicAttr, ctx);
			} else if (attribute instanceof JaxbHbmManyToOneType m2o) {
				HbmAssociationBuilder.processManyToOne(nestedClass, m2o, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmNestedCompositeElementType deepNested) {
				processNestedCompositeElement(nestedClass, deepNested, defaultPackage, ctx);
			}
		}

		ctx.registerClassDetails(nestedClass);
		ctx.addEmbeddableClassDetails(nestedClass);

		// Add @Embedded field on the parent
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				nestedClass, TypeDetails.Kind.CLASS);
		DynamicFieldDetails field = parentClass.applyAttribute(
				fieldName, fieldType, false, false, ctx.getModelsContext());
		EmbeddedJpaAnnotation embeddedAnnotation =
				JpaAnnotations.EMBEDDED.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(embeddedAnnotation);
	}

	/**
	 * Processes a {@code <dynamic-component>} element. Dynamic components
	 * use {@code Map}-based storage and have no explicit class. We create
	 * a field typed as {@code java.util.Map} and process the nested
	 * attributes on the owning entity (since there is no separate
	 * embeddable class for dynamic components).
	 */
	public static void processDynamicComponent(DynamicClassDetails entityClass,
												JaxbHbmDynamicComponentType dynComponent,
												String defaultPackage,
												HbmBuildContext ctx) {
		String fieldName = dynComponent.getName();

		// Create a Map-typed field for the dynamic component
		ClassDetails mapClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails(Map.class.getName());
		TypeDetails fieldType = new ClassTypeDetailsImpl(
				mapClass, TypeDetails.Kind.CLASS);
		entityClass.applyAttribute(
				fieldName, fieldType, false, false, ctx.getModelsContext());

		// Process nested attributes directly on the owning entity
		// (dynamic components don't have a separate embeddable class)
		HbmSubclassBuilder.processAttributes(entityClass,
				dynComponent.getAttributes(), defaultPackage, ctx);
	}
}
