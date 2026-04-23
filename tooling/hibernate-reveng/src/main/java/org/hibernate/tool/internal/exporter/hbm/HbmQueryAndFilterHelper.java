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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

import org.hibernate.tool.internal.util.TypeHelper;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Handles entity-level filters, filter definitions, named queries,
 * named native queries, secondary table joins, SQL DML overrides,
 * and fetch profiles for hbm.xml template generation.
 *
 * @author Koen Aers
 */
public class HbmQueryAndFilterHelper {

	private final ClassDetails classDetails;
	private final Map<String, List<String>> metaAttributes;

	HbmQueryAndFilterHelper(ClassDetails classDetails,
							Map<String, List<String>> metaAttributes) {
		this.classDetails = classDetails;
		this.metaAttributes = metaAttributes;
	}

	private String getClassMetaValue(String key) {
		List<String> values = metaAttributes.get(key);
		return values != null && !values.isEmpty() ? values.get(0) : null;
	}

	// --- Filters ---

	public List<HbmTemplateHelper.FilterInfo> getFilters() {
		List<HbmTemplateHelper.FilterInfo> result = new ArrayList<>();
		Filter single = classDetails.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new HbmTemplateHelper.FilterInfo(single.name(), single.condition()));
		}
		Filters container = classDetails.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new HbmTemplateHelper.FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public List<HbmTemplateHelper.FilterDefInfo> getFilterDefs() {
		List<HbmTemplateHelper.FilterDefInfo> result = new ArrayList<>();
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

	private HbmTemplateHelper.FilterDefInfo toFilterDefInfo(FilterDef fd) {
		Map<String, String> params = new LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), TypeHelper.toHibernateType(pd.type().getName()));
			}
		}
		return new HbmTemplateHelper.FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	// --- Named queries ---

	public List<HbmTemplateHelper.NamedQueryInfo> getNamedQueries() {
		List<HbmTemplateHelper.NamedQueryInfo> result = new ArrayList<>();
		org.hibernate.annotations.NamedQuery hibSingle =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedQuery.class);
		if (hibSingle != null) {
			result.add(toNamedQueryInfo(hibSingle));
		}
		org.hibernate.annotations.NamedQueries hibContainer =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedQueries.class);
		if (hibContainer != null) {
			for (org.hibernate.annotations.NamedQuery nq : hibContainer.value()) {
				result.add(toNamedQueryInfo(nq));
			}
		}
		if (result.isEmpty()) {
			NamedQuery single = classDetails.getDirectAnnotationUsage(NamedQuery.class);
			if (single != null) {
				result.add(new HbmTemplateHelper.NamedQueryInfo(single.name(), single.query(),
						"", false, "", -1, -1, "", false));
			}
			NamedQueries container = classDetails.getDirectAnnotationUsage(NamedQueries.class);
			if (container != null) {
				for (NamedQuery nq : container.value()) {
					result.add(new HbmTemplateHelper.NamedQueryInfo(nq.name(), nq.query(),
							"", false, "", -1, -1, "", false));
				}
			}
		}
		return result;
	}

	private HbmTemplateHelper.NamedQueryInfo toNamedQueryInfo(
			org.hibernate.annotations.NamedQuery nq) {
		String flushMode = nq.flushMode() != FlushModeType.PERSISTENCE_CONTEXT
				? nq.flushMode().name().toLowerCase() : "";
		return new HbmTemplateHelper.NamedQueryInfo(nq.name(), nq.query(), flushMode,
				nq.cacheable(), nq.cacheRegion(), nq.fetchSize(), nq.timeout(),
				nq.comment(), nq.readOnly());
	}

	public List<HbmTemplateHelper.NamedNativeQueryInfo> getNamedNativeQueries() {
		List<HbmTemplateHelper.NamedNativeQueryInfo> result = new ArrayList<>();
		org.hibernate.annotations.NamedNativeQuery hibSingle =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedNativeQuery.class);
		if (hibSingle != null) {
			result.add(toNamedNativeQueryInfo(hibSingle.name(), hibSingle.query(),
					hibSingle.flushMode(), hibSingle.cacheable(), hibSingle.cacheRegion(),
					hibSingle.fetchSize(), hibSingle.timeout(), hibSingle.comment(),
					hibSingle.readOnly(), hibSingle.querySpaces(),
					hibSingle.resultClass(), hibSingle.resultSetMapping()));
		}
		org.hibernate.annotations.NamedNativeQueries hibContainer =
				classDetails.getDirectAnnotationUsage(org.hibernate.annotations.NamedNativeQueries.class);
		if (hibContainer != null) {
			for (org.hibernate.annotations.NamedNativeQuery nnq : hibContainer.value()) {
				result.add(toNamedNativeQueryInfo(nnq.name(), nnq.query(),
						nnq.flushMode(), nnq.cacheable(), nnq.cacheRegion(),
						nnq.fetchSize(), nnq.timeout(), nnq.comment(),
						nnq.readOnly(), nnq.querySpaces(),
						nnq.resultClass(), nnq.resultSetMapping()));
			}
		}
		if (result.isEmpty()) {
			NamedNativeQuery single = classDetails.getDirectAnnotationUsage(NamedNativeQuery.class);
			if (single != null) {
				result.add(toNamedNativeQueryInfo(single.name(), single.query(),
						FlushModeType.PERSISTENCE_CONTEXT, false, "", -1, -1, "",
						false, new String[0], single.resultClass(), single.resultSetMapping()));
			}
			NamedNativeQueries container = classDetails.getDirectAnnotationUsage(NamedNativeQueries.class);
			if (container != null) {
				for (NamedNativeQuery nnq : container.value()) {
					result.add(toNamedNativeQueryInfo(nnq.name(), nnq.query(),
							FlushModeType.PERSISTENCE_CONTEXT, false, "", -1, -1, "",
							false, new String[0], nnq.resultClass(), nnq.resultSetMapping()));
				}
			}
		}
		return result;
	}

	private HbmTemplateHelper.NamedNativeQueryInfo toNamedNativeQueryInfo(
			String name, String query, FlushModeType flushModeType,
			boolean cacheable, String cacheRegion, int fetchSize, int timeout,
			String comment, boolean readOnly, String[] spaces,
			Class<?> resultClass, String resultSetMapping) {
		String flushMode = flushModeType != FlushModeType.PERSISTENCE_CONTEXT
				? flushModeType.name().toLowerCase() : "";
		List<String> querySpaces = spaces != null && spaces.length > 0
				? List.of(spaces) : List.of();
		List<HbmTemplateHelper.EntityReturnInfo> entityReturns = new ArrayList<>();
		List<HbmTemplateHelper.ScalarReturnInfo> scalarReturns = new ArrayList<>();
		if (resultClass != null && resultClass != void.class) {
			entityReturns.add(new HbmTemplateHelper.EntityReturnInfo(
					resultClass.getName(), resultClass.getName(), "", List.of()));
		}
		if (resultSetMapping != null && !resultSetMapping.isEmpty()) {
			SqlResultSetMapping mapping = findSqlResultSetMapping(resultSetMapping);
			if (mapping != null) {
				for (EntityResult er : mapping.entities()) {
					List<HbmTemplateHelper.FieldMappingInfo> fieldMappings = new ArrayList<>();
					for (FieldResult fr : er.fields()) {
						fieldMappings.add(new HbmTemplateHelper.FieldMappingInfo(fr.name(), fr.column()));
					}
					entityReturns.add(new HbmTemplateHelper.EntityReturnInfo(
							er.entityClass().getName(), er.entityClass().getName(),
							er.discriminatorColumn(), fieldMappings));
				}
				for (ColumnResult cr : mapping.columns()) {
					scalarReturns.add(new HbmTemplateHelper.ScalarReturnInfo(cr.name()));
				}
			}
		}
		List<HbmTemplateHelper.ReturnJoinInfo> returnJoins = new ArrayList<>();
		List<HbmTemplateHelper.LoadCollectionInfo> loadCollections = new ArrayList<>();
		String rjAlias = getClassMetaValue("hibernate.sql-query." + name + ".return-join.alias");
		String rjProperty = getClassMetaValue("hibernate.sql-query." + name + ".return-join.property");
		if (rjAlias != null && rjProperty != null) {
			returnJoins.add(new HbmTemplateHelper.ReturnJoinInfo(rjAlias, rjProperty));
		}
		String lcAlias = getClassMetaValue("hibernate.sql-query." + name + ".load-collection.alias");
		String lcRole = getClassMetaValue("hibernate.sql-query." + name + ".load-collection.role");
		String lcLockMode = getClassMetaValue("hibernate.sql-query." + name + ".load-collection.lock-mode");
		if (lcAlias != null && lcRole != null) {
			loadCollections.add(new HbmTemplateHelper.LoadCollectionInfo(lcAlias, lcRole, lcLockMode));
		}
		String retAlias = getClassMetaValue("hibernate.sql-query." + name + ".return.alias");
		String retClass = getClassMetaValue("hibernate.sql-query." + name + ".return.class");
		if (retClass != null) {
			entityReturns.add(new HbmTemplateHelper.EntityReturnInfo(
					retAlias != null ? retAlias : retClass,
					retClass, "", List.of()));
		}
		return new HbmTemplateHelper.NamedNativeQueryInfo(name, query, flushMode, cacheable, cacheRegion,
				fetchSize, timeout, comment, readOnly, querySpaces,
				entityReturns, scalarReturns, returnJoins, loadCollections);
	}

	private SqlResultSetMapping findSqlResultSetMapping(String name) {
		SqlResultSetMapping single = classDetails.getDirectAnnotationUsage(SqlResultSetMapping.class);
		if (single != null && name.equals(single.name())) {
			return single;
		}
		SqlResultSetMappings container = classDetails.getDirectAnnotationUsage(SqlResultSetMappings.class);
		if (container != null) {
			for (SqlResultSetMapping mapping : container.value()) {
				if (name.equals(mapping.name())) {
					return mapping;
				}
			}
		}
		return null;
	}

	// --- SecondaryTable / Joins ---

	public List<HbmTemplateHelper.JoinInfo> getJoins() {
		List<HbmTemplateHelper.JoinInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(SecondaryTable.class);
		if (single != null) {
			result.add(toJoinInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(SecondaryTables.class);
		if (container != null) {
			for (SecondaryTable st : container.value()) {
				result.add(toJoinInfo(st));
			}
		}
		return result;
	}

	public List<FieldDetails> getJoinProperties(String tableName) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			Column col = field.getDirectAnnotationUsage(Column.class);
			if (col != null && tableName.equals(col.table())) {
				result.add(field);
			}
		}
		return result;
	}

	private HbmTemplateHelper.JoinInfo toJoinInfo(SecondaryTable st) {
		List<String> keyColumns = new ArrayList<>();
		if (st.pkJoinColumns() != null) {
			for (PrimaryKeyJoinColumn pkjc : st.pkJoinColumns()) {
				keyColumns.add(pkjc.name());
			}
		}
		return new HbmTemplateHelper.JoinInfo(st.name(), keyColumns);
	}

	public String getJoinComment(String tableName) {
		return getClassMetaValue("hibernate.join.comment." + tableName);
	}

	// --- SQL operations ---

	public HbmTemplateHelper.CustomSqlInfo getSQLInsert() {
		SQLInsert si = classDetails.getDirectAnnotationUsage(SQLInsert.class);
		return si != null ? new HbmTemplateHelper.CustomSqlInfo(si.sql(), si.callable()) : null;
	}

	public HbmTemplateHelper.CustomSqlInfo getSQLUpdate() {
		SQLUpdate su = classDetails.getDirectAnnotationUsage(SQLUpdate.class);
		return su != null ? new HbmTemplateHelper.CustomSqlInfo(su.sql(), su.callable()) : null;
	}

	public HbmTemplateHelper.CustomSqlInfo getSQLDelete() {
		SQLDelete sd = classDetails.getDirectAnnotationUsage(SQLDelete.class);
		return sd != null ? new HbmTemplateHelper.CustomSqlInfo(sd.sql(), sd.callable()) : null;
	}

	public HbmTemplateHelper.CustomSqlInfo getSQLDeleteAll() {
		SQLDeleteAll sda = classDetails.getDirectAnnotationUsage(SQLDeleteAll.class);
		return sda != null ? new HbmTemplateHelper.CustomSqlInfo(sda.sql(), sda.callable()) : null;
	}

	// --- Fetch profiles ---

	public List<HbmTemplateHelper.FetchProfileInfo> getFetchProfiles() {
		List<HbmTemplateHelper.FetchProfileInfo> result = new ArrayList<>();
		FetchProfile single = classDetails.getDirectAnnotationUsage(FetchProfile.class);
		if (single != null) {
			result.add(toFetchProfileInfo(single));
		}
		FetchProfiles container = classDetails.getDirectAnnotationUsage(FetchProfiles.class);
		if (container != null) {
			for (FetchProfile fp : container.value()) {
				result.add(toFetchProfileInfo(fp));
			}
		}
		return result;
	}

	private HbmTemplateHelper.FetchProfileInfo toFetchProfileInfo(FetchProfile fp) {
		List<HbmTemplateHelper.FetchOverrideInfo> overrides = new ArrayList<>();
		for (FetchProfile.FetchOverride fo : fp.fetchOverrides()) {
			overrides.add(new HbmTemplateHelper.FetchOverrideInfo(
					fo.entity().getName(), fo.association(), fo.mode().name().toLowerCase()));
		}
		return new HbmTemplateHelper.FetchProfileInfo(fp.name(), overrides);
	}
}
