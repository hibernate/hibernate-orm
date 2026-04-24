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
package org.hibernate.tool.reveng.internal.exporter.entity;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Subselect;

import org.hibernate.models.spi.ClassDetails;

import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterDefInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedNativeQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.SecondaryTableInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.SqlResultSetMappingInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.EntityResultInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FieldResultInfo;

public class ClassAnnotationGenerator {

	private final ClassDetails classDetails;
	private final ImportContext importContext;
	private final TemplateHelper templateHelper;
	private final boolean annotated;

	ClassAnnotationGenerator(
			ClassDetails classDetails,
			ImportContext importContext,
			TemplateHelper templateHelper) {
		this.classDetails = classDetails;
		this.importContext = importContext;
		this.templateHelper = templateHelper;
		this.annotated = templateHelper.isAnnotated();
	}

	public String generate() {
		if (!annotated) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (classDetails.hasDirectAnnotationUsage(Embeddable.class)) {
			return appendEmbeddable(sb);
		}
		appendEntity(sb);
		appendTable(sb);
		appendSecondaryTables(sb);
		appendInheritance(sb);
		appendHibernateClassAnnotations(sb);
		appendNamedQueries(sb);
		appendResultSetMappings(sb);
		appendNamedNativeQueries(sb);
		appendFilters(sb);
		appendSqlDml(sb);
		appendFetchProfiles(sb);
		appendEntityListeners(sb);
		return sb.toString().stripTrailing();
	}

	private String appendEmbeddable(StringBuilder sb) {
		importType("jakarta.persistence.Embeddable");
		sb.append("@Embeddable\n");
		return sb.toString().stripTrailing();
	}

	private void appendEntity(StringBuilder sb) {
		importType("jakarta.persistence.Entity");
		sb.append("@Entity\n");
	}

	private void appendTable(StringBuilder sb) {
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		if (table != null) {
			sb.append(generateTableAnnotation(table));
		}
	}

	private void appendSecondaryTables(StringBuilder sb) {
		for (SecondaryTableInfo st : templateHelper.getSecondaryTables()) {
			importType("jakarta.persistence.SecondaryTable");
			sb.append("@SecondaryTable(name = \"").append(st.tableName()).append("\"");
			appendSecondaryTableKeyColumns(sb, st.keyColumns());
			sb.append(")\n");
		}
	}

	private void appendSecondaryTableKeyColumns(StringBuilder sb, List<String> keyColumns) {
		if (keyColumns.isEmpty()) {
			return;
		}
		importType("jakarta.persistence.PrimaryKeyJoinColumn");
		if (keyColumns.size() == 1) {
			sb.append(", pkJoinColumns = @PrimaryKeyJoinColumn(name = \"")
					.append(keyColumns.get(0)).append("\")");
			return;
		}
		sb.append(", pkJoinColumns = {");
		for (int i = 0; i < keyColumns.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append("@PrimaryKeyJoinColumn(name = \"")
					.append(keyColumns.get(i)).append("\")");
		}
		sb.append("}");
	}

	private void appendInheritance(StringBuilder sb) {
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		if (inh != null) {
			sb.append(generateInheritanceAnnotation(inh));
		}
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		if (dv != null) {
			importType("jakarta.persistence.DiscriminatorValue");
			sb.append("@DiscriminatorValue(\"").append(dv.value()).append("\")\n");
		}
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		if (pkjc != null) {
			importType("jakarta.persistence.PrimaryKeyJoinColumn");
			sb.append("@PrimaryKeyJoinColumn(name = \"").append(pkjc.name()).append("\")\n");
		}
	}

	private void appendHibernateClassAnnotations(StringBuilder sb) {
		appendImmutable(sb);
		appendDynamicFlags(sb);
		appendBatchSize(sb);
		appendCache(sb);
		appendOptimisticLocking(sb);
		appendRowId(sb);
		appendSubselect(sb);
		appendConcreteProxy(sb);
		appendSqlRestriction(sb);
		appendAccess(sb);
	}

	private void appendImmutable(StringBuilder sb) {
		if (classDetails.hasDirectAnnotationUsage(Immutable.class)) {
			importType("org.hibernate.annotations.Immutable");
			sb.append("@Immutable\n");
		}
	}

	private void appendDynamicFlags(StringBuilder sb) {
		if (classDetails.hasDirectAnnotationUsage(DynamicInsert.class)) {
			importType("org.hibernate.annotations.DynamicInsert");
			sb.append("@DynamicInsert\n");
		}
		if (classDetails.hasDirectAnnotationUsage(DynamicUpdate.class)) {
			importType("org.hibernate.annotations.DynamicUpdate");
			sb.append("@DynamicUpdate\n");
		}
	}

	private void appendBatchSize(StringBuilder sb) {
		BatchSize bs = classDetails.getDirectAnnotationUsage(BatchSize.class);
		if (bs != null) {
			importType("org.hibernate.annotations.BatchSize");
			sb.append("@BatchSize(size = ").append(bs.size()).append(")\n");
		}
	}

	private void appendCache(StringBuilder sb) {
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return;
		}
		importType("org.hibernate.annotations.Cache");
		importType("org.hibernate.annotations.CacheConcurrencyStrategy");
		sb.append("@Cache(usage = CacheConcurrencyStrategy.").append(cache.usage().name());
		if (cache.region() != null && !cache.region().isEmpty()) {
			sb.append(", region = \"").append(cache.region()).append("\"");
		}
		if (!cache.includeLazy()) {
			sb.append(", includeLazy = false");
		}
		sb.append(")\n");
	}

	private void appendOptimisticLocking(StringBuilder sb) {
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol != null && ol.type() != OptimisticLockType.VERSION) {
			importType("org.hibernate.annotations.OptimisticLocking");
			importType("org.hibernate.annotations.OptimisticLockType");
			sb.append("@OptimisticLocking(type = OptimisticLockType.")
					.append(ol.type().name()).append(")\n");
		}
	}

	private void appendRowId(StringBuilder sb) {
		RowId rowId = classDetails.getDirectAnnotationUsage(RowId.class);
		if (rowId != null && rowId.value() != null && !rowId.value().isEmpty()) {
			importType("org.hibernate.annotations.RowId");
			sb.append("@RowId(\"").append(rowId.value()).append("\")\n");
		}
	}

	private void appendSubselect(StringBuilder sb) {
		Subselect subselect = classDetails.getDirectAnnotationUsage(Subselect.class);
		if (subselect != null) {
			importType("org.hibernate.annotations.Subselect");
			sb.append("@Subselect(\"").append(subselect.value()).append("\")\n");
		}
	}

	private void appendConcreteProxy(StringBuilder sb) {
		if (classDetails.hasDirectAnnotationUsage(ConcreteProxy.class)) {
			importType("org.hibernate.annotations.ConcreteProxy");
			sb.append("@ConcreteProxy\n");
		}
	}

	private void appendSqlRestriction(StringBuilder sb) {
		SQLRestriction r = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		if (r != null) {
			importType("org.hibernate.annotations.SQLRestriction");
			sb.append("@SQLRestriction(\"").append(r.value()).append("\")\n");
		}
	}

	private void appendAccess(StringBuilder sb) {
		Access a = classDetails.getDirectAnnotationUsage(Access.class);
		if (a != null && a.value() != AccessType.FIELD) {
			importType("jakarta.persistence.Access");
			importType("jakarta.persistence.AccessType");
			sb.append("@Access(AccessType.").append(a.value().name()).append(")\n");
		}
	}

	private void appendNamedQueries(StringBuilder sb) {
		for (NamedQueryInfo nq : templateHelper.getNamedQueries()) {
			importType("jakarta.persistence.NamedQuery");
			sb.append("@NamedQuery(name = \"").append(nq.name())
					.append("\", query = \"").append(nq.query()).append("\")\n");
		}
	}

	private void appendResultSetMappings(StringBuilder sb) {
		for (SqlResultSetMappingInfo mapping : templateHelper.getSqlResultSetMappings()) {
			importType("jakarta.persistence.SqlResultSetMapping");
			sb.append("@SqlResultSetMapping(name = \"").append(mapping.name()).append("\"");
			appendEntityResults(sb, mapping.entityResults());
			appendColumnResults(sb, mapping.columnResults());
			sb.append(")\n");
		}
	}

	private void appendEntityResults(StringBuilder sb, List<EntityResultInfo> entityResults) {
		if (entityResults.isEmpty()) {
			return;
		}
		importType("jakarta.persistence.EntityResult");
		sb.append(", entities = {");
		for (int i = 0; i < entityResults.size(); i++) {
			if (i > 0) sb.append(", ");
			appendEntityResult(sb, entityResults.get(i));
		}
		sb.append("}");
	}

	private void appendEntityResult(StringBuilder sb, EntityResultInfo er) {
		String simpleEntityClass = importType(er.entityClass());
		sb.append("@EntityResult(entityClass = ").append(simpleEntityClass).append(".class");
		if (er.discriminatorColumn() != null) {
			sb.append(", discriminatorColumn = \"").append(er.discriminatorColumn()).append("\"");
		}
		appendFieldResults(sb, er.fieldResults());
		sb.append(")");
	}

	private void appendFieldResults(StringBuilder sb, List<FieldResultInfo> fieldResults) {
		if (fieldResults.isEmpty()) {
			return;
		}
		importType("jakarta.persistence.FieldResult");
		sb.append(", fields = {");
		for (int j = 0; j < fieldResults.size(); j++) {
			if (j > 0) sb.append(", ");
			FieldResultInfo fr = fieldResults.get(j);
			sb.append("@FieldResult(name = \"").append(fr.name())
					.append("\", column = \"").append(fr.column()).append("\")");
		}
		sb.append("}");
	}

	private void appendColumnResults(
			StringBuilder sb,
			List<TemplateHelper.ColumnResultInfo> columnResults) {
		if (columnResults.isEmpty()) {
			return;
		}
		importType("jakarta.persistence.ColumnResult");
		sb.append(", columns = {");
		for (int i = 0; i < columnResults.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append("@ColumnResult(name = \"")
					.append(columnResults.get(i).name()).append("\")");
		}
		sb.append("}");
	}

	private void appendNamedNativeQueries(StringBuilder sb) {
		for (NamedNativeQueryInfo nnq : templateHelper.getNamedNativeQueries()) {
			importType("jakarta.persistence.NamedNativeQuery");
			sb.append("@NamedNativeQuery(name = \"").append(nnq.name())
					.append("\", query = \"").append(nnq.query()).append("\"");
			if (nnq.resultClass() != null) {
				String simpleResultClass = importType(nnq.resultClass());
				sb.append(", resultClass = ").append(simpleResultClass).append(".class");
			}
			if (nnq.resultSetMapping() != null) {
				sb.append(", resultSetMapping = \"")
						.append(nnq.resultSetMapping()).append("\"");
			}
			sb.append(")\n");
		}
	}

	private void appendFilters(StringBuilder sb) {
		appendFilterDefs(sb);
		appendFilterUsages(sb);
	}

	private void appendFilterDefs(StringBuilder sb) {
		for (FilterDefInfo fd : templateHelper.getFilterDefs()) {
			importType("org.hibernate.annotations.FilterDef");
			sb.append("@FilterDef(name = \"").append(fd.name()).append("\"");
			if (!fd.defaultCondition().isEmpty()) {
				sb.append(", defaultCondition = \"")
						.append(fd.defaultCondition()).append("\"");
			}
			appendFilterDefParameters(sb, fd.parameters());
			sb.append(")\n");
		}
	}

	private void appendFilterDefParameters(StringBuilder sb, Map<String, Class<?>> parameters) {
		if (parameters.isEmpty()) {
			return;
		}
		importType("org.hibernate.annotations.ParamDef");
		sb.append(", parameters = {");
		boolean first = true;
		for (Map.Entry<String, Class<?>> entry : parameters.entrySet()) {
			if (!first) sb.append(", ");
			first = false;
			String simpleType = importType(entry.getValue().getName());
			sb.append("@ParamDef(name = \"").append(entry.getKey())
					.append("\", type = ").append(simpleType).append(".class)");
		}
		sb.append("}");
	}

	private void appendFilterUsages(StringBuilder sb) {
		for (FilterInfo fi : templateHelper.getFilters()) {
			importType("org.hibernate.annotations.Filter");
			sb.append("@Filter(name = \"").append(fi.name()).append("\"");
			if (!fi.condition().isEmpty()) {
				sb.append(", condition = \"").append(fi.condition()).append("\"");
			}
			sb.append(")\n");
		}
	}

	private void appendSqlDml(StringBuilder sb) {
		for (SQLInsert si : templateHelper.getSQLInserts()) {
			importType("org.hibernate.annotations.SQLInsert");
			sb.append("@SQLInsert(sql = \"").append(si.sql()).append("\"");
			if (si.callable()) sb.append(", callable = true");
			sb.append(")\n");
		}
		for (SQLUpdate su : templateHelper.getSQLUpdates()) {
			importType("org.hibernate.annotations.SQLUpdate");
			sb.append("@SQLUpdate(sql = \"").append(su.sql()).append("\"");
			if (su.callable()) sb.append(", callable = true");
			sb.append(")\n");
		}
		for (SQLDelete sd : templateHelper.getSQLDeletes()) {
			importType("org.hibernate.annotations.SQLDelete");
			sb.append("@SQLDelete(sql = \"").append(sd.sql()).append("\"");
			if (sd.callable()) sb.append(", callable = true");
			sb.append(")\n");
		}
		SQLDeleteAll sda = classDetails.getDirectAnnotationUsage(SQLDeleteAll.class);
		if (sda != null) {
			importType("org.hibernate.annotations.SQLDeleteAll");
			sb.append("@SQLDeleteAll(sql = \"").append(sda.sql()).append("\"");
			if (sda.callable()) sb.append(", callable = true");
			sb.append(")\n");
		}
	}

	private void appendFetchProfiles(StringBuilder sb) {
		for (FetchProfile fp : templateHelper.getFetchProfiles()) {
			importType("org.hibernate.annotations.FetchProfile");
			sb.append("@FetchProfile(name = \"").append(fp.name()).append("\"");
			appendFetchOverrides(sb, fp.fetchOverrides());
			sb.append(")\n");
		}
	}

	private void appendFetchOverrides(StringBuilder sb, FetchProfile.FetchOverride[] overrides) {
		if (overrides.length == 0) {
			return;
		}
		importType("org.hibernate.annotations.FetchMode");
		sb.append(", fetchOverrides = {");
		for (int i = 0; i < overrides.length; i++) {
			if (i > 0) sb.append(", ");
			FetchProfile.FetchOverride fo = overrides[i];
			String simpleEntity = importType(fo.entity().getName());
			sb.append("@FetchProfile.FetchOverride(entity = ").append(simpleEntity)
					.append(".class, association = \"").append(fo.association())
					.append("\", mode = FetchMode.").append(fo.mode().name()).append(")");
		}
		sb.append("}");
	}

	private void appendEntityListeners(StringBuilder sb) {
		EntityListeners el = classDetails.getDirectAnnotationUsage(EntityListeners.class);
		if (el == null || el.value() == null || el.value().length == 0) {
			return;
		}
		importType("jakarta.persistence.EntityListeners");
		sb.append("@EntityListeners(");
		if (el.value().length == 1) {
			sb.append(importType(el.value()[0].getName())).append(".class");
		} else {
			sb.append("{");
			for (int i = 0; i < el.value().length; i++) {
				if (i > 0) sb.append(", ");
				sb.append(importType(el.value()[i].getName())).append(".class");
			}
			sb.append("}");
		}
		sb.append(")\n");
	}

	// --- Table annotation ---

	String generateTableAnnotation(Table table) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Table");
		sb.append("@Table(name = \"").append(table.name()).append("\"");
		appendSchemaAndCatalog(sb, table);
		appendUniqueConstraints(sb, table.uniqueConstraints());
		appendIndexes(sb, table.indexes());
		appendCheckConstraints(sb, table.check());
		sb.append(")\n");
		return sb.toString();
	}

	private void appendSchemaAndCatalog(StringBuilder sb, Table table) {
		if (table.schema() != null && !table.schema().isEmpty()) {
			sb.append(", schema = \"").append(table.schema()).append("\"");
		}
		if (table.catalog() != null && !table.catalog().isEmpty()) {
			sb.append(", catalog = \"").append(table.catalog()).append("\"");
		}
	}

	private void appendUniqueConstraints(StringBuilder sb, UniqueConstraint[] ucs) {
		if (ucs == null || ucs.length == 0) {
			return;
		}
		importType("jakarta.persistence.UniqueConstraint");
		sb.append(", uniqueConstraints = ");
		appendAnnotationArray(sb, ucs, this::formatUniqueConstraint);
	}

	private void appendIndexes(StringBuilder sb, Index[] indexes) {
		if (indexes == null || indexes.length == 0) {
			return;
		}
		importType("jakarta.persistence.Index");
		sb.append(", indexes = ");
		appendAnnotationArray(sb, indexes, this::formatIndex);
	}

	private void appendCheckConstraints(StringBuilder sb, CheckConstraint[] checks) {
		if (checks == null || checks.length == 0) {
			return;
		}
		importType("jakarta.persistence.CheckConstraint");
		sb.append(", check = ");
		appendAnnotationArray(sb, checks, this::formatCheckConstraint);
	}

	private <T> void appendAnnotationArray(
			StringBuilder sb, T[] items, java.util.function.Function<T, String> formatter) {
		if (items.length == 1) {
			sb.append(formatter.apply(items[0]));
		} else {
			sb.append("{ ");
			for (int i = 0; i < items.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append(formatter.apply(items[i]));
			}
			sb.append(" }");
		}
	}

	String formatCheckConstraint(CheckConstraint cc) {
		StringBuilder sb = new StringBuilder("@CheckConstraint(");
		if (cc.name() != null && !cc.name().isEmpty()) {
			sb.append("name = \"").append(cc.name()).append("\", ");
		}
		sb.append("constraint = \"").append(cc.constraint()).append("\")");
		return sb.toString();
	}

	String formatIndex(Index idx) {
		StringBuilder sb = new StringBuilder("@Index(");
		if (idx.name() != null && !idx.name().isEmpty()) {
			sb.append("name = \"").append(idx.name()).append("\", ");
		}
		sb.append("columnList = \"").append(idx.columnList()).append("\"");
		if (idx.unique()) {
			sb.append(", unique = true");
		}
		sb.append(")");
		return sb.toString();
	}

	String formatUniqueConstraint(UniqueConstraint uc) {
		StringBuilder sb = new StringBuilder("@UniqueConstraint(");
		if (uc.name() != null && !uc.name().isEmpty()) {
			sb.append("name = \"").append(uc.name()).append("\", ");
		}
		sb.append("columnNames = { ");
		String[] cols = uc.columnNames();
		for (int i = 0; i < cols.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append("\"").append(cols[i]).append("\"");
		}
		sb.append(" })");
		return sb.toString();
	}

	// --- Inheritance annotation ---

	String generateInheritanceAnnotation(Inheritance inh) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Inheritance");
		importType("jakarta.persistence.InheritanceType");
		sb.append("@Inheritance(strategy = InheritanceType.")
				.append(inh.strategy().name()).append(")\n");
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc != null) {
			appendDiscriminatorColumn(sb, dc);
		}
		return sb.toString();
	}

	private void appendDiscriminatorColumn(StringBuilder sb, DiscriminatorColumn dc) {
		importType("jakarta.persistence.DiscriminatorColumn");
		sb.append("@DiscriminatorColumn(name = \"").append(dc.name()).append("\"");
		if (dc.discriminatorType() != DiscriminatorType.STRING) {
			importType("jakarta.persistence.DiscriminatorType");
			sb.append(", discriminatorType = DiscriminatorType.")
					.append(dc.discriminatorType().name());
		}
		if (dc.length() != 31) {
			sb.append(", length = ").append(dc.length());
		}
		sb.append(")\n");
	}

	private String importType(String fqcn) {
		return importContext.importType(fqcn);
	}
}
