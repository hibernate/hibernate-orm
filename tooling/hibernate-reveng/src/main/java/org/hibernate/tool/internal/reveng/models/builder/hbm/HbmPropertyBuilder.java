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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NaturalIdAnnotation;
import org.hibernate.boot.models.annotations.internal.VersionJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Builds fields from hbm.xml {@code <property>}, {@code <version>},
 * {@code <timestamp>}, and {@code <natural-id>} elements with
 * appropriate JPA/Hibernate annotations.
 *
 * @author Koen Aers
 */
public class HbmPropertyBuilder {

	public static void processProperty(DynamicClassDetails entityClass,
										JaxbHbmBasicAttributeType basicAttr,
										HbmBuildContext ctx) {
		String name = basicAttr.getName();
		String typeName = basicAttr.getTypeAttribute();
		JaxbHbmTypeSpecificationType typeElement = basicAttr.getType();

		// When type is specified via nested <type> element (e.g. EnumType with
		// params), use the type attribute if available, otherwise fall back to
		// "string" — the <type> name is a Hibernate type class, not a Java type.
		if (typeName == null && typeElement != null) {
			// Don't use the Hibernate type name as Java type
			typeName = null;
		}
		String javaType = ctx.resolveJavaType(typeName != null ? typeName : "string");

		DynamicFieldDetails field = ctx.createField(entityClass, name, javaType);
		ctx.addColumnAnnotationFromBasicAttr(
				field,
				basicAttr.getColumnAttribute(),
				basicAttr.isNotNull(),
				basicAttr.getLength(),
				basicAttr.getPrecision(),
				basicAttr.getScale(),
				basicAttr.isUnique(),
				basicAttr.getColumnOrFormula(),
				name);

		// Store type parameters from <type><param> elements as field meta attributes
		if (typeElement != null) {
			String className = entityClass.getClassName();
			ctx.addFieldMetaAttribute(className, name,
					"hibernate.type.name", typeElement.getName());
			if (typeElement.getConfigParameters() != null) {
				for (JaxbHbmConfigParameterType param : typeElement.getConfigParameters()) {
					ctx.addFieldMetaAttribute(className, name,
							"hibernate.type.param:" + param.getName(),
							param.getValue() != null ? param.getValue() : "");
				}
			}
		}
	}

	public static void processVersion(DynamicClassDetails entityClass,
										JaxbHbmVersionAttributeType version,
										HbmBuildContext ctx) {
		if (version == null) {
			return;
		}
		String name = version.getName();
		String typeName = version.getType();
		String javaType = ctx.resolveJavaType(typeName != null ? typeName : "integer");

		DynamicFieldDetails field = ctx.createField(entityClass, name, javaType);
		VersionJpaAnnotation versionAnnotation =
				JpaAnnotations.VERSION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(versionAnnotation);
		ctx.addColumnAnnotation(field, version.getColumn(), version.getColumnAttribute(), name);
	}

	public static void processTimestamp(DynamicClassDetails entityClass,
										 JaxbHbmTimestampAttributeType timestamp,
										 HbmBuildContext ctx) {
		if (timestamp == null) {
			return;
		}
		String name = timestamp.getName();
		String javaType = "java.util.Date";

		DynamicFieldDetails field = ctx.createField(entityClass, name, javaType);
		VersionJpaAnnotation versionAnnotation =
				JpaAnnotations.VERSION.createUsage(ctx.getModelsContext());
		field.addAnnotationUsage(versionAnnotation);
		ctx.addColumnAnnotation(field, null, timestamp.getColumnAttribute(), name);
	}

	/**
	 * Marks fields added since {@code fieldCountBefore} with
	 * {@code @NaturalId}. Called after processing {@code <natural-id>}
	 * attributes.
	 */
	public static void markNaturalIdFields(DynamicClassDetails entityClass,
											int fieldCountBefore,
											boolean mutable,
											HbmBuildContext ctx) {
		List<FieldDetails> fields = entityClass.getFields();
		for (int i = fieldCountBefore; i < fields.size(); i++) {
			NaturalIdAnnotation natIdAnnotation =
					HibernateAnnotations.NATURAL_ID.createUsage(ctx.getModelsContext());
			natIdAnnotation.mutable(mutable);
			((DynamicFieldDetails) fields.get(i)).addAnnotationUsage(natIdAnnotation);
		}
	}
}
