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

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.VersionJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;

/**
 * Builds fields from hbm.xml {@code <property>}, {@code <version>},
 * and {@code <timestamp>} elements with appropriate JPA annotations.
 *
 * @author Koen Aers
 */
public class HbmPropertyBuilder {

	public static void processProperty(DynamicClassDetails entityClass,
										JaxbHbmBasicAttributeType basicAttr,
										HbmBuildContext ctx) {
		String name = basicAttr.getName();
		String typeName = basicAttr.getTypeAttribute();
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
}
