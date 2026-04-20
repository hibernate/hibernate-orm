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

import java.io.Serializable;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.AttributeMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;

/**
 * Dispatches hbm.xml attribute elements to the appropriate builder:
 * properties, associations, collections, and components.
 *
 * @author Koen Aers
 */
class HbmAttributeProcessor {

	static void processAttributes(DynamicClassDetails entityClass,
								  List<Serializable> attributes,
								  String defaultPackage,
								  HbmBuildContext ctx) {
		if (attributes == null) {
			return;
		}
		for (Serializable attribute : attributes) {
			processAttribute(entityClass, attribute, defaultPackage, ctx);
			extractFieldMetaAttributes(entityClass, attribute, ctx);
		}
	}

	private static void processAttribute(DynamicClassDetails entityClass,
										   Serializable attribute,
										   String defaultPackage,
										   HbmBuildContext ctx) {
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
			processPropertiesGroup(entityClass, properties, defaultPackage, ctx);
		}
	}

	private static void processPropertiesGroup(DynamicClassDetails entityClass,
												JaxbHbmPropertiesType properties,
												String defaultPackage,
												HbmBuildContext ctx) {
		int fieldCountBefore = entityClass.getFields().size();
		processAttributes(entityClass, properties.getAttributes(), defaultPackage, ctx);
		String groupName = properties.getName();
		if (groupName == null || groupName.isEmpty()) {
			return;
		}
		for (int i = fieldCountBefore; i < entityClass.getFields().size(); i++) {
			String fieldName = entityClass.getFields().get(i).getName();
			ctx.addFieldMetaAttribute(entityClass.getClassName(), fieldName,
					"hibernate.properties-group", groupName);
		}
		if (properties.isUnique()) {
			ctx.addClassMetaAttribute(entityClass.getClassName(),
					"hibernate.properties-group." + groupName + ".unique", "true");
		}
		if (!properties.isInsert()) {
			ctx.addClassMetaAttribute(entityClass.getClassName(),
					"hibernate.properties-group." + groupName + ".insert", "false");
		}
		if (!properties.isUpdate()) {
			ctx.addClassMetaAttribute(entityClass.getClassName(),
					"hibernate.properties-group." + groupName + ".update", "false");
		}
		if (!properties.isOptimisticLock()) {
			ctx.addClassMetaAttribute(entityClass.getClassName(),
					"hibernate.properties-group." + groupName + ".optimistic-lock", "false");
		}
	}

	private static void extractFieldMetaAttributes(DynamicClassDetails entityClass,
													Serializable attribute,
													HbmBuildContext ctx) {
		if (attribute instanceof AttributeMapping am
				&& attribute instanceof ToolingHintContainer thc) {
			ctx.extractFieldMetaAttributes(
					entityClass.getClassName(), am.getName(), thc);
		}
	}
}
