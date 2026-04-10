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
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.AttributeMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityDiscriminatorType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

/**
 * Processes hbm.xml inheritance elements: {@code <subclass>} (single-table),
 * {@code <joined-subclass>} (joined), and {@code <union-subclass>}
 * (table-per-class). Adds appropriate JPA inheritance annotations to
 * root entities and subclass entities.
 *
 * @author Koen Aers
 */
public class HbmSubclassBuilder {

	/**
	 * Processes all subclass declarations on a root entity.
	 * Adds {@code @Inheritance} and {@code @DiscriminatorColumn} to
	 * the root entity, then builds each subclass entity.
	 */
	public static void processSubclasses(DynamicClassDetails rootEntity,
										  JaxbHbmRootEntityType entityType,
										  String defaultPackage,
										  HbmBuildContext ctx) {
		// Single-table inheritance: <subclass>
		List<JaxbHbmDiscriminatorSubclassEntityType> subclasses = entityType.getSubclass();
		if (subclasses != null && !subclasses.isEmpty()) {
			addInheritanceAnnotation(rootEntity, InheritanceType.SINGLE_TABLE, ctx);
			addDiscriminatorColumn(rootEntity, entityType, ctx);
			addDiscriminatorValue(rootEntity, entityType.getDiscriminatorValue(), ctx);
			for (JaxbHbmDiscriminatorSubclassEntityType subclass : subclasses) {
				buildDiscriminatorSubclass(subclass, rootEntity, defaultPackage, ctx);
			}
		}

		// Joined inheritance: <joined-subclass>
		List<JaxbHbmJoinedSubclassEntityType> joinedSubclasses = entityType.getJoinedSubclass();
		if (joinedSubclasses != null && !joinedSubclasses.isEmpty()) {
			addInheritanceAnnotation(rootEntity, InheritanceType.JOINED, ctx);
			for (JaxbHbmJoinedSubclassEntityType subclass : joinedSubclasses) {
				buildJoinedSubclass(subclass, rootEntity, defaultPackage, ctx);
			}
		}

		// Table-per-class inheritance: <union-subclass>
		List<JaxbHbmUnionSubclassEntityType> unionSubclasses = entityType.getUnionSubclass();
		if (unionSubclasses != null && !unionSubclasses.isEmpty()) {
			addInheritanceAnnotation(rootEntity, InheritanceType.TABLE_PER_CLASS, ctx);
			for (JaxbHbmUnionSubclassEntityType subclass : unionSubclasses) {
				buildUnionSubclass(subclass, rootEntity, defaultPackage, ctx);
			}
		}
	}

	private static void addInheritanceAnnotation(DynamicClassDetails entityClass,
												   InheritanceType strategy,
												   HbmBuildContext ctx) {
		InheritanceJpaAnnotation inheritanceAnnotation =
				JpaAnnotations.INHERITANCE.createUsage(ctx.getModelsContext());
		inheritanceAnnotation.strategy(strategy);
		entityClass.addAnnotationUsage(inheritanceAnnotation);
	}

	private static void addDiscriminatorColumn(DynamicClassDetails rootEntity,
												JaxbHbmRootEntityType entityType,
												HbmBuildContext ctx) {
		JaxbHbmEntityDiscriminatorType discriminator = entityType.getDiscriminator();
		if (discriminator == null) {
			return;
		}
		DiscriminatorColumnJpaAnnotation discColAnnotation =
				JpaAnnotations.DISCRIMINATOR_COLUMN.createUsage(ctx.getModelsContext());

		String columnName = discriminator.getColumnAttribute();
		if (columnName != null && !columnName.isEmpty()) {
			discColAnnotation.name(columnName);
		}

		String type = discriminator.getType();
		if (type != null) {
			discColAnnotation.discriminatorType(mapDiscriminatorType(type));
		}

		Integer length = discriminator.getLength();
		if (length != null && length > 0) {
			discColAnnotation.length(length);
		}

		rootEntity.addAnnotationUsage(discColAnnotation);
	}

	private static void addDiscriminatorValue(DynamicClassDetails entityClass,
											   String discriminatorValue,
											   HbmBuildContext ctx) {
		if (discriminatorValue == null || discriminatorValue.isEmpty()) {
			return;
		}
		DiscriminatorValueJpaAnnotation discValAnnotation =
				JpaAnnotations.DISCRIMINATOR_VALUE.createUsage(ctx.getModelsContext());
		discValAnnotation.value(discriminatorValue);
		entityClass.addAnnotationUsage(discValAnnotation);
	}

	private static void buildDiscriminatorSubclass(JaxbHbmDiscriminatorSubclassEntityType subclass,
													DynamicClassDetails parentEntity,
													String defaultPackage,
													HbmBuildContext ctx) {
		String className = subclass.getName();
		String fullName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullName);

		DynamicClassDetails subclassEntity = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, parentEntity, null, ctx.getModelsContext());

		addEntityAnnotation(subclassEntity, simpleName, ctx);
		addDiscriminatorValue(subclassEntity, subclass.getDiscriminatorValue(), ctx);

		// Subclass-level meta attributes
		ctx.extractClassMetaAttributes(fullName, subclass);

		processAttributes(subclassEntity, subclass.getAttributes(), defaultPackage, ctx);

		ctx.registerClassDetails(subclassEntity);
		ctx.addSubclassEntityDetails(subclassEntity);

		// Process nested subclasses
		for (JaxbHbmDiscriminatorSubclassEntityType nested : subclass.getSubclass()) {
			buildDiscriminatorSubclass(nested, subclassEntity, defaultPackage, ctx);
		}
	}

	private static void buildJoinedSubclass(JaxbHbmJoinedSubclassEntityType subclass,
											 DynamicClassDetails parentEntity,
											 String defaultPackage,
											 HbmBuildContext ctx) {
		String className = subclass.getName();
		String fullName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullName);

		DynamicClassDetails subclassEntity = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, parentEntity, null, ctx.getModelsContext());

		addEntityAnnotation(subclassEntity, simpleName, ctx);

		// @Table for joined subclass
		String tableName = subclass.getTable();
		if (tableName != null && !tableName.isEmpty()) {
			TableJpaAnnotation tableAnnotation =
					JpaAnnotations.TABLE.createUsage(ctx.getModelsContext());
			tableAnnotation.name(tableName);
			if (subclass.getSchema() != null && !subclass.getSchema().isEmpty()) {
				tableAnnotation.schema(subclass.getSchema());
			}
			if (subclass.getCatalog() != null && !subclass.getCatalog().isEmpty()) {
				tableAnnotation.catalog(subclass.getCatalog());
			}
			subclassEntity.addAnnotationUsage(tableAnnotation);
		}

		// Subclass-level meta attributes
		ctx.extractClassMetaAttributes(fullName, subclass);

		// @PrimaryKeyJoinColumn from <key>
		if (subclass.getKey() != null) {
			String keyColumn = subclass.getKey().getColumnAttribute();
			if (keyColumn != null && !keyColumn.isEmpty()) {
				PrimaryKeyJoinColumnJpaAnnotation pkJoinColAnnotation =
						JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(ctx.getModelsContext());
				pkJoinColAnnotation.name(keyColumn);
				subclassEntity.addAnnotationUsage(pkJoinColAnnotation);
			}
		}

		processAttributes(subclassEntity, subclass.getAttributes(), defaultPackage, ctx);

		ctx.registerClassDetails(subclassEntity);
		ctx.addSubclassEntityDetails(subclassEntity);

		// Process nested joined subclasses
		for (JaxbHbmJoinedSubclassEntityType nested : subclass.getJoinedSubclass()) {
			buildJoinedSubclass(nested, subclassEntity, defaultPackage, ctx);
		}
	}

	private static void buildUnionSubclass(JaxbHbmUnionSubclassEntityType subclass,
											DynamicClassDetails parentEntity,
											String defaultPackage,
											HbmBuildContext ctx) {
		String className = subclass.getName();
		String fullName = HbmBuildContext.resolveClassName(className, defaultPackage);
		String simpleName = HbmBuildContext.simpleName(fullName);

		DynamicClassDetails subclassEntity = new DynamicClassDetails(
				simpleName, fullName, Object.class,
				false, parentEntity, null, ctx.getModelsContext());

		addEntityAnnotation(subclassEntity, simpleName, ctx);

		// Subclass-level meta attributes
		ctx.extractClassMetaAttributes(fullName, subclass);

		// @Table for union subclass
		String tableName = subclass.getTable();
		if (tableName != null && !tableName.isEmpty()) {
			TableJpaAnnotation tableAnnotation =
					JpaAnnotations.TABLE.createUsage(ctx.getModelsContext());
			tableAnnotation.name(tableName);
			if (subclass.getSchema() != null && !subclass.getSchema().isEmpty()) {
				tableAnnotation.schema(subclass.getSchema());
			}
			if (subclass.getCatalog() != null && !subclass.getCatalog().isEmpty()) {
				tableAnnotation.catalog(subclass.getCatalog());
			}
			subclassEntity.addAnnotationUsage(tableAnnotation);
		}

		processAttributes(subclassEntity, subclass.getAttributes(), defaultPackage, ctx);

		ctx.registerClassDetails(subclassEntity);
		ctx.addSubclassEntityDetails(subclassEntity);

		// Process nested union subclasses
		for (JaxbHbmUnionSubclassEntityType nested : subclass.getUnionSubclass()) {
			buildUnionSubclass(nested, subclassEntity, defaultPackage, ctx);
		}
	}

	private static void addEntityAnnotation(DynamicClassDetails entityClass,
											  String entityName,
											  HbmBuildContext ctx) {
		EntityJpaAnnotation entityAnnotation =
				JpaAnnotations.ENTITY.createUsage(ctx.getModelsContext());
		entityAnnotation.name(entityName);
		entityClass.addAnnotationUsage(entityAnnotation);
	}

	static void processAttributes(DynamicClassDetails entityClass,
								   List<Serializable> attributes,
								   String defaultPackage,
								   HbmBuildContext ctx) {
		if (attributes == null) {
			return;
		}
		for (Serializable attribute : attributes) {
			if (attribute instanceof JaxbHbmBasicAttributeType basicAttr) {
				HbmPropertyBuilder.processProperty(entityClass, basicAttr, ctx);
			} else if (attribute instanceof JaxbHbmManyToOneType m2o) {
				HbmAssociationBuilder.processManyToOne(entityClass, m2o, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmOneToOneType o2o) {
				HbmAssociationBuilder.processOneToOne(entityClass, o2o, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmAnyAssociationType any) {
				HbmAssociationBuilder.processAny(entityClass, any, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmSetType set) {
				HbmCollectionBuilder.processSet(entityClass, set, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmListType list) {
				HbmCollectionBuilder.processList(entityClass, list, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmBagCollectionType bag) {
				HbmCollectionBuilder.processBag(entityClass, bag, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmMapType map) {
				HbmCollectionBuilder.processMap(entityClass, map, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmArrayType array) {
				HbmCollectionBuilder.processArray(entityClass, array, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmPrimitiveArrayType primArray) {
				HbmCollectionBuilder.processPrimitiveArray(entityClass, primArray, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmIdBagCollectionType idBag) {
				HbmCollectionBuilder.processIdBag(entityClass, idBag, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmCompositeAttributeType component) {
				HbmComponentBuilder.processComponent(entityClass, component, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmDynamicComponentType dynComponent) {
				HbmComponentBuilder.processDynamicComponent(entityClass, dynComponent, defaultPackage, ctx);
			} else if (attribute instanceof JaxbHbmPropertiesType properties) {
				// <properties> is a grouping element — recurse into its children
				processAttributes(entityClass, properties.getAttributes(), defaultPackage, ctx);
			}
			// Extract field-level <meta> attributes
			if (attribute instanceof AttributeMapping am
					&& attribute instanceof ToolingHintContainer thc) {
				ctx.extractFieldMetaAttributes(
						entityClass.getClassName(), am.getName(), thc);
			}
		}
	}

	private static DiscriminatorType mapDiscriminatorType(String hbmType) {
		if (hbmType == null) {
			return DiscriminatorType.STRING;
		}
		return switch (hbmType.toLowerCase()) {
			case "integer", "int", "long", "short" -> DiscriminatorType.INTEGER;
			case "character", "char" -> DiscriminatorType.CHAR;
			default -> DiscriminatorType.STRING;
		};
	}
}
