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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

import org.hibernate.tool.internal.util.CascadeUtil;
import org.hibernate.tool.internal.util.TypeHelper;

import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Handles collection-specific HBM attributes: collection tag, inverse,
 * lazy, fetch, cascade, ordering, caching, filters, list/map/idbag
 * specifics, collection SQL DML, and sort.
 *
 * @author Koen Aers
 */
class HbmCollectionAttributeHelper {

	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;

	HbmCollectionAttributeHelper(Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.fieldMetaAttributes = fieldMetaAttributes;
	}

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field);
	}

	private List<String> getFieldMetaAttribute(FieldDetails field, String name) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field)
				.getOrDefault(name, Collections.emptyList());
	}

	// --- Collection type ---

	String getCollectionTag(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> tagMeta = fieldMeta.get("hibernate.collection.tag");
		if (tagMeta != null && !tagMeta.isEmpty()) {
			return tagMeta.get(0);
		}
		if (field.hasDirectAnnotationUsage(Bag.class)) {
			return "bag";
		}
		if (field.hasDirectAnnotationUsage(OrderColumn.class)) {
			return "list";
		}
		if (field.hasDirectAnnotationUsage(CollectionId.class)) {
			return "idbag";
		}
		TypeDetails type = field.getType();
		if (type.isImplementor(java.util.Map.class)) {
			return "map";
		}
		if (field.isArray()) {
			TypeDetails elementType = field.getElementType();
			if (elementType != null
					&& elementType.getTypeKind() == TypeDetails.Kind.PRIMITIVE) {
				return "primitive-array";
			}
			return "array";
		}
		return "set";
	}

	boolean isCollectionInverse(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null) {
			if (o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()) {
				return true;
			}
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null) {
			if (m2m.mappedBy() != null && !m2m.mappedBy().isEmpty()) {
				return true;
			}
		}
		List<String> inverseAttr = getFieldMetaAttribute(field, "hibernate.inverse");
		if (inverseAttr != null && !inverseAttr.isEmpty()) {
			return "true".equals(inverseAttr.get(0));
		}
		return false;
	}

	String getCollectionLazy(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.fetch() == FetchType.EAGER) {
			return "false";
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null && m2m.fetch() == FetchType.EAGER) {
			return "false";
		}
		ElementCollection ec = field.getDirectAnnotationUsage(ElementCollection.class);
		if (ec != null && ec.fetch() == FetchType.EAGER) {
			return "false";
		}
		List<String> lazyAttr = getFieldMetaAttribute(field, "hibernate.lazy");
		if (lazyAttr != null && !lazyAttr.isEmpty()) {
			return lazyAttr.get(0);
		}
		return null;
	}

	String getCollectionFetchMode(FieldDetails field) {
		Fetch fetch = field.getDirectAnnotationUsage(Fetch.class);
		if (fetch == null) {
			return null;
		}
		return switch (fetch.value()) {
			case JOIN -> "join";
			case SELECT -> "select";
			case SUBSELECT -> "subselect";
		};
	}

	int getCollectionBatchSize(FieldDetails field) {
		BatchSize bs = field.getDirectAnnotationUsage(BatchSize.class);
		return bs != null ? bs.size() : 0;
	}

	String getCollectionCascadeString(FieldDetails field) {
		Map<String, List<String>> fieldMeta = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> rawCascade = fieldMeta.get("hibernate.cascade");
		if (rawCascade != null && !rawCascade.isEmpty()) {
			return rawCascade.get(0);
		}
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.cascade().length > 0) {
			return CascadeUtil.formatJpaCascade(o2m.cascade());
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m != null && m2m.cascade().length > 0) {
			return CascadeUtil.formatJpaCascade(m2m.cascade());
		}
		Cascade cascade = field.getDirectAnnotationUsage(Cascade.class);
		if (cascade != null && cascade.value().length > 0) {
			return CascadeUtil.formatHibernateCascade(cascade);
		}
		return null;
	}

	String getCollectionOrderBy(FieldDetails field) {
		OrderBy ob = field.getDirectAnnotationUsage(OrderBy.class);
		return ob != null && ob.value() != null && !ob.value().isEmpty()
				? ob.value() : null;
	}

	String getCollectionCacheUsage(FieldDetails field) {
		Cache cache = field.getDirectAnnotationUsage(Cache.class);
		if (cache == null || cache.usage() == CacheConcurrencyStrategy.NONE) {
			return null;
		}
		return cache.usage().name().toLowerCase().replace('_', '-');
	}

	String getCollectionCacheRegion(FieldDetails field) {
		Cache cache = field.getDirectAnnotationUsage(Cache.class);
		return cache != null && cache.region() != null && !cache.region().isEmpty()
				? cache.region() : null;
	}

	List<HbmTemplateHelper.FilterInfo> getCollectionFilters(FieldDetails field) {
		List<HbmTemplateHelper.FilterInfo> result = new ArrayList<>();
		Filter single = field.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new HbmTemplateHelper.FilterInfo(single.name(), single.condition()));
		}
		Filters container = field.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new HbmTemplateHelper.FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	// --- List-specific ---

	String getListIndexColumnName(FieldDetails field) {
		OrderColumn oc = field.getDirectAnnotationUsage(OrderColumn.class);
		return oc != null ? oc.name() : null;
	}

	// --- Map-specific ---

	String getMapKeyColumnName(FieldDetails field) {
		MapKeyColumn mkc = field.getDirectAnnotationUsage(MapKeyColumn.class);
		return mkc != null ? mkc.name() : null;
	}

	String getMapKeyType(FieldDetails field) {
		TypeDetails mapKeyType = field.getMapKeyType();
		if (mapKeyType != null) {
			return TypeHelper.toHibernateType(
					mapKeyType.determineRawClass().getClassName());
		}
		return null;
	}

	boolean hasMapKeyJoinColumn(FieldDetails field) {
		return field.hasDirectAnnotationUsage(MapKeyJoinColumn.class);
	}

	String getMapKeyJoinColumnName(FieldDetails field) {
		MapKeyJoinColumn mkjc = field.getDirectAnnotationUsage(MapKeyJoinColumn.class);
		return mkjc != null ? mkjc.name() : null;
	}

	String getMapKeyEntityClass(FieldDetails field) {
		TypeDetails mapKeyType = field.getMapKeyType();
		if (mapKeyType != null) {
			return mapKeyType.determineRawClass().getClassName();
		}
		return null;
	}

	// --- IdBag-specific ---

	String getCollectionIdColumnName(FieldDetails field) {
		CollectionId cid = field.getDirectAnnotationUsage(CollectionId.class);
		return cid != null ? cid.column().name() : null;
	}

	String getCollectionIdGenerator(FieldDetails field) {
		CollectionId cid = field.getDirectAnnotationUsage(CollectionId.class);
		return cid != null && cid.generator() != null && !cid.generator().isEmpty()
				? cid.generator() : null;
	}

	// --- Collection element type ---

	String getCollectionElementType(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	// --- Collection SQL operations ---

	HbmTemplateHelper.CustomSqlInfo getCollectionSQLInsert(FieldDetails field) {
		SQLInsert si = field.getDirectAnnotationUsage(SQLInsert.class);
		return si != null ? new HbmTemplateHelper.CustomSqlInfo(si.sql(), si.callable()) : null;
	}

	HbmTemplateHelper.CustomSqlInfo getCollectionSQLUpdate(FieldDetails field) {
		SQLUpdate su = field.getDirectAnnotationUsage(SQLUpdate.class);
		return su != null ? new HbmTemplateHelper.CustomSqlInfo(su.sql(), su.callable()) : null;
	}

	HbmTemplateHelper.CustomSqlInfo getCollectionSQLDelete(FieldDetails field) {
		SQLDelete sd = field.getDirectAnnotationUsage(SQLDelete.class);
		return sd != null ? new HbmTemplateHelper.CustomSqlInfo(sd.sql(), sd.callable()) : null;
	}

	HbmTemplateHelper.CustomSqlInfo getCollectionSQLDeleteAll(FieldDetails field) {
		SQLDeleteAll sda = field.getDirectAnnotationUsage(SQLDeleteAll.class);
		return sda != null ? new HbmTemplateHelper.CustomSqlInfo(sda.sql(), sda.callable()) : null;
	}

	// --- Sort ---

	String getSort(FieldDetails field) {
		if (field.hasDirectAnnotationUsage(SortNatural.class)) {
			return "natural";
		}
		SortComparator sc = field.getDirectAnnotationUsage(SortComparator.class);
		if (sc != null) {
			return sc.value().getName();
		}
		return null;
	}

}
