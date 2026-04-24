/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeletes;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLInserts;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SQLUpdates;

import org.hibernate.models.spi.ClassDetails;

import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.ColumnResultInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.EntityResultInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FieldResultInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterDefInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedNativeQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.NamedQueryInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.SecondaryTableInfo;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.SqlResultSetMappingInfo;

public class QueryAndFilterHelper {

	private final ClassDetails classDetails;

	QueryAndFilterHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	// --- Named queries ---

	public List<NamedQueryInfo> getNamedQueries() {
		List<NamedQueryInfo> result = new ArrayList<>();
		NamedQuery single = classDetails.getDirectAnnotationUsage(NamedQuery.class);
		if (single != null) {
			result.add(new NamedQueryInfo(single.name(), single.query()));
		}
		NamedQueries container = classDetails.getDirectAnnotationUsage(NamedQueries.class);
		if (container != null) {
			for (NamedQuery nq : container.value()) {
				result.add(new NamedQueryInfo(nq.name(), nq.query()));
			}
		}
		return result;
	}

	public List<NamedNativeQueryInfo> getNamedNativeQueries() {
		List<NamedNativeQueryInfo> result = new ArrayList<>();
		NamedNativeQuery single = classDetails.getDirectAnnotationUsage(
				NamedNativeQuery.class);
		if (single != null) {
			result.add(toNamedNativeQueryInfo(single));
		}
		NamedNativeQueries container = classDetails.getDirectAnnotationUsage(
				NamedNativeQueries.class);
		if (container != null) {
			for (NamedNativeQuery nnq : container.value()) {
				result.add(toNamedNativeQueryInfo(nnq));
			}
		}
		return result;
	}

	private NamedNativeQueryInfo toNamedNativeQueryInfo(NamedNativeQuery nnq) {
		String resultClassName = null;
		if (nnq.resultClass() != null && nnq.resultClass() != void.class) {
			resultClassName = nnq.resultClass().getName();
		}
		String resultSetMapping =
				nnq.resultSetMapping() != null && !nnq.resultSetMapping().isEmpty()
						? nnq.resultSetMapping() : null;
		return new NamedNativeQueryInfo(
				nnq.name(), nnq.query(), resultClassName, resultSetMapping);
	}

	// --- SQL result set mappings ---

	public List<SqlResultSetMappingInfo> getSqlResultSetMappings() {
		List<SqlResultSetMappingInfo> result = new ArrayList<>();
		SqlResultSetMapping single = classDetails.getDirectAnnotationUsage(
				SqlResultSetMapping.class);
		if (single != null) {
			result.add(toSqlResultSetMappingInfo(single));
		}
		SqlResultSetMappings container = classDetails.getDirectAnnotationUsage(
				SqlResultSetMappings.class);
		if (container != null) {
			for (SqlResultSetMapping mapping : container.value()) {
				result.add(toSqlResultSetMappingInfo(mapping));
			}
		}
		return result;
	}

	private SqlResultSetMappingInfo toSqlResultSetMappingInfo(
			SqlResultSetMapping mapping) {
		List<EntityResultInfo> entityResults = new ArrayList<>();
		for (EntityResult er : mapping.entities()) {
			List<FieldResultInfo> fieldResults = new ArrayList<>();
			for (FieldResult fr : er.fields()) {
				fieldResults.add(new FieldResultInfo(fr.name(), fr.column()));
			}
			String discriminator =
					er.discriminatorColumn() != null && !er.discriminatorColumn().isEmpty()
							? er.discriminatorColumn() : null;
			entityResults.add(new EntityResultInfo(
					er.entityClass().getName(), discriminator, fieldResults));
		}
		List<ColumnResultInfo> columnResults = new ArrayList<>();
		for (ColumnResult cr : mapping.columns()) {
			columnResults.add(new ColumnResultInfo(cr.name()));
		}
		return new SqlResultSetMappingInfo(
				mapping.name(), entityResults, columnResults);
	}

	// --- Filters ---

	public List<FilterInfo> getFilters() {
		List<FilterInfo> result = new ArrayList<>();
		Filter single = classDetails.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new FilterInfo(single.name(), single.condition()));
		}
		Filters container = classDetails.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public List<FilterDefInfo> getFilterDefs() {
		List<FilterDefInfo> result = new ArrayList<>();
		FilterDef single = classDetails.getDirectAnnotationUsage(FilterDef.class);
		if (single != null) {
			result.add(toFilterDefInfo(single));
		}
		FilterDefs container = classDetails.getDirectAnnotationUsage(FilterDefs.class);
		if (container != null) {
			for (FilterDef fd : container.value()) {
				result.add(toFilterDefInfo(fd));
			}
		}
		return result;
	}

	private FilterDefInfo toFilterDefInfo(FilterDef fd) {
		Map<String, Class<?>> params = new LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), pd.type());
			}
		}
		return new FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	// --- SQL DML ---

	public List<SQLInsert> getSQLInserts() {
		List<SQLInsert> result = new ArrayList<>();
		SQLInsert single = classDetails.getDirectAnnotationUsage(SQLInsert.class);
		if (single != null) {
			result.add(single);
		}
		SQLInserts container = classDetails.getDirectAnnotationUsage(SQLInserts.class);
		if (container != null) {
			for (SQLInsert si : container.value()) {
				result.add(si);
			}
		}
		return result;
	}

	public List<SQLUpdate> getSQLUpdates() {
		List<SQLUpdate> result = new ArrayList<>();
		SQLUpdate single = classDetails.getDirectAnnotationUsage(SQLUpdate.class);
		if (single != null) {
			result.add(single);
		}
		SQLUpdates container = classDetails.getDirectAnnotationUsage(SQLUpdates.class);
		if (container != null) {
			for (SQLUpdate su : container.value()) {
				result.add(su);
			}
		}
		return result;
	}

	public List<SQLDelete> getSQLDeletes() {
		List<SQLDelete> result = new ArrayList<>();
		SQLDelete single = classDetails.getDirectAnnotationUsage(SQLDelete.class);
		if (single != null) {
			result.add(single);
		}
		SQLDeletes container = classDetails.getDirectAnnotationUsage(SQLDeletes.class);
		if (container != null) {
			for (SQLDelete sd : container.value()) {
				result.add(sd);
			}
		}
		return result;
	}

	// --- Fetch profiles ---

	public List<FetchProfile> getFetchProfiles() {
		List<FetchProfile> result = new ArrayList<>();
		FetchProfile single = classDetails.getDirectAnnotationUsage(
				FetchProfile.class);
		if (single != null) {
			result.add(single);
		}
		FetchProfiles container = classDetails.getDirectAnnotationUsage(
				FetchProfiles.class);
		if (container != null) {
			for (FetchProfile fp : container.value()) {
				result.add(fp);
			}
		}
		return result;
	}

	// --- Secondary tables ---

	public List<SecondaryTableInfo> getSecondaryTables() {
		List<SecondaryTableInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(
				SecondaryTable.class);
		if (single != null) {
			result.add(toSecondaryTableInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(
				SecondaryTables.class);
		if (container != null) {
			for (SecondaryTable st : container.value()) {
				result.add(toSecondaryTableInfo(st));
			}
		}
		return result;
	}

	private SecondaryTableInfo toSecondaryTableInfo(SecondaryTable st) {
		List<String> keyColumns = new ArrayList<>();
		if (st.pkJoinColumns() != null) {
			for (PrimaryKeyJoinColumn pkjc : st.pkJoinColumns()) {
				if (pkjc.name() != null && !pkjc.name().isEmpty()) {
					keyColumns.add(pkjc.name());
				}
			}
		}
		return new SecondaryTableInfo(st.name(), keyColumns);
	}
}
