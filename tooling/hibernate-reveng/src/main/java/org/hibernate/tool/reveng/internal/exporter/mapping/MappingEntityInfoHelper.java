/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.mapping;

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
public class MappingEntityInfoHelper {

	private final ClassDetails classDetails;

	MappingEntityInfoHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	// --- Entity / class ---

	public boolean isEmbeddable() {
		return classDetails.hasDirectAnnotationUsage(Embeddable.class);
	}

	public String getClassName() {
		return classDetails.getClassName();
	}

	public String getPackageName() {
		String name = getClassName();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : null;
	}

	// --- Entity-level Hibernate extensions ---

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

	public String getCacheAccessType() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name();
	}

	public String getCacheRegion() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	public boolean isCacheIncludeLazy() {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		return cache == null || cache.includeLazy();
	}

	public String getSqlRestriction() {
		SQLRestriction sr = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		return sr != null ? sr.value() : null;
	}

	public String getOptimisticLockMode() {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol == null || ol.type() == OptimisticLockType.VERSION) {
			return null;
		}
		return ol.type().name();
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
		return classDetails.hasDirectAnnotationUsage(ConcreteProxy.class);
	}

	public String getClassAccessType() {
		Access access = classDetails.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return null;
		}
		return access.value().name();
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

	// --- Inheritance ---

	public boolean hasInheritance() {
		return classDetails.hasDirectAnnotationUsage(Inheritance.class);
	}

	public String getInheritanceStrategy() {
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		return inh != null ? inh.strategy().name() : null;
	}

	public String getDiscriminatorColumnName() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.name() : null;
	}

	public String getDiscriminatorType() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.discriminatorType().name() : null;
	}

	public int getDiscriminatorColumnLength() {
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		return dc != null ? dc.length() : 0;
	}

	public String getDiscriminatorValue() {
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		return dv != null ? dv.value() : null;
	}

	public String getPrimaryKeyJoinColumnName() {
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		return pkjc != null ? pkjc.name() : null;
	}

	public List<String> getPrimaryKeyJoinColumnNames() {
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

	public List<MappingXmlHelper.SecondaryTableInfo> getSecondaryTables() {
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
