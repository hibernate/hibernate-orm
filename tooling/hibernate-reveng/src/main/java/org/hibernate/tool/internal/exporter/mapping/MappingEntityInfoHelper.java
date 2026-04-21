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
package org.hibernate.tool.internal.exporter.mapping;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Inheritance;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
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
 * Handles entity/class-level information for mapping XML template generation:
 * class name, table, inheritance, cache, optimistic locking, proxy,
 * secondary tables, and other behavioral attributes.
 *
 * @author Koen Aers
 */
class MappingEntityInfoHelper {

	private final ClassDetails classDetails;

	MappingEntityInfoHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	// --- Entity / class ---

	boolean isEmbeddable() {
		return classDetails.hasDirectAnnotationUsage(Embeddable.class);
	}

	String getClassName() {
		return classDetails.getClassName();
	}

	String getPackageName() {
		String name = getClassName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : null;
	}

	// --- Entity-level Hibernate extensions ---

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

	String getCacheAccessType() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name();
	}

	String getCacheRegion() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	boolean isCacheIncludeLazy() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache == null || cache.includeLazy();
	}

	String getSqlRestriction() {
		SQLRestriction sr = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		return sr != null ? sr.value() : null;
	}

	String getOptimisticLockMode() {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol == null || ol.type() == OptimisticLockType.VERSION) {
			return null;
		}
		return ol.type().name();
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
		return classDetails.hasDirectAnnotationUsage(ConcreteProxy.class);
	}

	String getClassAccessType() {
		Access access = classDetails.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return null;
		}
		return access.value().name();
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

	// --- Inheritance ---

	boolean hasInheritance() {
		return classDetails.hasDirectAnnotationUsage(Inheritance.class);
	}

	String getInheritanceStrategy() {
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		return inh != null ? inh.strategy().name() : null;
	}

	String getDiscriminatorColumnName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.name() : null;
	}

	String getDiscriminatorType() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.discriminatorType().name() : null;
	}

	int getDiscriminatorColumnLength() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.length() : 0;
	}

	String getDiscriminatorValue() {
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		return dv != null ? dv.value() : null;
	}

	String getPrimaryKeyJoinColumnName() {
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		return pkjc != null ? pkjc.name() : null;
	}

	List<String> getPrimaryKeyJoinColumnNames() {
		List<String> result = new ArrayList<>();
		PrimaryKeyJoinColumn single = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		if (single != null) {
			result.add(single.name());
		}
		PrimaryKeyJoinColumns container = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumns.class);
		if (container != null) {
			for (PrimaryKeyJoinColumn pkjc : container.value()) {
				result.add(pkjc.name());
			}
		}
		return result;
	}

	// --- Secondary tables ---

	List<MappingXmlHelper.SecondaryTableInfo> getSecondaryTables() {
		List<MappingXmlHelper.SecondaryTableInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(SecondaryTable.class);
		if (single != null) {
			result.add(toSecondaryTableInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(SecondaryTables.class);
		if (container != null) {
			for (SecondaryTable st : container.value()) {
				result.add(toSecondaryTableInfo(st));
			}
		}
		return result;
	}

	private MappingXmlHelper.SecondaryTableInfo toSecondaryTableInfo(SecondaryTable st) {
		List<String> keyColumns = new ArrayList<>();
		if (st.pkJoinColumns() != null) {
			for (PrimaryKeyJoinColumn pkjc : st.pkJoinColumns()) {
				keyColumns.add(pkjc.name());
			}
		}
		return new MappingXmlHelper.SecondaryTableInfo(st.name(), keyColumns);
	}
}
