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
package org.hibernate.tool.reveng.internal.builder.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToAnyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyDiscriminatorValuesAnnotation;
import org.hibernate.boot.models.annotations.internal.AnyKeyJavaClassAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToAnyAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;

class HbmCollectionFieldFactory {

	static DynamicFieldDetails createCollectionField(
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
					key, collectionInterfaceName, defaultPackage, ctx);
		} else if (element != null) {
			return buildElementCollectionField(entityClass, name, element,
					key, collectionInterfaceName, ctx);
		}
		return null;
	}

	static DynamicFieldDetails createMapCollectionField(
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
					key, keyClass, defaultPackage, ctx);
		} else if (manyToAny != null) {
			field = buildMapManyToAnyField(entityClass, name, manyToAny,
					key, keyClass, defaultPackage, ctx);
		} else if (element != null) {
			field = buildMapElementCollectionField(entityClass, name, element,
					key, keyClass, ctx);
		}

		if (field != null && mapKeyColumnName != null && !mapKeyColumnName.isEmpty()) {
			MapKeyColumnJpaAnnotation mkcAnnotation =
					JpaAnnotations.MAP_KEY_COLUMN.createUsage(ctx.getModelsContext());
			mkcAnnotation.name(mapKeyColumnName);
			field.addAnnotationUsage(mkcAnnotation);
		}
		return field;
	}

	static DynamicFieldDetails buildCompositeElementCollectionField(
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
		String fullComponentName = HbmTypeResolver.resolveClassName(componentClassName, defaultPackage);
		ClassDetails componentClass = ctx.resolveOrCreateClassDetails(
				HbmTypeResolver.simpleName(fullComponentName), fullComponentName);
		if (componentClass instanceof DynamicClassDetails dynamicComponent) {
			HbmComponentBuilder.buildEmbeddableFromCompositeElement(
					dynamicComponent, compositeElement, defaultPackage, ctx);
		}
		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, componentClass, collectionInterfaceName);
		ElementCollectionJpaAnnotation ecAnnotation =
				JpaAnnotations.ELEMENT_COLLECTION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(ecAnnotation);
		HbmCollectionMetadataApplier.addCollectionTableFromKey(field, key, ctx);
		return field;
	}

	// --- Non-map field builders ---

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
		String fullTargetName = HbmTypeResolver.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmTypeResolver.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, targetClass, collectionInterfaceName);
		HbmCollectionMetadataApplier.addKeyJoinColumns(field, key, ctx);
		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2mAnnotation);
		return field;
	}

	private static DynamicFieldDetails buildManyToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			JaxbHbmKeyType key,
			String collectionInterfaceName,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = manyToMany.getClazz();
		if (targetClassName == null || targetClassName.isEmpty()) {
			targetClassName = manyToMany.getEntityName();
		}
		if (targetClassName == null || targetClassName.isEmpty()) {
			return null;
		}
		String fullTargetName = HbmTypeResolver.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmTypeResolver.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createCollectionField(
				entityClass, name, targetClass, collectionInterfaceName);
		ManyToManyJpaAnnotation m2mAnnotation =
				JpaAnnotations.MANY_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2mAnnotation);
		HbmCollectionMetadataApplier.addManyToManyJoinTable(field, key, manyToMany, ctx);
		return field;
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
		HbmCollectionMetadataApplier.addCollectionTableFromKey(field, key, ctx);
		String elementColumn = element.getColumnAttribute();
		if (elementColumn != null && !elementColumn.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(elementColumn);
			field.addAnnotationUsage(colAnnotation);
		}
		return field;
	}

	// --- Map field builders ---

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
		String fullTargetName = HbmTypeResolver.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmTypeResolver.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, targetClass);
		HbmCollectionMetadataApplier.addKeyJoinColumns(field, key, ctx);
		OneToManyJpaAnnotation o2mAnnotation =
				JpaAnnotations.ONE_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(o2mAnnotation);
		return field;
	}

	private static DynamicFieldDetails buildMapManyToManyField(
			DynamicClassDetails entityClass,
			String name,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			JaxbHbmKeyType key,
			ClassDetails keyClass,
			String defaultPackage,
			HbmBuildContext ctx) {
		String targetClassName = manyToMany.getClazz();
		if (targetClassName == null || targetClassName.isEmpty()) {
			targetClassName = manyToMany.getEntityName();
		}
		if (targetClassName == null || targetClassName.isEmpty()) {
			return null;
		}
		String fullTargetName = HbmTypeResolver.resolveClassName(targetClassName, defaultPackage);
		ClassDetails targetClass = ctx.resolveOrCreateClassDetails(
				HbmTypeResolver.simpleName(fullTargetName), fullTargetName);
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, targetClass);
		ManyToManyJpaAnnotation m2mAnnotation =
				JpaAnnotations.MANY_TO_MANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2mAnnotation);
		HbmCollectionMetadataApplier.addManyToManyJoinTable(field, key, manyToMany, ctx);
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
		ClassDetails objectClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails("java.lang.Object");
		DynamicFieldDetails field = ctx.createMapField(entityClass, name, keyClass, objectClass);

		ManyToAnyAnnotation m2aAnnotation =
				HibernateAnnotations.MANY_TO_ANY.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(m2aAnnotation);

		String metaType = manyToAny.getMetaType();
		if (metaType != null && !metaType.isEmpty()) {
			AnyDiscriminatorAnnotation discAnnotation =
					HibernateAnnotations.ANY_DISCRIMINATOR.createUsage(ctx.getModelsContext());
			discAnnotation.value(HbmAssociationBuilder.mapAnyDiscriminatorType(metaType));
			field.addAnnotationUsage(discAnnotation);
		}

		List<JaxbHbmAnyValueMappingType> metaValues = manyToAny.getMetaValue();
		if (metaValues != null && !metaValues.isEmpty()) {
			HbmAssociationBuilder.applyAnyDiscriminatorValues(metaValues, field,
					entityClass.getClassName(), defaultPackage, ctx);
		}

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

		HbmCollectionMetadataApplier.addKeyJoinColumns(field, key, ctx);
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
		HbmCollectionMetadataApplier.addCollectionTableFromKey(field, key, ctx);
		String elementColumn = element.getColumnAttribute();
		if (elementColumn != null && !elementColumn.isEmpty()) {
			ColumnJpaAnnotation colAnnotation =
					JpaAnnotations.COLUMN.createUsage(ctx.getModelsContext());
			colAnnotation.name(elementColumn);
			field.addAnnotationUsage(colAnnotation);
		}
		return field;
	}
}
