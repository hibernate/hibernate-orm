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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Provides entity navigation and property helpers for the
 * documentation FreeMarker templates. Replaces the old
 * {@code DocHelper} by working with {@link ClassDetails} via
 * {@link EntityDocInfo} adapters.
 * <p>
 * Templates access this as {@code dochelper} in the FreeMarker context.
 *
 * @author Koen Aers
 */
public class EntityDocHelper {

	public static final String DEFAULT_NO_PACKAGE = "All Entities";

	private final Map<String, List<EntityDocInfo>> classesByPackage =
			new HashMap<>();
	private final List<EntityDocInfo> allClasses = new ArrayList<>();
	private final Map<String, EntityDocInfo> entityByQualifiedName =
			new HashMap<>();

	public EntityDocHelper(List<ClassDetails> entities) {
		for (ClassDetails entity : entities) {
			EntityDocInfo info = new EntityDocInfo(entity);
			processClass(info);
			entityByQualifiedName.put(
					info.getQualifiedDeclarationName(), info);
		}
	}

	private void processClass(EntityDocInfo info) {
		allClasses.add(info);
		String packageName = info.getPackageName();
		if (packageName == null || packageName.isEmpty()) {
			packageName = DEFAULT_NO_PACKAGE;
		}
		classesByPackage
				.computeIfAbsent(packageName, k -> new ArrayList<>())
				.add(info);
	}

	public List<String> getPackages() {
		List<String> packages = new ArrayList<>(classesByPackage.keySet());
		Collections.sort(packages);
		return packages;
	}

	public List<EntityDocInfo> getClasses() {
		List<EntityDocInfo> sorted = new ArrayList<>(allClasses);
		sorted.sort(Comparator.comparing(EntityDocInfo::getDeclarationName));
		return sorted;
	}

	public List<EntityDocInfo> getClasses(String packageName) {
		List<EntityDocInfo> list = classesByPackage.get(packageName);
		if (list == null) {
			return Collections.emptyList();
		}
		List<EntityDocInfo> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.comparing(EntityDocInfo::getDeclarationName));
		return sorted;
	}

	public List<EntityDocInfo> getInheritanceHierarchy(EntityDocInfo entity) {
		if (!entity.isSubclass()) {
			return Collections.emptyList();
		}
		List<EntityDocInfo> superClasses = new ArrayList<>();
		EntityDocInfo superClass = entity.getSuperClass();
		while (superClass != null) {
			EntityDocInfo registered = entityByQualifiedName.get(
					superClass.getQualifiedDeclarationName());
			superClasses.add(registered != null ? registered : superClass);
			superClass = superClass.getSuperClass();
		}
		return superClasses;
	}

	/**
	 * Returns an {@link EntityDocInfo} for the component type of an
	 * embedded property, or {@code null} if the property is not embedded.
	 * The component class must have been included in the entity list
	 * passed to the constructor.
	 */
	public EntityDocInfo getComponentPOJO(PropertyDocInfo property) {
		FieldDetails field = property.getFieldDetails();
		if (field.hasDirectAnnotationUsage(EmbeddedId.class)
				|| field.hasDirectAnnotationUsage(Embedded.class)) {
			String typeName =
					field.getType().determineRawClass().getClassName();
			return entityByQualifiedName.get(typeName);
		}
		return null;
	}

	public List<PropertyDocInfo> getSimpleProperties(EntityDocInfo entity) {
		List<PropertyDocInfo> result = new ArrayList<>();
		Iterator<PropertyDocInfo> it = entity.getAllPropertiesIterator();
		PropertyDocInfo id = entity.getIdentifierProperty();
		PropertyDocInfo version = entity.getVersionProperty();
		while (it.hasNext()) {
			PropertyDocInfo prop = it.next();
			if (prop != id && prop != version) {
				result.add(prop);
			}
		}
		return result;
	}

	public List<PropertyDocInfo> getOrderedSimpleProperties(
			EntityDocInfo entity) {
		List<PropertyDocInfo> result = getSimpleProperties(entity);
		result.sort(Comparator.comparing(PropertyDocInfo::getName));
		return result;
	}
}
