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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;

/**
 * Builds collection fields from hbm.xml {@code <set>}, {@code <list>},
 * {@code <bag>}, {@code <map>}, {@code <array>}, {@code <primitive-array>},
 * and {@code <idbag>} elements with {@code @OneToMany},
 * {@code @ManyToMany}, or {@code @ElementCollection} annotations.
 *
 * @author Koen Aers
 */
public class HbmCollectionBuilder {

	public static void processSet(DynamicClassDetails entityClass,
								   JaxbHbmSetType set,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		processCollectionElement(entityClass, set.getName(),
				set.getOneToMany(), set.getManyToMany(), set.getElement(),
				defaultPackage, ctx);
	}

	public static void processList(DynamicClassDetails entityClass,
									JaxbHbmListType list,
									String defaultPackage,
									HbmBuildContext ctx) {
		processCollectionElement(entityClass, list.getName(),
				list.getOneToMany(), list.getManyToMany(), list.getElement(),
				defaultPackage, ctx);
	}

	public static void processBag(DynamicClassDetails entityClass,
								   JaxbHbmBagCollectionType bag,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		processCollectionElement(entityClass, bag.getName(),
				bag.getOneToMany(), bag.getManyToMany(), bag.getElement(),
				defaultPackage, ctx);
	}

	public static void processMap(DynamicClassDetails entityClass,
								   JaxbHbmMapType map,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		processCollectionElement(entityClass, map.getName(),
				map.getOneToMany(), map.getManyToMany(), map.getElement(),
				defaultPackage, ctx);
	}

	public static void processArray(DynamicClassDetails entityClass,
									  JaxbHbmArrayType array,
									  String defaultPackage,
									  HbmBuildContext ctx) {
		processCollectionElement(entityClass, array.getName(),
				array.getOneToMany(), array.getManyToMany(), array.getElement(),
				defaultPackage, ctx);
	}

	public static void processPrimitiveArray(DynamicClassDetails entityClass,
											  JaxbHbmPrimitiveArrayType array,
											  String defaultPackage,
											  HbmBuildContext ctx) {
		JaxbHbmBasicCollectionElementType element = array.getElement();
		if (element != null) {
			processElementCollection(entityClass, array.getName(), element, ctx);
		}
	}

	public static void processIdBag(DynamicClassDetails entityClass,
									 JaxbHbmIdBagCollectionType idBag,
									 String defaultPackage,
									 HbmBuildContext ctx) {
		processCollectionElement(entityClass, idBag.getName(),
				null, idBag.getManyToMany(), idBag.getElement(),
				defaultPackage, ctx);
	}

	private static void processCollectionElement(DynamicClassDetails entityClass,
												  String name,
												  JaxbHbmOneToManyCollectionElementType oneToMany,
												  JaxbHbmManyToManyCollectionElementType manyToMany,
												  JaxbHbmBasicCollectionElementType element,
												  String defaultPackage,
												  HbmBuildContext ctx) {
		if (oneToMany != null) {
			processOneToManyCollection(entityClass, name, oneToMany, defaultPackage, ctx);
		} else if (manyToMany != null) {
			processManyToManyCollection(entityClass, name, manyToMany, defaultPackage, ctx);
		} else if (element != null) {
			processElementCollection(entityClass, name, element, ctx);
		}
	}

	private static void processOneToManyCollection(DynamicClassDetails entityClass,
													String name,
													JaxbHbmOneToManyCollectionElementType oneToMany,
													String defaultPackage,
													HbmBuildContext ctx) {
		String targetClassName = oneToMany.getClazz();
		if (targetClassName == null) {
			return;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		DynamicFieldDetails field = ctx.createCollectionField(entityClass, name, targetClass);

		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2mAnnotation);
	}

	private static void processManyToManyCollection(DynamicClassDetails entityClass,
													 String name,
													 JaxbHbmManyToManyCollectionElementType manyToMany,
													 String defaultPackage,
													 HbmBuildContext ctx) {
		String targetClassName = manyToMany.getClazz();
		if (targetClassName == null) {
			return;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		DynamicFieldDetails field = ctx.createCollectionField(entityClass, name, targetClass);

		ManyToManyJpaAnnotation m2mAnnotation =
				JpaAnnotations.MANY_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2mAnnotation);
	}

	private static void processElementCollection(DynamicClassDetails entityClass,
												   String name,
												   JaxbHbmBasicCollectionElementType element,
												   HbmBuildContext ctx) {
		String typeName = element.getTypeAttribute();
		String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");
		ClassDetails elementClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails(javaType);

		DynamicFieldDetails field = ctx.createCollectionField(entityClass, name, elementClass);

		ElementCollectionJpaAnnotation ecAnnotation =
				JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(ecAnnotation);
	}
}
