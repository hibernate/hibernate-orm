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
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Cascade;

import org.hibernate.tool.reveng.internal.util.CascadeUtil;

import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Handles per-field HBM association attributes for template generation:
 * many-to-one, one-to-one, one-to-many, many-to-many, join columns,
 * join tables, attribute overrides, and array/many-to-any meta helpers.
 *
 * @author Koen Aers
 */
public class HbmAssociationAttributeHelper {

	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;
	private final Map<String, Map<String, List<String>>> allClassMetaAttributes;

	HbmAssociationAttributeHelper(Map<String, Map<String, List<String>>> fieldMetaAttributes,
								   Map<String, Map<String, List<String>>> allClassMetaAttributes) {
		this.fieldMetaAttributes = fieldMetaAttributes;
		this.allClassMetaAttributes = allClassMetaAttributes;
	}

	private Map<String, List<String>> getFieldMetaAttributeMap(FieldDetails field) {
		return FieldMetaUtil.forField(fieldMetaAttributes, field);
	}

	private List<String> getFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.getOrDefault(name, Collections.emptyList());
	}

	// --- ManyToOne ---

	public boolean isManyToOneEntityNameRef(FieldDetails field) {
		String targetClassName = field.getType().determineRawClass().getClassName();
		Map<String, List<String>> targetMeta = allClassMetaAttributes.get(targetClassName);
		if (targetMeta != null) {
			List<String> realClass = targetMeta.get("hibernate.class-name");
			if (realClass != null && !realClass.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public String getManyToOneEntityName(FieldDetails field) {
		if (!isManyToOneEntityNameRef(field)) {
			return null;
		}
		String identityName = field.getType().determineRawClass().getClassName();
		int dot = identityName.lastIndexOf('.');
		return dot >= 0 ? identityName.substring(dot + 1) : identityName;
	}

	public List<String> getManyToOneFormulas(FieldDetails field) {
		List<String> formulas = getFieldMetaAttribute(field, "hibernate.formula");
		return formulas != null ? formulas : Collections.emptyList();
	}

	public boolean isManyToOneLazy(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() == FetchType.LAZY;
	}

	public boolean isManyToOneUpdatable(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) return jc.updatable();
		JoinColumns jcs = field.getDirectAnnotationUsage(JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) return jcs.value()[0].updatable();
		return true;
	}

	public boolean isManyToOneInsertable(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) return jc.insertable();
		JoinColumns jcs = field.getDirectAnnotationUsage(JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) return jcs.value()[0].insertable();
		return true;
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	public String getManyToOneCascadeString(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		if (m2o != null && m2o.cascade().length > 0) {
			return CascadeUtil.formatJpaCascade(m2o.cascade());
		}
		Cascade cascade = field.getDirectAnnotationUsage(Cascade.class);
		if (cascade != null && cascade.value().length > 0) {
			return CascadeUtil.formatHibernateCascade(cascade);
		}
		return null;
	}

	// --- JoinColumn (shared by ManyToOne, OneToOne) ---

	public String getPropertyRef(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null && jc.referencedColumnName() != null
				&& !jc.referencedColumnName().isEmpty()
				? jc.referencedColumnName() : null;
	}

	public String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
	}

	public List<String> getJoinColumnNames(FieldDetails field) {
		List<String> result = new ArrayList<>();
		JoinColumn single = field.getDirectAnnotationUsage(JoinColumn.class);
		if (single != null) {
			result.add(single.name());
		}
		JoinColumns container = field.getDirectAnnotationUsage(JoinColumns.class);
		if (container != null) {
			for (JoinColumn jc : container.value()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	// --- OneToOne ---

	public String getOneToOneMappedBy(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		return o2o != null && o2o.mappedBy() != null && !o2o.mappedBy().isEmpty()
				? o2o.mappedBy() : null;
	}

	public String getOneToOneCascadeString(FieldDetails field) {
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		if (o2o == null || o2o.cascade().length == 0) {
			return null;
		}
		return CascadeUtil.formatJpaCascade(o2o.cascade());
	}

	public boolean isOneToOneConstrained(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinColumn.class)
				|| field.hasDirectAnnotationUsage(JoinColumns.class);
	}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public List<String> getKeyColumnNames(FieldDetails field) {
		JoinColumns jcs = field.getDirectAnnotationUsage(JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) {
			List<String> names = new ArrayList<>();
			for (JoinColumn jc : jcs.value()) {
				names.add(jc.name());
			}
			return names;
		}
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) {
			return Collections.singletonList(jc.name());
		}
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m != null && o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()) {
			return Collections.singletonList(o2m.mappedBy());
		}
		return Collections.emptyList();
	}

	public String getOneToManyCascadeString(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m == null || o2m.cascade().length == 0) {
			return null;
		}
		return CascadeUtil.formatJpaCascade(o2m.cascade());
	}

	public boolean isOneToManyEager(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.fetch() == FetchType.EAGER;
	}

	// --- ManyToMany ---

	public String getManyToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public boolean isManyToManyInverse(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		return m2m != null && m2m.mappedBy() != null && !m2m.mappedBy().isEmpty();
	}

	public boolean isManyToManyEntityNameRef(FieldDetails field) {
		String targetClassName = getManyToManyTargetEntity(field);
		Map<String, List<String>> targetMeta = allClassMetaAttributes.get(targetClassName);
		if (targetMeta != null) {
			List<String> realClass = targetMeta.get("hibernate.class-name");
			if (realClass != null && !realClass.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public String getManyToManyEntityName(FieldDetails field) {
		if (!isManyToManyEntityNameRef(field)) {
			return null;
		}
		String identityName = getManyToManyTargetEntity(field);
		int dot = identityName.lastIndexOf('.');
		return dot >= 0 ? identityName.substring(dot + 1) : identityName;
	}

	public List<String> getManyToManyFormulas(FieldDetails field) {
		List<String> formulas = getFieldMetaAttribute(field, "hibernate.formula");
		return formulas != null ? formulas : Collections.emptyList();
	}

	public String getManyToManyCascadeString(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m == null || m2m.cascade().length == 0) {
			return null;
		}
		return CascadeUtil.formatJpaCascade(m2m.cascade());
	}

	// --- JoinTable ---

	public String getJoinTableName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		if (jt == null) return null;
		String name = jt.name();
		return (name != null && !name.isEmpty()) ? name : null;
	}

	public boolean hasJoinTable(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinTable.class);
	}

	public String getJoinTableSchema(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.schema() != null && !jt.schema().isEmpty()
				? jt.schema() : null;
	}

	public String getJoinTableCatalog(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.catalog() != null && !jt.catalog().isEmpty()
				? jt.catalog() : null;
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.joinColumns().length > 0 ? jt.joinColumns()[0].name() : null;
	}

	public List<String> getJoinTableJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.joinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.inverseJoinColumns().length > 0
				? jt.inverseJoinColumns()[0].name() : null;
	}

	public List<String> getJoinTableInverseJoinColumnNames(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		List<String> result = new ArrayList<>();
		if (jt != null) {
			for (JoinColumn jc : jt.inverseJoinColumns()) {
				result.add(jc.name());
			}
		}
		return result;
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	public List<HbmTemplateHelper.AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		List<HbmTemplateHelper.AttributeOverrideInfo> result = new ArrayList<>();
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null) {
			for (AttributeOverride ao : overrides.value()) {
				result.add(new HbmTemplateHelper.AttributeOverrideInfo(ao.name(), ao.column().name()));
			}
		}
		return result;
	}

	// --- Array / ManyToAny meta helpers ---

	public String getArrayElementClass(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> ec = fieldMeta.get("hibernate.array.element-class");
		return (ec != null && !ec.isEmpty()) ? ec.get(0) : null;
	}

	public String getManyToAnyFkColumnName(FieldDetails field) {
		Map<String, List<String>> fieldMeta = getFieldMetaAttributeMap(field);
		List<String> fkCol = fieldMeta.get("hibernate.any.fk.column");
		return (fkCol != null && !fkCol.isEmpty()) ? fkCol.get(0) : null;
	}
}
