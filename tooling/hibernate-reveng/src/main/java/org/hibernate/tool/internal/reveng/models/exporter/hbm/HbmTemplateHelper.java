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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Wraps a {@link ClassDetails} and provides template-friendly methods
 * for generating Hibernate hbm.xml mapping files.
 *
 * @author Koen Aers
 */
public class HbmTemplateHelper {

	private final ClassDetails classDetails;
	private final String comment;
	private final Map<String, List<String>> metaAttributes;

	HbmTemplateHelper(ClassDetails classDetails) {
		this(classDetails, null, Collections.emptyMap());
	}

	HbmTemplateHelper(ClassDetails classDetails, String comment,
					   Map<String, List<String>> metaAttributes) {
		this.classDetails = classDetails;
		this.comment = comment;
		this.metaAttributes = metaAttributes != null ? metaAttributes : Collections.emptyMap();
	}

	// --- Entity / class ---

	public String getClassName() {
		String name = classDetails.getClassName();
		return name.startsWith(".") ? name.substring(1) : name;
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

	// --- Field categorization ---

	public FieldDetails getCompositeIdField() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}

	public String getCompositeIdClassName() {
		FieldDetails cid = getCompositeIdField();
		return cid != null ? cid.getType().determineRawClass().getClassName() : null;
	}

	public List<FieldDetails> getIdFields() {
		return getFieldsWithAnnotation(Id.class);
	}

	public List<FieldDetails> getBasicFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (!isRelationshipField(field) && !isEmbeddedField(field)
					&& !field.hasDirectAnnotationUsage(EmbeddedId.class)
					&& !field.hasDirectAnnotationUsage(Id.class)
					&& !field.hasDirectAnnotationUsage(Version.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getVersionFields() {
		return getFieldsWithAnnotation(Version.class);
	}

	public List<FieldDetails> getManyToOneFields() {
		return getFieldsWithAnnotation(ManyToOne.class);
	}

	public List<FieldDetails> getOneToOneFields() {
		return getFieldsWithAnnotation(OneToOne.class);
	}

	public List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	public List<FieldDetails> getManyToManyFields() {
		return getFieldsWithAnnotation(ManyToMany.class);
	}

	public List<FieldDetails> getEmbeddedFields() {
		return getFieldsWithAnnotation(Embedded.class);
	}

	// --- Column / type attributes ---

	public String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	public String getHibernateTypeName(FieldDetails field) {
		String className = field.getType().determineRawClass().getClassName();
		return JavaClassToHibernateType.toHibernateType(className);
	}

	public String getColumnAttributes(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		StringBuilder sb = new StringBuilder();
		if (col != null && !col.nullable()) {
			sb.append("not-null=\"true\" ");
		}
		if (col != null && col.unique()) {
			sb.append("unique=\"true\" ");
		}
		if (col != null && col.length() != 255 && col.length() > 0) {
			sb.append("length=\"").append(col.length()).append("\" ");
		}
		if (col != null && col.precision() > 0) {
			sb.append("precision=\"").append(col.precision()).append("\" ");
		}
		if (col != null && col.scale() > 0) {
			sb.append("scale=\"").append(col.scale()).append("\" ");
		}
		return sb.toString().stripTrailing();
	}

	public String getGeneratorClass(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return toGeneratorClass(gv != null ? gv.strategy() : null);
	}

	// --- ManyToOne ---

	public String getTargetEntityName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public boolean isManyToOneLazy(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o != null && m2o.fetch() == jakarta.persistence.FetchType.LAZY;
	}

	public boolean isManyToOneOptional(FieldDetails field) {
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		return m2o == null || m2o.optional();
	}

	// --- JoinColumn (shared by ManyToOne, OneToOne) ---

	public String getJoinColumnName(FieldDetails field) {
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		return jc != null ? jc.name() : null;
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
		return getCascadeString(o2o.cascade());
	}

	public boolean isOneToOneConstrained(FieldDetails field) {
		return field.hasDirectAnnotationUsage(JoinColumn.class);
	}

	// --- OneToMany ---

	public String getOneToManyTargetEntity(FieldDetails field) {
		TypeDetails elementType = field.getElementType();
		return elementType != null ? elementType.determineRawClass().getClassName() : null;
	}

	public String getOneToManyMappedBy(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.mappedBy() != null && !o2m.mappedBy().isEmpty()
				? o2m.mappedBy() : null;
	}

	public String getOneToManyCascadeString(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		if (o2m == null || o2m.cascade().length == 0) {
			return null;
		}
		return getCascadeString(o2m.cascade());
	}

	public boolean isOneToManyEager(FieldDetails field) {
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
		return o2m != null && o2m.fetch() == jakarta.persistence.FetchType.EAGER;
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

	public String getJoinTableName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null ? jt.name() : null;
	}

	public String getJoinTableJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.joinColumns().length > 0 ? jt.joinColumns()[0].name() : null;
	}

	public String getJoinTableInverseJoinColumnName(FieldDetails field) {
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		return jt != null && jt.inverseJoinColumns().length > 0
				? jt.inverseJoinColumns()[0].name() : null;
	}

	public String getManyToManyCascadeString(FieldDetails field) {
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m == null || m2m.cascade().length == 0) {
			return null;
		}
		return getCascadeString(m2m.cascade());
	}

	// --- Embedded / EmbeddedId attribute overrides ---

	public String getEmbeddableClassName(FieldDetails field) {
		return field.getType().determineRawClass().getClassName();
	}

	public List<AttributeOverrideInfo> getAttributeOverrides(FieldDetails field) {
		List<AttributeOverrideInfo> result = new ArrayList<>();
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null) {
			for (AttributeOverride ao : overrides.value()) {
				result.add(new AttributeOverrideInfo(ao.name(), ao.column().name()));
			}
		}
		return result;
	}

	public record AttributeOverrideInfo(String fieldName, String columnName) {}

	// --- Meta attributes ---

	public Map<String, List<String>> getMetaAttributes() {
		return metaAttributes;
	}

	public List<String> getMetaAttribute(String name) {
		return metaAttributes.getOrDefault(name, Collections.emptyList());
	}

	// --- Utilities ---

	public String getCascadeString(CascadeType[] cascadeTypes) {
		if (cascadeTypes == null || cascadeTypes.length == 0) {
			return "none";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cascadeTypes.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(toHbmCascade(cascadeTypes[i]));
		}
		return sb.toString();
	}

	String toGeneratorClass(GenerationType generationType) {
		if (generationType == null) {
			return "assigned";
		}
		return switch (generationType) {
			case IDENTITY -> "identity";
			case SEQUENCE -> "sequence";
			case TABLE -> "table";
			case AUTO -> "native";
			case UUID -> "uuid2";
		};
	}

	// --- Private helpers ---

	private boolean isRelationshipField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(ManyToOne.class)
				|| field.hasDirectAnnotationUsage(OneToMany.class)
				|| field.hasDirectAnnotationUsage(OneToOne.class)
				|| field.hasDirectAnnotationUsage(ManyToMany.class);
	}

	private boolean isEmbeddedField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Embedded.class);
	}

	private <A extends Annotation> List<FieldDetails> getFieldsWithAnnotation(Class<A> annotationType) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(annotationType)) {
				result.add(field);
			}
		}
		return result;
	}

	private String toHbmCascade(CascadeType cascadeType) {
		return switch (cascadeType) {
			case ALL -> "all";
			case PERSIST -> "persist";
			case MERGE -> "merge";
			case REMOVE -> "delete";
			case REFRESH -> "refresh";
			case DETACH -> "evict";
		};
	}
}
