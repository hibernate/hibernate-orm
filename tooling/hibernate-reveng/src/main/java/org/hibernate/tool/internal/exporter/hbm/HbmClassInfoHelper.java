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
package org.hibernate.tool.internal.exporter.hbm;

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
class HbmClassInfoHelper {

	private final ClassDetails classDetails;
	private final String comment;
	private final Map<String, List<String>> metaAttributes;

	HbmClassInfoHelper(ClassDetails classDetails, String comment,
						Map<String, List<String>> metaAttributes) {
		this.classDetails = classDetails;
		this.comment = comment;
		this.metaAttributes = metaAttributes;
	}

	private List<String> getClassMetaAttribute(String name) {
		List<String> values = metaAttributes.get(name);
		return (values != null && !values.isEmpty()) ? values : null;
	}

	// --- Entity / class ---

	String getClassName() {
		List<String> realClass = getClassMetaAttribute("hibernate.class-name");
		String name = (realClass != null && !realClass.isEmpty())
				? realClass.get(0) : classDetails.getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	String getPackageName() {
		String name = getClassName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : null;
	}

	// --- Table ---

	String getTableName() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null ? table.name() : null;
	}

	String getSchema() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.schema() != null && !table.schema().isEmpty()
				? table.schema() : null;
	}

	String getCatalog() {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		return table != null && table.catalog() != null && !table.catalog().isEmpty()
				? table.catalog() : null;
	}

	String getComment() {
		return comment;
	}

	// --- Class-level attributes ---

	boolean isMutable() {
		return !classDetails.hasDirectAnnotationUsage(Immutable.class);
	}

	boolean isDynamicUpdate() {
		return classDetails.hasDirectAnnotationUsage(DynamicUpdate.class);
	}

	boolean isDynamicInsert() {
		return classDetails.hasDirectAnnotationUsage(DynamicInsert.class);
	}

	int getBatchSize() {
		BatchSize bs = classDetails.getDirectAnnotationUsage(BatchSize.class);
		return bs != null ? bs.size() : 0;
	}

	String getCacheUsage() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name().toLowerCase().replace('_', '-');
	}

	String getCacheRegion() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	String getCacheInclude() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && !cache.includeLazy() ? "non-lazy" : null;
	}

	String getWhere() {
		SQLRestriction sr = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		return sr != null ? sr.value() : null;
	}

	boolean isAbstract() {
		return classDetails.isAbstract();
	}

	String getOptimisticLockMode() {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol == null || ol.type() == OptimisticLockType.VERSION) {
			return null;
		}
		return ol.type().name().toLowerCase();
	}

	String getRowId() {
		RowId rid = classDetails.getDirectAnnotationUsage(RowId.class);
		return rid != null && rid.value() != null && !rid.value().isEmpty()
				? rid.value() : null;
	}

	String getSubselect() {
		Subselect ss = classDetails.getDirectAnnotationUsage(Subselect.class);
		return ss != null ? ss.value() : null;
	}

	boolean isConcreteProxy() {
		return classDetails.hasDirectAnnotationUsage(ConcreteProxy.class)
				&& getProxy() == null;
	}

	String getProxy() {
		List<String> proxyValues = metaAttributes.get("hibernate.proxy");
		return proxyValues != null && !proxyValues.isEmpty() ? proxyValues.get(0) : null;
	}

	String getEntityName() {
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

	boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null
				&& !"java.lang.Object".equals(superClass.getClassName());
	}

	String getParentClassName() {
		if (!isSubclass()) {
			return null;
		}
		String name = classDetails.getSuperClass().getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
	}

	String getClassTag() {
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

	boolean needsDiscriminator() {
		return classDetails.hasDirectAnnotationUsage(DiscriminatorColumn.class);
	}

	String getDiscriminatorColumnName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.name() : null;
	}

	String getDiscriminatorTypeName() {
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

	int getDiscriminatorColumnLength() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc == null) {
			return 0;
		}
		return dc.length() != 31 ? dc.length() : 0;
	}

	String getDiscriminatorValue() {
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		return dv != null ? dv.value() : null;
	}

	String getPrimaryKeyJoinColumnName() {
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		return pkjc != null ? pkjc.name() : null;
	}
}
