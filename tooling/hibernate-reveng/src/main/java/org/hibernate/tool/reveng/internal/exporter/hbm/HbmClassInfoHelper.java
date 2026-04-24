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
package org.hibernate.tool.reveng.internal.exporter.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Subselect;

import org.hibernate.models.spi.ClassDetails;

/**
 * Handles entity/class-level information for HBM template generation:
 * class name, table, inheritance, cache, optimistic locking, proxy,
 * and other behavioral attributes.
 *
 * @author Koen Aers
 */
public class HbmClassInfoHelper {

	private final ClassDetails classDetails;
	private final String comment;
	private final Map<String, List<String>> metaAttributes;
	private final Map<String, String> imports;

	HbmClassInfoHelper(ClassDetails classDetails, String comment,
						Map<String, List<String>> metaAttributes,
						Map<String, String> imports) {
		this.classDetails = classDetails;
		this.comment = comment;
		this.metaAttributes = metaAttributes;
		this.imports = imports;
	}

	private List<String> getClassMetaAttribute(String name) {
		List<String> values = metaAttributes.get(name);
		return (values != null && !values.isEmpty()) ? values : null;
	}

	// --- Entity / class ---

	public String getClassName() {
		List<String> realClass = getClassMetaAttribute("hibernate.class-name");
		String name = (realClass != null && !realClass.isEmpty())
				? realClass.get(0) : classDetails.getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	public String getPackageName() {
		String name = getClassName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : null;
	}

	// --- Table ---

	public String getTableName() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null ? table.name() : null;
	}

	public String getSchema() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.schema() != null && !table.schema().isEmpty()
				? table.schema() : null;
	}

	public String getCatalog() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.catalog() != null && !table.catalog().isEmpty()
				? table.catalog() : null;
	}

	public String getComment() {
		return comment;
	}

	// --- Class-level attributes ---

	public boolean isMutable() {
		return !classDetails.hasDirectAnnotationUsage(Immutable.class);
	}

	public boolean isDynamicUpdate() {
		return classDetails.hasDirectAnnotationUsage(DynamicUpdate.class);
	}

	public boolean isDynamicInsert() {
		return classDetails.hasDirectAnnotationUsage(DynamicInsert.class);
	}

	public int getBatchSize() {
		BatchSize bs = classDetails.getDirectAnnotationUsage(BatchSize.class);
		return bs != null ? bs.size() : 0;
	}

	public String getCacheUsage() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name().toLowerCase().replace('_', '-');
	}

	public String getCacheRegion() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	public String getCacheInclude() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && !cache.includeLazy() ? "non-lazy" : null;
	}

	public String getWhere() {
		SQLRestriction sr = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		return sr != null ? sr.value() : null;
	}

	public boolean isAbstract() {
		return classDetails.isAbstract();
	}

	public String getOptimisticLockMode() {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol == null || ol.type() == OptimisticLockType.VERSION) {
			return null;
		}
		return ol.type().name().toLowerCase();
	}

	public String getRowId() {
		RowId rid = classDetails.getDirectAnnotationUsage(RowId.class);
		return rid != null && rid.value() != null && !rid.value().isEmpty()
				? rid.value() : null;
	}

	public String getSubselect() {
		Subselect ss = classDetails.getDirectAnnotationUsage(Subselect.class);
		return ss != null ? ss.value() : null;
	}

	public boolean isConcreteProxy() {
		return classDetails.hasDirectAnnotationUsage(ConcreteProxy.class)
				&& getProxy() == null;
	}

	public String getProxy() {
		List<String> proxyValues = metaAttributes.get("hibernate.proxy");
		return proxyValues != null && !proxyValues.isEmpty() ? proxyValues.get(0) : null;
	}

	public String getEntityName() {
		jakarta.persistence.Entity entity = classDetails.getDirectAnnotationUsage(jakarta.persistence.Entity.class);
		if (entity == null || entity.name() == null || entity.name().isEmpty()) {
			return null;
		}
		String simpleName = getClassName();
		int dot = simpleName.lastIndexOf('.');
		if (dot >= 0) {
			simpleName = simpleName.substring(dot + 1);
		}
		return entity.name().equals(simpleName) ? null : entity.name();
	}

	// --- Inheritance ---

	public boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null
				&& !"java.lang.Object".equals(superClass.getClassName());
	}

	public String getParentClassName() {
		if (!isSubclass()) {
			return null;
		}
		String name = classDetails.getSuperClass().getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	public String getClassTag() {
		if (!isSubclass()) {
			return "class";
		}
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		if (inh != null && inh.strategy() == InheritanceType.TABLE_PER_CLASS) {
			return "union-subclass";
		}
		if (classDetails.hasDirectAnnotationUsage(PrimaryKeyJoinColumn.class)) {
			return "joined-subclass";
		}
		return "subclass";
	}

	public boolean needsDiscriminator() {
		return classDetails.hasDirectAnnotationUsage(DiscriminatorColumn.class);
	}

	public String getDiscriminatorColumnName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.name() : null;
	}

	public String getDiscriminatorTypeName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc == null) {
			return "string";
		}
		return switch (dc.discriminatorType()) {
			case STRING -> "string";
			case CHAR -> "character";
			case INTEGER -> "integer";
		};
	}

	public int getDiscriminatorColumnLength() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc == null) {
			return 0;
		}
		return dc.length() != 31 ? dc.length() : 0;
	}

	public String getDiscriminatorValue() {
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		return dv != null ? dv.value() : null;
	}

	public String getPrimaryKeyJoinColumnName() {
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		return pkjc != null ? pkjc.name() : null;
	}

	// --- Meta attributes ---

	public Map<String, List<String>> getMetaAttributes() {
		Map<String, List<String>> result = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : metaAttributes.entrySet()) {
			if (!entry.getKey().startsWith("hibernate.proxy")
					&& !entry.getKey().equals("hibernate.comment")
					&& !entry.getKey().equals("hibernate.class-name")
					&& !entry.getKey().startsWith("hibernate.join.comment.")
					&& !entry.getKey().startsWith("hibernate.sql-query.")
					&& !entry.getKey().startsWith("hibernate.properties-group.")) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	public List<String> getMetaAttribute(String name) {
		return metaAttributes.getOrDefault(name, Collections.emptyList());
	}

	// --- Imports ---

	public List<ImportInfo> getImports() {
		List<ImportInfo> result = new ArrayList<>();
		for (Map.Entry<String, String> entry : imports.entrySet()) {
			if (!entry.getKey().equals(entry.getValue())) {
				result.add(new ImportInfo(entry.getKey(), entry.getValue()));
			}
		}
		return result;
	}

	public record ImportInfo(String className, String rename) {}
}
