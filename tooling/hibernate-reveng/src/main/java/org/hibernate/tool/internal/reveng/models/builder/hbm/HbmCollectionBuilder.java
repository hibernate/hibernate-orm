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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCustomSqlDmlType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCollectionIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToAnyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValueAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheAnnotation;
import org.hibernate.boot.models.annotations.internal.CascadeAnnotation;
import org.hibernate.boot.models.annotations.internal.CheckAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionIdAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.FilterAnnotation;
import org.hibernate.boot.models.annotations.internal.FiltersAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAllAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLInsertAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLRestrictionAnnotation;
import org.hibernate.boot.models.annotations.internal.SQLUpdateAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.annotations.internal.SortNaturalAnnotation;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

/**
 * Builds collection fields from hbm.xml {@code <set>}, {@code <list>},
 * {@code <bag>}, {@code <map>}, {@code <array>}, {@code <primitive-array>},
 * and {@code <idbag>} elements with {@code @OneToMany},
 * {@code @ManyToMany}, or {@code @ElementCollection} annotations,
 * plus collection-level metadata: {@code cascade}, {@code fetch},
 * {@code batch-size}, {@code where}, {@code cache}, {@code filter},
 * {@code sql-insert}/{@code sql-update}/{@code sql-delete}/{@code sql-delete-all},
 * {@code sort}, {@code order-by}, {@code check}, {@code mutable},
 * {@code optimistic-lock}, and {@code table}/{@code schema}/{@code catalog}.
 *
 * @author Koen Aers
 */
public class HbmCollectionBuilder {

	public static void processSet(DynamicClassDetails entityClass,
								   JaxbHbmSetType set,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		DynamicFieldDetails field = createCollectionField(entityClass, set.getName(),
				set.getOneToMany(), set.getManyToMany(), set.getElement(),
				set.getKey(), "java.util.Set", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, set.getAccess(), ctx);
		applyCommonMetadata(field, set.getCascade(), set.getFetch(),
				set.getLazy(), set.getWhere(), set.getBatchSize(),
				set.getCache(), set.getFilter(),
				set.getSqlInsert(), set.getSqlUpdate(),
				set.getSqlDelete(), set.getSqlDeleteAll(),
				set.isMutable(), set.isOptimisticLock(),
				set.getCheck(), set.getTable(), set.getSchema(),
				set.getCatalog(), ctx);
		applySortAnnotation(field, set.getSort(), ctx);
		applyOrderBy(field, set.getOrderBy(), ctx);
	}

	public static void processList(DynamicClassDetails entityClass,
									JaxbHbmListType list,
									String defaultPackage,
									HbmBuildContext ctx) {
		DynamicFieldDetails field;
		if (list.getCompositeElement() != null) {
			field = buildCompositeElementCollectionField(entityClass, list.getName(),
					list.getCompositeElement(), list.getKey(), "java.util.List",
					defaultPackage, ctx);
		} else {
			field = createCollectionField(entityClass, list.getName(),
					list.getOneToMany(), list.getManyToMany(), list.getElement(),
					list.getKey(), "java.util.List", defaultPackage, ctx);
		}
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, list.getAccess(), ctx);
		applyListIndex(field, list.getIndex(), list.getListIndex(), ctx);
		applyCommonMetadata(field, list.getCascade(), list.getFetch(),
				list.getLazy(), list.getWhere(), list.getBatchSize(),
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
		DynamicFieldDetails field = createCollectionField(entityClass, bag.getName(),
				bag.getOneToMany(), bag.getManyToMany(), bag.getElement(),
				bag.getKey(), "java.util.Collection", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, bag.getAccess(), ctx);
		applyCommonMetadata(field, bag.getCascade(), bag.getFetch(),
				bag.getLazy(), bag.getWhere(), bag.getBatchSize(),
				bag.getCache(), bag.getFilter(),
				bag.getSqlInsert(), bag.getSqlUpdate(),
				bag.getSqlDelete(), bag.getSqlDeleteAll(),
				bag.isMutable(), bag.isOptimisticLock(),
				bag.getCheck(), bag.getTable(), bag.getSchema(),
				bag.getCatalog(), ctx);
		applyOrderBy(field, bag.getOrderBy(), ctx);
	}

	public static void processMap(DynamicClassDetails entityClass,
								   JaxbHbmMapType map,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		DynamicFieldDetails field = createMapCollectionField(entityClass, map.getName(),
				map.getOneToMany(), map.getManyToMany(), map.getManyToAny(),
				map.getElement(), map.getKey(), map, defaultPackage, ctx);
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, map.getAccess(), ctx);
		applyCommonMetadata(field, map.getCascade(), map.getFetch(),
				map.getLazy(), map.getWhere(), map.getBatchSize(),
				map.getCache(), map.getFilter(),
				map.getSqlInsert(), map.getSqlUpdate(),
				map.getSqlDelete(), map.getSqlDeleteAll(),
				map.isMutable(), map.isOptimisticLock(),
				map.getCheck(), map.getTable(), map.getSchema(),
				map.getCatalog(), ctx);
		applySortAnnotation(field, map.getSort(), ctx);
		applyOrderBy(field, map.getOrderBy(), ctx);
	}

	public static void processArray(DynamicClassDetails entityClass,
									  JaxbHbmArrayType array,
									  String defaultPackage,
									  HbmBuildContext ctx) {
		DynamicFieldDetails field = createCollectionField(entityClass, array.getName(),
				array.getOneToMany(), array.getManyToMany(), array.getElement(),
				array.getKey(), "java.util.List", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, array.getAccess(), ctx);
		applyListIndex(field, array.getIndex(), array.getListIndex(), ctx);
		// Mark as array (not list) for HBM template rendering
		ctx.addFieldMetaAttribute(entityClass.getClassName(), array.getName(),
				"hibernate.collection.tag", "array");
		// Store element-class if specified
		String elementClass = array.getElementClass();
		if (elementClass != null && !elementClass.isEmpty()) {
			String fullElementClass = HbmBuildContext.resolveClassName(elementClass, defaultPackage);
			ctx.addFieldMetaAttribute(entityClass.getClassName(), array.getName(),
					"hibernate.array.element-class", fullElementClass);
		}
		applyCommonMetadata(field, array.getCascade(), array.getFetch(),
				array.getLazy(), array.getWhere(), array.getBatchSize(),
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
		DynamicFieldDetails field = buildElementCollectionField(
				entityClass, array.getName(), element, array.getKey(), "java.util.List", ctx);
		applyAccessAnnotation(field, array.getAccess(), ctx);
		applyCommonMetadata(field, array.getCascade(), array.getFetch(),
				array.getLazy(), array.getWhere(), array.getBatchSize(),
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
		DynamicFieldDetails field = createCollectionField(entityClass, idBag.getName(),
				null, idBag.getManyToMany(), idBag.getElement(),
				idBag.getKey(), "java.util.List", defaultPackage, ctx);
		if (field == null) {
			return;
		}
		applyAccessAnnotation(field, idBag.getAccess(), ctx);
		// @CollectionId from <collection-id>
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
		applyCommonMetadata(field, idBag.getCascade(), idBag.getFetch(),
				idBag.getLazy(), idBag.getWhere(), idBag.getBatchSize(),
				idBag.getCache(), idBag.getFilter(),
				idBag.getSqlInsert(), idBag.getSqlUpdate(),
				idBag.getSqlDelete(), idBag.getSqlDeleteAll(),
				idBag.isMutable(), idBag.isOptimisticLock(),
				idBag.getCheck(), idBag.getTable(), idBag.getSchema(),
				idBag.getCatalog(), ctx);
	}

	// --- Field creation ---

	private static DynamicFieldDetails createCollectionField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmOneToManyCollectionElementType oneToMany,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			JaxbHbmBasicCollectionElementType element,
			JaxbHbmKeyType key,
			String collectionInterfaceName,
			String defaultPackage,
			HbmBuildContext ctx) {
		if (oneToMany != null) {
			return buildOneToManyField(entityClass, name, oneToMany,
					key, collectionInterfaceName, defaultPackage, ctx);
		} else if (manyToMany != null) {
			return buildManyToManyField(entityClass, name, manyToMany,
					collectionInterfaceName, defaultPackage, ctx);
		} else if (element != null) {
			return buildElementCollectionField(entityClass, name, element,
					key, collectionInterfaceName, ctx);
		}
		return null;
	}

	private static DynamicFieldDetails createMapCollectionField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmOneToManyCollectionElementType oneToMany,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			JaxbHbmManyToAnyCollectionElementType manyToAny,
			JaxbHbmBasicCollectionElementType element,
			JaxbHbmKeyType key,
			JaxbHbmMapType map,
			String defaultPackage,
			HbmBuildContext ctx) {
		// Determine the map key type and column
		String keyTypeName = "java.lang.String";
		String mapKeyColumnName = null;
		if (map.getMapKey() != null) {
			if (map.getMapKey().getTypeAttribute() != null) {
				keyTypeName = ctx.resolveJavaType(map.getMapKey().getTypeAttribute());
			}
			mapKeyColumnName = map.getMapKey().getColumnAttribute();
		} else if (map.getIndex() != null && map.getIndex().getType() != null) {
			keyTypeName = ctx.resolveJavaType(map.getIndex().getType());
		}
		ClassDetails keyClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails(keyTypeName);

		DynamicFieldDetails field = null;
		if (oneToMany != null) {
			field = buildMapOneToManyField(entityClass, name, oneToMany,
					key, keyClass, defaultPackage, ctx);
		} else if (manyToMany != null) {
			field = buildMapManyToManyField(entityClass, name, manyToMany,
					keyClass, defaultPackage, ctx);
		} else if (manyToAny != null) {
			field = buildMapManyToAnyField(entityClass, name, manyToAny,
					key, keyClass, defaultPackage, ctx);
		} else if (element != null) {
			field = buildMapElementCollectionField(entityClass, name, element,
					key, keyClass, ctx);
		}

		// @MapKeyColumn from <map-key column="...">
		if (field != null && mapKeyColumnName != null && !mapKeyColumnName.isEmpty()) {
			MapKeyColumnJpaAnnotation mkcAnnotation =
					JpaAnnotations.MAP_KEY_COLUMN.createUsage(ctx.getModelsContext());
			mkcAnnotation.name(mapKeyColumnName);
			field.addAnnotationUsage(mkcAnnotation);
		}
		return field;
	}

	private static DynamicFieldDetails buildMapOneToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmOneToManyCollectionElementType oneToMany,
			JaxbHbmKeyType key,
			ClassDetails keyClass,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = oneToMany.getClazz();
		if (targetClassName == null) {
			return null;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, targetClass);
		addKeyJoinColumns(field, key, ctx);
		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2mAnnotation);
		return field;
	}

	private static DynamicFieldDetails buildMapManyToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			ClassDetails keyClass,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = manyToMany.getClazz();
		if (targetClassName == null) {
			return null;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, targetClass);
		ManyToManyJpaAnnotation m2mAnnotation =
				JpaAnnotations.MANY_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2mAnnotation);
		return field;
	}

	private static DynamicFieldDetails buildMapManyToAnyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmManyToAnyCollectionElementType manyToAny,
			JaxbHbmKeyType key,
			ClassDetails keyClass,
			String defaultPackage,
			HbmBuildContext ctx) {
		// ManyToAny targets Object (polymorphic)
		ClassDetails objectClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails("java.lang.Object");
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, objectClass);

		// @ManyToAny
		ManyToAnyAnnotation m2aAnnotation =
				HibernateAnnotations.MANY_TO_ANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2aAnnotation);

		// @AnyDiscriminator from meta-type
		String metaType = manyToAny.getMetaType();
		if (metaType != null && !metaType.isEmpty()) {
			AnyDiscriminatorAnnotation discAnnotation =
					HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.getModelsContext());
			discAnnotation.value(HbmAssociationBuilder.mapAnyDiscriminatorType(metaType));
			field.addAnnotationUsage(discAnnotation);
		}

		// @AnyDiscriminatorValues from <meta-value>
		List<JaxbHbmAnyValueMappingType> metaValues = manyToAny.getMetaValue();
		if (metaValues != null && !metaValues.isEmpty()) {
			HbmAssociationBuilder.applyAnyDiscriminatorValues(metaValues, field,
					entityClass.getClassName(), defaultPackage, ctx);
		}

		// @AnyKeyJavaClass from id-type
		String idType = manyToAny.getIdType();
		if (idType != null && !idType.isEmpty()) {
			String javaType = ctx.resolveJavaType(idType);
			Class<?> keyJavaClass = HbmAssociationBuilder.resolvePrimitiveOrClass(javaType);
			if (keyJavaClass != null) {
				AnyKeyJavaClassAnnotation keyAnnotation =
						HibernateAnnotations.ANY_KEY_JAVA_CLASS.createUsage(ctx.getModelsContext());
				keyAnnotation.value(keyJavaClass);
				field.addAnnotationUsage(keyAnnotation);
			}
		}

		// Columns: first is discriminator (@Column), second is FK (meta attribute)
		List<JaxbHbmColumnType> columns = manyToAny.getColumn();
		if (columns != null && !columns.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(columns.get(0).getName());
			field.addAnnotationUsage(colAnnotation);
			if (columns.size() >= 2) {
				ctx.addFieldMetaAttribute(entityClass.getClassName(), name,
						"hibernate.any.fk.column", columns.get(1).getName());
			}
		}

		// Key column from <key> → @JoinColumn (for getKeyColumnNames)
		addKeyJoinColumns(field, key, ctx);

		return field;
	}

	private static DynamicFieldDetails buildMapElementCollectionField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmBasicCollectionElementType element,
			JaxbHbmKeyType key,
			ClassDetails keyClass,
			HbmBuildContext ctx) {
		String typeName = element.getTypeAttribute();
		String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");
		ClassDetails elementClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails(javaType);
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, elementClass);
		ElementCollectionJpaAnnotation ecAnnotation =
				JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(ecAnnotation);

		// @CollectionTable with join columns from <key>
		addCollectionTableFromKey(field, key, ctx);

		// @Column from <element column="...">
		String elementColumn = element.getColumnAttribute();
		if (elementColumn != null && !elementColumn.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(elementColumn);
			field.addAnnotationUsage(colAnnotation);
		}

		return field;
	}

	private static DynamicFieldDetails buildOneToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmOneToManyCollectionElementType oneToMany,
			JaxbHbmKeyType key,
			String collectionInterfaceName,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = oneToMany.getClazz();
		if (targetClassName == null) {
			return null;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, targetClass, collectionInterfaceName);

		addKeyJoinColumns(field, key, ctx);

		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2mAnnotation);
		return field;
	}

	private static DynamicFieldDetails buildManyToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			String collectionInterfaceName,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = manyToMany.getClazz();
		if (targetClassName == null) {
			return null;
		}
		String fullTargetName = HbmBuildContext.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullTargetName), fullTargetName);

		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, targetClass, collectionInterfaceName);

		ManyToManyJpaAnnotation m2mAnnotation =
				JpaAnnotations.MANY_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2mAnnotation);
		return field;
	}

	private static void applyListIndex(DynamicFieldDetails field,
									JaxbHbmIndexType index,
									JaxbHbmListIndexType listIndex,
									HbmBuildContext ctx) {
		String columnName = null;
		if (listIndex != null) {
			columnName = listIndex.getColumnAttribute();
			if ((columnName == null || columnName.isEmpty())
					&& listIndex.getColumn() != null) {
				columnName = listIndex.getColumn().getName();
			}
		} else if (index != null) {
			columnName = index.getColumnAttribute();
		}
		if (columnName != null && !columnName.isEmpty()) {
			OrderColumnJpaAnnotation ocAnnotation =
					JpaAnnotations.ORDER_COLUMN.createUsage(ctx.getModelsContext());
			ocAnnotation.name(columnName);
			field.addAnnotationUsage(ocAnnotation);
		}
	}

	private static DynamicFieldDetails buildCompositeElementCollectionField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmCompositeCollectionElementType compositeElement,
			JaxbHbmKeyType key,
			String collectionInterfaceName,
			String defaultPackage,
			HbmBuildContext ctx) {
		String componentClassName = compositeElement.getClazz();
		if (componentClassName == null) {
			return null;
		}
		String fullComponentName = HbmBuildContext.resolveClassName(componentClassName, defaultPackage);

		// Create or resolve the embeddable class
		ClassDetails componentClass = ctx.resolveOrCreateClassDetails(
				HbmBuildContext.simpleName(fullComponentName), fullComponentName);

		// Process the embeddable's attributes via HbmComponentBuilder
		if (componentClass instanceof DynamicClassDetails dynamicComponent) {
			HbmComponentBuilder.buildEmbeddableFromCompositeElement(
					dynamicComponent, compositeElement, defaultPackage, ctx);
		}

		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, componentClass, collectionInterfaceName);

		ElementCollectionJpaAnnotation ecAnnotation =
				JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(ecAnnotation);

		// @CollectionTable with join columns from <key>
		addCollectionTableFromKey(field, key, ctx);

		return field;
	}

	private static void addKeyJoinColumns(DynamicFieldDetails field,
										  JaxbHbmKeyType key,
										  HbmBuildContext ctx) {
		if (key == null) {
			return;
		}
		// Single column via attribute
		if (key.getColumnAttribute() != null) {
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
			jc.name(key.getColumnAttribute());
			field.addAnnotationUsage(jc);
			return;
		}
		// Multiple columns via nested <column> elements
		List<JaxbHbmColumnType> columns = key.getColumn();
		if (columns != null && !columns.isEmpty()) {
			if (columns.size() == 1) {
				JoinColumnJpaAnnotation jc =
						JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
				jc.name(columns.get(0).getName());
				field.addAnnotationUsage(jc);
			} else {
				jakarta.persistence.JoinColumn[] jcArray =
						new jakarta.persistence.JoinColumn[columns.size()];
				for (int i = 0; i < columns.size(); i++) {
					JoinColumnJpaAnnotation jc =
							JpaAnnotations.JOIN_COLUMN.createUsage(ctx.getModelsContext());
					jc.name(columns.get(i).getName());
					jcArray[i] = jc;
				}
				JoinColumnsJpaAnnotation jcs =
						JpaAnnotations.JOIN_COLUMNS.createUsage(ctx.getModelsContext());
				jcs.value(jcArray);
				field.addAnnotationUsage(jcs);
			}
		}
	}

	private static DynamicFieldDetails buildElementCollectionField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmBasicCollectionElementType element,
			JaxbHbmKeyType key,
			String collectionInterfaceName,
			HbmBuildContext ctx) {
		String typeName = element.getTypeAttribute();
		String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");
		ClassDetails elementClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails(javaType);

		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, elementClass, collectionInterfaceName);

		ElementCollectionJpaAnnotation ecAnnotation =
				JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(ecAnnotation);

		// @CollectionTable with join columns from <key>
		addCollectionTableFromKey(field, key, ctx);

		// @Column from <element column="...">
		String elementColumn = element.getColumnAttribute();
		if (elementColumn != null && !elementColumn.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(elementColumn);
			field.addAnnotationUsage(colAnnotation);
		}

		return field;
	}

	// --- Collection metadata ---

	private static void applyCommonMetadata(DynamicFieldDetails field,
											 String cascade,
											 JaxbHbmFetchStyleWithSubselectEnum fetch,
											 JaxbHbmLazyWithExtraEnum lazy,
											 String where,
											 int batchSize,
											 JaxbHbmCacheType cache,
											 List<JaxbHbmFilterType> filters,
											 JaxbHbmCustomSqlDmlType sqlInsert,
											 JaxbHbmCustomSqlDmlType sqlUpdate,
											 JaxbHbmCustomSqlDmlType sqlDelete,
											 JaxbHbmCustomSqlDmlType sqlDeleteAll,
											 boolean mutable,
											 boolean optimisticLock,
											 String check,
											 String table,
											 String schema,
											 String catalog,
											 HbmBuildContext ctx) {
		ModelsContext mc = ctx.getModelsContext();

		// @Cascade
		applyCascade(field, cascade, ctx);

		// lazy → FetchType on @OneToMany / @ManyToMany / @ElementCollection
		if (lazy != null && lazy == JaxbHbmLazyWithExtraEnum.FALSE) {
			ManyToManyJpaAnnotation m2m = (ManyToManyJpaAnnotation)
					field.getDirectAnnotationUsage(jakarta.persistence.ManyToMany.class);
			if (m2m != null) {
				m2m.fetch(jakarta.persistence.FetchType.EAGER);
			}
			OneToManyJpaAnnotation o2m = (OneToManyJpaAnnotation)
					field.getDirectAnnotationUsage(jakarta.persistence.OneToMany.class);
			if (o2m != null) {
				o2m.fetch(jakarta.persistence.FetchType.EAGER);
			}
		}

		// @Fetch
		if (fetch != null) {
			FetchAnnotation fetchAnnotation =
					HibernateAnnotations.FETCH.createUsage(mc);
			fetchAnnotation.value(mapFetchMode(fetch));
			field.addAnnotationUsage(fetchAnnotation);
		}

		// @SQLRestriction (where)
		if (where != null && !where.isEmpty()) {
			SQLRestrictionAnnotation srAnnotation =
					HibernateAnnotations.SQL_RESTRICTION.createUsage(mc);
			srAnnotation.value(where);
			field.addAnnotationUsage(srAnnotation);
		}

		// @BatchSize
		if (batchSize > 0) {
			BatchSizeAnnotation bsAnnotation =
					HibernateAnnotations.BATCH_SIZE.createUsage(mc);
			bsAnnotation.size(batchSize);
			field.addAnnotationUsage(bsAnnotation);
		}

		// @Cache
		if (cache != null) {
			CacheAnnotation cacheAnnotation =
					HibernateAnnotations.CACHE.createUsage(mc);
			cacheAnnotation.usage(mapCacheConcurrency(cache.getUsage()));
			String region = cache.getRegion();
			if (region != null && !region.isEmpty()) {
				cacheAnnotation.region(region);
			}
			field.addAnnotationUsage(cacheAnnotation);
		}

		// @Filter / @Filters
		if (filters != null && !filters.isEmpty()) {
			if (filters.size() == 1) {
				FilterAnnotation fa = HibernateAnnotations.FILTER.createUsage(mc);
				applyFilter(fa, filters.get(0));
				field.addAnnotationUsage(fa);
			} else {
				FilterAnnotation[] filterAnnotations = new FilterAnnotation[filters.size()];
				for (int i = 0; i < filters.size(); i++) {
					FilterAnnotation fa = HibernateAnnotations.FILTER.createUsage(mc);
					applyFilter(fa, filters.get(i));
					filterAnnotations[i] = fa;
				}
				FiltersAnnotation container = HibernateAnnotations.FILTERS.createUsage(mc);
				container.value(filterAnnotations);
				field.addAnnotationUsage(container);
			}
		}

		// @SQLInsert
		if (sqlInsert != null) {
			SQLInsertAnnotation annotation = HibernateAnnotations.SQL_INSERT.createUsage(mc);
			annotation.sql(sqlInsert.getValue());
			annotation.callable(sqlInsert.isCallable());
			field.addAnnotationUsage(annotation);
		}

		// @SQLUpdate
		if (sqlUpdate != null) {
			SQLUpdateAnnotation annotation = HibernateAnnotations.SQL_UPDATE.createUsage(mc);
			annotation.sql(sqlUpdate.getValue());
			annotation.callable(sqlUpdate.isCallable());
			field.addAnnotationUsage(annotation);
		}

		// @SQLDelete
		if (sqlDelete != null) {
			SQLDeleteAnnotation annotation = HibernateAnnotations.SQL_DELETE.createUsage(mc);
			annotation.sql(sqlDelete.getValue());
			annotation.callable(sqlDelete.isCallable());
			field.addAnnotationUsage(annotation);
		}

		// @SQLDeleteAll
		if (sqlDeleteAll != null) {
			SQLDeleteAllAnnotation annotation =
					HibernateAnnotations.SQL_DELETE_ALL.createUsage(mc);
			annotation.sql(sqlDeleteAll.getValue());
			annotation.callable(sqlDeleteAll.isCallable());
			field.addAnnotationUsage(annotation);
		}

		// @Immutable (mutable=false)
		if (!mutable) {
			field.addAnnotationUsage(
					HibernateAnnotations.IMMUTABLE.createUsage(mc));
		}

		// @OptimisticLock(excluded=true) (optimistic-lock=false)
		if (!optimisticLock) {
			OptimisticLockAnnotation olAnnotation =
					HibernateAnnotations.OPTIMISTIC_LOCK.createUsage(mc);
			olAnnotation.excluded(true);
			field.addAnnotationUsage(olAnnotation);
		}

		// @Check
		if (check != null && !check.isEmpty()) {
			CheckAnnotation checkAnnotation =
					HibernateAnnotations.CHECK.createUsage(mc);
			checkAnnotation.constraints(check);
			field.addAnnotationUsage(checkAnnotation);
		}

		// @JoinTable (collection table)
		if (table != null && !table.isEmpty()) {
			JoinTableJpaAnnotation jtAnnotation =
					JpaAnnotations.JOIN_TABLE.createUsage(mc);
			jtAnnotation.name(table);
			if (schema != null && !schema.isEmpty()) {
				jtAnnotation.schema(schema);
			}
			if (catalog != null && !catalog.isEmpty()) {
				jtAnnotation.catalog(catalog);
			}
			field.addAnnotationUsage(jtAnnotation);
		}
	}

	private static void applyFilter(FilterAnnotation annotation,
									  JaxbHbmFilterType filter) {
		annotation.name(filter.getName());
		String condition = filter.getCondition();
		if (condition != null && !condition.isEmpty()) {
			annotation.condition(condition);
		}
	}

	private static void applySortAnnotation(DynamicFieldDetails field,
											 String sort,
											 HbmBuildContext ctx) {
		if (sort == null || sort.isEmpty() || "unsorted".equals(sort)) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		if ("natural".equals(sort)) {
			SortNaturalAnnotation sortAnnotation =
					HibernateAnnotations.SORT_NATURAL.createUsage(mc);
			field.addAnnotationUsage(sortAnnotation);
		} else {
			// sort value is a Comparator class name
			SortComparatorAnnotation sortAnnotation =
					HibernateAnnotations.SORT_COMPARATOR.createUsage(mc);
			try {
				sortAnnotation.value(
						(Class<? extends java.util.Comparator<?>>) Class.forName(sort));
			} catch (ClassNotFoundException e) {
				// Comparator class not on classpath — skip
				return;
			}
			field.addAnnotationUsage(sortAnnotation);
		}
	}

	private static void applyOrderBy(DynamicFieldDetails field,
									   String orderBy,
									   HbmBuildContext ctx) {
		if (orderBy == null || orderBy.isEmpty()) {
			return;
		}
		// Use Hibernate's @SQLOrder for SQL-level ordering
		var sqlOrderAnnotation =
				HibernateAnnotations.SQL_ORDER.createUsage(ctx.getModelsContext());
		sqlOrderAnnotation.value(orderBy);
		field.addAnnotationUsage(sqlOrderAnnotation);
	}

	private static void addCollectionTableFromKey(DynamicFieldDetails field,
												JaxbHbmKeyType key,
												HbmBuildContext ctx) {
		if (key == null) {
			return;
		}
		ModelsContext mc = ctx.getModelsContext();
		String columnName = key.getColumnAttribute();
		if (columnName == null || columnName.isEmpty()) {
			List<JaxbHbmColumnType> columns = key.getColumn();
			if (columns != null && !columns.isEmpty()) {
				columnName = columns.get(0).getName();
			}
		}
		if (columnName != null && !columnName.isEmpty()) {
			CollectionTableJpaAnnotation ctAnnotation =
					JpaAnnotations.COLLECTION_TABLE.createUsage(mc);
			JoinColumnJpaAnnotation jc =
					JpaAnnotations.JOIN_COLUMN.createUsage(mc);
			jc.name(columnName);
			ctAnnotation.joinColumns(new jakarta.persistence.JoinColumn[] { jc });
			field.addAnnotationUsage(ctAnnotation);
		}
	}

	static void applyAccessAnnotation(DynamicFieldDetails field,
											   String access,
											   HbmBuildContext ctx) {
		if (access == null || access.isEmpty()) {
			return;
		}
		AccessJpaAnnotation accessAnnotation =
				JpaAnnotations.ACCESS.createUsage(ctx.getModelsContext());
		if ("field".equals(access)) {
			accessAnnotation.value(jakarta.persistence.AccessType.FIELD);
		} else if ("property".equals(access)) {
			accessAnnotation.value(jakarta.persistence.AccessType.PROPERTY);
		} else {
			// Custom access strategy — store as FIELD with the actual class name available via the annotation
			accessAnnotation.value(jakarta.persistence.AccessType.FIELD);
		}
		field.addAnnotationUsage(accessAnnotation);
	}

	static void applyCascade(DynamicFieldDetails field, String cascade,
							  HbmBuildContext ctx) {
		if (cascade != null && !cascade.isEmpty() && !"none".equals(cascade)) {
			CascadeType[] cascadeTypes = parseCascade(cascade);
			if (cascadeTypes.length > 0) {
				CascadeAnnotation cascadeAnnotation =
						HibernateAnnotations.CASCADE.createUsage(ctx.getModelsContext());
				cascadeAnnotation.value(cascadeTypes);
				field.addAnnotationUsage(cascadeAnnotation);
			}
		}
	}

	// --- Mapping helpers ---

	private static CascadeType[] parseCascade(String cascade) {
		List<CascadeType> types = new ArrayList<>();
		for (String part : cascade.split(",")) {
			String trimmed = part.trim().toLowerCase();
			CascadeType ct = mapCascadeType(trimmed);
			if (ct != null) {
				types.add(ct);
				// "all-delete-orphan" means ALL + DELETE_ORPHAN
				if ("all-delete-orphan".equals(trimmed)) {
					types.add(CascadeType.DELETE_ORPHAN);
				}
			}
		}
		return types.toArray(new CascadeType[0]);
	}

	private static CascadeType mapCascadeType(String value) {
		return switch (value) {
			case "all", "all-delete-orphan" -> CascadeType.ALL;
			case "save-update" -> CascadeType.PERSIST; // closest equivalent in Hibernate 8
			case "delete", "remove" -> CascadeType.REMOVE;
			case "persist" -> CascadeType.PERSIST;
			case "merge" -> CascadeType.MERGE;
			case "refresh" -> CascadeType.REFRESH;
			case "lock" -> CascadeType.LOCK;
			case "replicate" -> CascadeType.REPLICATE;
			case "evict", "detach" -> CascadeType.DETACH;
			case "delete-orphan" -> CascadeType.DELETE_ORPHAN;
			default -> null;
		};
	}

	private static FetchMode mapFetchMode(JaxbHbmFetchStyleWithSubselectEnum fetch) {
		return switch (fetch) {
			case JOIN -> FetchMode.JOIN;
			case SELECT -> FetchMode.SELECT;
			case SUBSELECT -> FetchMode.SUBSELECT;
		};
	}

	private static CacheConcurrencyStrategy mapCacheConcurrency(AccessType usage) {
		return switch (usage) {
			case READ_ONLY -> CacheConcurrencyStrategy.READ_ONLY;
			case READ_WRITE -> CacheConcurrencyStrategy.READ_WRITE;
			case NONSTRICT_READ_WRITE -> CacheConcurrencyStrategy.NONSTRICT_READ_WRITE;
			case TRANSACTIONAL -> CacheConcurrencyStrategy.TRANSACTIONAL;
		};
	}
}
