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
package org.hibernate.tool.internal.builder.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCollectionIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.CollectionIdAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;

/**
 * Builds collection fields from hbm.xml {@code <set>}, {@code <list>},
 * {@code <bag>}, {@code <map>}, {@code <array>}, {@code <primitive-array>},
 * and {@code <idbag>} elements. Delegates field creation to
 * {@link HbmCollectionFieldFactory} and metadata application to
 * {@link HbmCollectionMetadataApplier}.
 *
 * @author Koen Aers
 */
public class HbmCollectionBuilder {

	public static void processSet(DynamicClassDetails entityClass,
								   JaxbHbmSetType set,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, set.getName(),
				set.getOneToMany(), set.getManyToMany(), set.getElement(),
				set.getKey(), "java.util.Set", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, set.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyInverse(field, set.isInverse(), entityClass, ctx);
		HbmCollectionMetadataApplier.applyCommonMetadata(field, set.getCascade(),
				set.getFetch(), set.getLazy(), set.getWhere(), set.getBatchSize(),
				set.getCache(), set.getFilter(),
				set.getSqlInsert(), set.getSqlUpdate(),
				set.getSqlDelete(), set.getSqlDeleteAll(),
				set.isMutable(), set.isOptimisticLock(),
				set.getCheck(), set.getTable(), set.getSchema(),
				set.getCatalog(), ctx);
		HbmCollectionMetadataApplier.applySortAnnotation(field, set.getSort(), ctx);
		HbmCollectionMetadataApplier.applyOrderBy(field, set.getOrderBy(), ctx);
	}

	public static void processList(DynamicClassDetails entityClass,
									JaxbHbmListType list,
									String defaultPackage,
									HbmBuildContext ctx) {
		DynamicFieldDetails field;
		if (list.getCompositeElement() != null) {
			field = HbmCollectionFieldFactory.buildCompositeElementCollectionField(
					entityClass, list.getName(),
					list.getCompositeElement(), list.getKey(), "java.util.List",
					defaultPackage, ctx);
		} else {
			field = HbmCollectionFieldFactory.createCollectionField(
					entityClass, list.getName(),
					list.getOneToMany(), list.getManyToMany(), list.getElement(),
					list.getKey(), "java.util.List", defaultPackage, ctx);
		}
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, list.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyListIndex(
				field, list.getIndex(), list.getListIndex(), ctx);
		HbmCollectionMetadataApplier.applyCommonMetadata(field, list.getCascade(),
				list.getFetch(), list.getLazy(), list.getWhere(), list.getBatchSize(),
				list.getCache(), list.getFilter(),
				list.getSqlInsert(), list.getSqlUpdate(),
				list.getSqlDelete(), list.getSqlDeleteAll(),
				list.isMutable(), list.isOptimisticLock(),
				list.getCheck(), list.getTable(), list.getSchema(),
				list.getCatalog(), ctx);
	}

	public static void processBag(DynamicClassDetails entityClass,
								   JaxbHbmBagCollectionType bag,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, bag.getName(),
				bag.getOneToMany(), bag.getManyToMany(), bag.getElement(),
				bag.getKey(), "java.util.Collection", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, bag.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyInverse(field, bag.isInverse(), entityClass, ctx);
		HbmCollectionMetadataApplier.applyCommonMetadata(field, bag.getCascade(),
				bag.getFetch(), bag.getLazy(), bag.getWhere(), bag.getBatchSize(),
				bag.getCache(), bag.getFilter(),
				bag.getSqlInsert(), bag.getSqlUpdate(),
				bag.getSqlDelete(), bag.getSqlDeleteAll(),
				bag.isMutable(), bag.isOptimisticLock(),
				bag.getCheck(), bag.getTable(), bag.getSchema(),
				bag.getCatalog(), ctx);
		HbmCollectionMetadataApplier.applyOrderBy(field, bag.getOrderBy(), ctx);
	}

	public static void processMap(DynamicClassDetails entityClass,
								   JaxbHbmMapType map,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createMapCollectionField(
				entityClass, map.getName(),
				map.getOneToMany(), map.getManyToMany(), map.getManyToAny(),
				map.getElement(), map.getKey(), map, defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, map.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyInverse(field, map.isInverse(), entityClass, ctx);
		HbmCollectionMetadataApplier.applyCommonMetadata(field, map.getCascade(),
				map.getFetch(), map.getLazy(), map.getWhere(), map.getBatchSize(),
				map.getCache(), map.getFilter(),
				map.getSqlInsert(), map.getSqlUpdate(),
				map.getSqlDelete(), map.getSqlDeleteAll(),
				map.isMutable(), map.isOptimisticLock(),
				map.getCheck(), map.getTable(), map.getSchema(),
				map.getCatalog(), ctx);
		HbmCollectionMetadataApplier.applySortAnnotation(field, map.getSort(), ctx);
		HbmCollectionMetadataApplier.applyOrderBy(field, map.getOrderBy(), ctx);
	}

	public static void processArray(DynamicClassDetails entityClass,
									  JaxbHbmArrayType array,
									  String defaultPackage,
									  HbmBuildContext ctx) {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, array.getName(),
				array.getOneToMany(), array.getManyToMany(), array.getElement(),
				array.getKey(), "java.util.List", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, array.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyListIndex(
				field, array.getIndex(), array.getListIndex(), ctx);
		ctx.addFieldMetaAttribute(entityClass.getClassName(), array.getName(),
				"hibernate.collection.tag", "array");
		String elementClass = array.getElementClass();
		if (elementClass != null && !elementClass.isEmpty()) {
			String fullElementClass = HbmTypeResolver.resolveClassName(elementClass, defaultPackage);
			ctx.addFieldMetaAttribute(entityClass.getClassName(), array.getName(),
					"hibernate.array.element-class", fullElementClass);
		}
		HbmCollectionMetadataApplier.applyCommonMetadata(field, array.getCascade(),
				array.getFetch(), array.getLazy(), array.getWhere(), array.getBatchSize(),
				array.getCache(), array.getFilter(),
				array.getSqlInsert(), array.getSqlUpdate(),
				array.getSqlDelete(), array.getSqlDeleteAll(),
				array.isMutable(), array.isOptimisticLock(),
				array.getCheck(), array.getTable(), array.getSchema(),
				array.getCatalog(), ctx);
	}

	public static void processPrimitiveArray(DynamicClassDetails entityClass,
											  JaxbHbmPrimitiveArrayType array,
											  String defaultPackage,
											  HbmBuildContext ctx) {
		JaxbHbmBasicCollectionElementType element = array.getElement();
		if (element == null) {
			return;
		}
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, array.getName(),
				null, null, element,
				array.getKey(), "java.util.List", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, array.getAccess(), ctx);
		HbmCollectionMetadataApplier.applyCommonMetadata(field, array.getCascade(),
				array.getFetch(), array.getLazy(), array.getWhere(), array.getBatchSize(),
				array.getCache(), array.getFilter(),
				array.getSqlInsert(), array.getSqlUpdate(),
				array.getSqlDelete(), array.getSqlDeleteAll(),
				array.isMutable(), array.isOptimisticLock(),
				array.getCheck(), array.getTable(), array.getSchema(),
				array.getCatalog(), ctx);
	}

	public static void processIdBag(DynamicClassDetails entityClass,
									 JaxbHbmIdBagCollectionType idBag,
									 String defaultPackage,
									 HbmBuildContext ctx) {
		DynamicFieldDetails field = HbmCollectionFieldFactory.createCollectionField(
				entityClass, idBag.getName(),
				null, idBag.getManyToMany(), idBag.getElement(),
				idBag.getKey(), "java.util.List", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		HbmCollectionMetadataApplier.applyAccessAnnotation(field, idBag.getAccess(), ctx);
		JaxbHbmCollectionIdType collId = idBag.getCollectionId();
		if (collId != null) {
			CollectionIdAnnotation cidAnnotation =
					HibernateAnnotations.COLLECTION_ID.createUsage(ctx.getModelsContext());
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			String colName = collId.getColumnAttribute();
			if (colName != null) {
				colAnnotation.name(colName);
			}
			cidAnnotation.column(colAnnotation);
			if (collId.getGenerator() != null && collId.getGenerator().getClazz() != null) {
				cidAnnotation.generator(collId.getGenerator().getClazz());
			}
			field.addAnnotationUsage(cidAnnotation);
		}
		HbmCollectionMetadataApplier.applyCommonMetadata(field, idBag.getCascade(),
				idBag.getFetch(), idBag.getLazy(), idBag.getWhere(), idBag.getBatchSize(),
				idBag.getCache(), idBag.getFilter(),
				idBag.getSqlInsert(), idBag.getSqlUpdate(),
				idBag.getSqlDelete(), idBag.getSqlDeleteAll(),
				idBag.isMutable(), idBag.isOptimisticLock(),
				idBag.getCheck(), idBag.getTable(), idBag.getSchema(),
				idBag.getCatalog(), ctx);
	}
}
