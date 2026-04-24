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
package org.hibernate.tool.reveng.internal.exporter.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;

/**
 * Handles query, filter, fetch profile, SQL DML override, entity listener,
 * and lifecycle callback information for mapping XML template generation.
 *
 * @author Koen Aers
 */
public class MappingQueryAndFilterHelper {

	private final ClassDetails classDetails;

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] LIFECYCLE_ANNOTATIONS = new Class[] {
			PrePersist.class, PostPersist.class,
			PreRemove.class, PostRemove.class,
			PreUpdate.class, PostUpdate.class,
			PostLoad.class
	};

	MappingQueryAndFilterHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
	}

	// --- Filters ---

	public List<MappingXmlHelper.FilterInfo> getFilters() {
		List<MappingXmlHelper.FilterInfo> result = new ArrayList<>();
		Filter single = classDetails.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new MappingXmlHelper.FilterInfo(single.name(), single.condition()));
		}
		Filters container = classDetails.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new MappingXmlHelper.FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public List<MappingXmlHelper.FilterDefInfo> getFilterDefs() {
		List<MappingXmlHelper.FilterDefInfo> result = new ArrayList<>();
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

	private MappingXmlHelper.FilterDefInfo toFilterDefInfo(FilterDef fd) {
		Map<String, String> params = new LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), pd.type().getName());
			}
		}
		return new MappingXmlHelper.FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	public List<MappingXmlHelper.FilterInfo> getCollectionFilters(FieldDetails field) {
		List<MappingXmlHelper.FilterInfo> result = new ArrayList<>();
		Filter single = field.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new MappingXmlHelper.FilterInfo(single.name(), single.condition()));
		}
		Filters container = field.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new MappingXmlHelper.FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	// --- Named queries ---

	public List<MappingXmlHelper.NamedQueryInfo> getNamedQueries() {
		List<MappingXmlHelper.NamedQueryInfo> result = new ArrayList<>();
		NamedQuery single = classDetails.getDirectAnnotationUsage(NamedQuery.class);
		if (single != null) {
			result.add(new MappingXmlHelper.NamedQueryInfo(single.name(), single.query()));
		}
		NamedQueries container = classDetails.getDirectAnnotationUsage(NamedQueries.class);
		if (container != null) {
			for (NamedQuery nq : container.value()) {
				result.add(new MappingXmlHelper.NamedQueryInfo(nq.name(), nq.query()));
			}
		}
		return result;
	}

	public List<MappingXmlHelper.NamedQueryInfo> getNamedNativeQueries() {
		List<MappingXmlHelper.NamedQueryInfo> result = new ArrayList<>();
		NamedNativeQuery single = classDetails.getDirectAnnotationUsage(NamedNativeQuery.class);
		if (single != null) {
			result.add(new MappingXmlHelper.NamedQueryInfo(single.name(), single.query()));
		}
		NamedNativeQueries container = classDetails.getDirectAnnotationUsage(NamedNativeQueries.class);
		if (container != null) {
			for (NamedNativeQuery nnq : container.value()) {
				result.add(new MappingXmlHelper.NamedQueryInfo(nnq.name(), nnq.query()));
			}
		}
		return result;
	}

	// --- Fetch profiles ---

	public List<MappingXmlHelper.FetchProfileInfo> getFetchProfiles() {
		List<MappingXmlHelper.FetchProfileInfo> result = new ArrayList<>();
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

	private MappingXmlHelper.FetchProfileInfo toFetchProfileInfo(FetchProfile fp) {
		List<MappingXmlHelper.FetchOverrideInfo> overrides = new ArrayList<>();
		for (FetchProfile.FetchOverride fo : fp.fetchOverrides()) {
			overrides.add(new MappingXmlHelper.FetchOverrideInfo(
					fo.entity().getName(), fo.association(), fo.mode().name().toLowerCase()));
		}
		return new MappingXmlHelper.FetchProfileInfo(fp.name(), overrides);
	}

	// --- SQL operations ---

	public MappingXmlHelper.CustomSqlInfo getSQLInsert() {
		SQLInsert si = classDetails.getDirectAnnotationUsage(SQLInsert.class);
		return si != null ? new MappingXmlHelper.CustomSqlInfo(si.sql(), si.callable()) : null;
	}

	public MappingXmlHelper.CustomSqlInfo getSQLUpdate() {
		SQLUpdate su = classDetails.getDirectAnnotationUsage(SQLUpdate.class);
		return su != null ? new MappingXmlHelper.CustomSqlInfo(su.sql(), su.callable()) : null;
	}

	public MappingXmlHelper.CustomSqlInfo getSQLDelete() {
		SQLDelete sd = classDetails.getDirectAnnotationUsage(SQLDelete.class);
		return sd != null ? new MappingXmlHelper.CustomSqlInfo(sd.sql(), sd.callable()) : null;
	}

	public MappingXmlHelper.CustomSqlInfo getSQLDeleteAll() {
		SQLDeleteAll sda = classDetails.getDirectAnnotationUsage(SQLDeleteAll.class);
		return sda != null ? new MappingXmlHelper.CustomSqlInfo(sda.sql(), sda.callable()) : null;
	}

	// --- Entity listeners ---

	public List<String> getEntityListenerClassNames() {
		EntityListeners el = classDetails.getDirectAnnotationUsage(EntityListeners.class);
		if (el == null || el.value() == null || el.value().length == 0) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (Class<?> c : el.value()) {
			result.add(c.getName());
		}
		return result;
	}

	// --- Lifecycle callbacks ---

	public List<MappingXmlHelper.LifecycleCallbackInfo> getLifecycleCallbacks() {
		List<MappingXmlHelper.LifecycleCallbackInfo> result = new ArrayList<>();
		for (MethodDetails method : classDetails.getMethods()) {
			for (Class<? extends Annotation> ann : LIFECYCLE_ANNOTATIONS) {
				if (method.hasDirectAnnotationUsage(ann)) {
					result.add(new MappingXmlHelper.LifecycleCallbackInfo(
							toElementName(ann), method.getName()));
				}
			}
		}
		return result;
	}

	private String toElementName(Class<? extends Annotation> ann) {
		StringBuilder sb = new StringBuilder();
		for (char c : ann.getSimpleName().toCharArray()) {
			if (Character.isUpperCase(c) && !sb.isEmpty()) {
				sb.append('-');
			}
			sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}
}
