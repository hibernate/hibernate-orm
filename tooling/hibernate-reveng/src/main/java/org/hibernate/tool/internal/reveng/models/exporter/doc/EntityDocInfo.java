/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Adapts a {@link ClassDetails} into a shape compatible with the
 * documentation FreeMarker templates, which expect the old
 * {@code POJOClass} API.
 * <p>
 * Templates access properties like {@code class.packageName},
 * {@code class.declarationName}, {@code class.hasIdentifierProperty()},
 * {@code class.getJavaTypeName(property, jdk5)}, etc.
 *
 * @author Koen Aers
 */
public class EntityDocInfo {

	private final ClassDetails classDetails;
	private final List<PropertyDocInfo> properties;

	public EntityDocInfo(ClassDetails classDetails) {
		this.classDetails = classDetails;
		this.properties = buildProperties(classDetails);
	}

	// --- POJOClass-compatible API for templates ---

	public String getDeclarationName() {
		return classDetails.getName();
	}

	public String getQualifiedDeclarationName() {
		return classDetails.getClassName();
	}

	public String getPackageName() {
		String className = classDetails.getClassName();
		if (className == null) {
			return "";
		}
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	public String getShortName() {
		return classDetails.getName();
	}

	public boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null
				&& !"java.lang.Object".equals(superClass.getClassName());
	}

	public EntityDocInfo getSuperClass() {
		ClassDetails superClass = classDetails.getSuperClass();
		if (superClass != null
				&& !"java.lang.Object".equals(superClass.getClassName())) {
			return new EntityDocInfo(superClass);
		}
		return null;
	}

	public boolean hasIdentifierProperty() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return true;
			}
		}
		return false;
	}

	public PropertyDocInfo getIdentifierProperty() {
		for (PropertyDocInfo prop : properties) {
			FieldDetails field = prop.getFieldDetails();
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return prop;
			}
		}
		return null;
	}

	public boolean hasVersionProperty() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(Version.class)) {
				return true;
			}
		}
		return false;
	}

	public PropertyDocInfo getVersionProperty() {
		for (PropertyDocInfo prop : properties) {
			if (prop.getFieldDetails().hasDirectAnnotationUsage(Version.class)) {
				return prop;
			}
		}
		return null;
	}

	/**
	 * Returns the Java type name for the given property.
	 * For collection types with jdk5=true, includes generic type parameters.
	 */
	public String getJavaTypeName(PropertyDocInfo property, boolean jdk5) {
		FieldDetails field = property.getFieldDetails();
		TypeDetails type = field.getType();
		String rawName = type.determineRawClass().getName();
		if (jdk5) {
			TypeDetails elementType = field.getElementType();
			if (elementType != null) {
				return rawName + "<"
						+ elementType.determineRawClass().getName() + ">";
			}
		}
		return rawName;
	}

	public boolean hasFieldJavaDoc(PropertyDocInfo property) {
		return false;
	}

	public String getFieldDescription(PropertyDocInfo property) {
		return "";
	}

	public String getMetaAsString(String key) {
		return "";
	}

	public Iterator<PropertyDocInfo> getAllPropertiesIterator() {
		return properties.iterator();
	}

	ClassDetails getClassDetails() {
		return classDetails;
	}

	private static List<PropertyDocInfo> buildProperties(
			ClassDetails classDetails) {
		List<PropertyDocInfo> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			result.add(new PropertyDocInfo(field));
		}
		return result;
	}
}
