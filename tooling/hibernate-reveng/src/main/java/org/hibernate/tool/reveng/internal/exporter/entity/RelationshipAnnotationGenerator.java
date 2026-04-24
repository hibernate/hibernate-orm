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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.models.spi.FieldDetails;

public class RelationshipAnnotationGenerator {

	private final ImportContext importContext;
	private final TemplateHelper templateHelper;
	private final boolean annotated;

	RelationshipAnnotationGenerator(
			ImportContext importContext, TemplateHelper templateHelper) {
		this.importContext = importContext;
		this.templateHelper = templateHelper;
		this.annotated = templateHelper.isAnnotated();
	}

	// --- @ManyToOne ---

	public String generateManyToOneAnnotation(FieldDetails field) {
		if (!annotated) return "";
		ManyToOne m2o = templateHelper.fieldGetAnnotation(field, ManyToOne.class);
		if (m2o == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToOne");
		sb.append("@ManyToOne");
		appendManyToOneAttributes(sb, m2o);
		appendSingleJoinColumnOrColumns(sb, field);
		return sb.toString();
	}

	private void appendManyToOneAttributes(StringBuilder sb, ManyToOne m2o) {
		boolean hasAttrs = m2o.fetch() != FetchType.EAGER || !m2o.optional();
		if (!hasAttrs) {
			return;
		}
		sb.append("(");
		boolean needComma = false;
		if (m2o.fetch() != FetchType.EAGER) {
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(m2o.fetch().name());
			needComma = true;
		}
		if (!m2o.optional()) {
			if (needComma) sb.append(", ");
			sb.append("optional = false");
		}
		sb.append(")");
	}

	// --- @OneToMany ---

	public String generateOneToManyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		OneToMany o2m = templateHelper.fieldGetAnnotation(field, OneToMany.class);
		if (o2m == null) {
			return "";
		}
		importType("jakarta.persistence.OneToMany");
		StringBuilder sb = new StringBuilder("@OneToMany(");
		boolean needComma = false;
		String mappedBy = o2m.mappedBy();
		if (mappedBy != null && !mappedBy.isEmpty()) {
			sb.append("mappedBy = \"").append(mappedBy).append("\"");
			needComma = true;
		}
		if (o2m.fetch() != FetchType.LAZY) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(o2m.fetch().name());
			needComma = true;
		}
		if (o2m.cascade().length > 0) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.CascadeType");
			sb.append("cascade = ");
			appendCascade(sb, o2m.cascade());
			needComma = true;
		}
		if (o2m.orphanRemoval()) {
			if (needComma) sb.append(", ");
			sb.append("orphanRemoval = true");
		}
		sb.append(")");
		return sb.toString();
	}

	// --- @OneToOne ---

	public String generateOneToOneAnnotation(FieldDetails field) {
		if (!annotated) return "";
		OneToOne o2o = templateHelper.fieldGetAnnotation(field, OneToOne.class);
		if (o2o == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.OneToOne");
		sb.append("@OneToOne");
		appendOneToOneAttributes(sb, o2o);
		appendSingleJoinColumnOrColumns(sb, field);
		appendMapsId(sb, field);
		return sb.toString();
	}

	private void appendOneToOneAttributes(StringBuilder sb, OneToOne o2o) {
		String mappedBy = o2o.mappedBy();
		boolean hasMappedBy = mappedBy != null && !mappedBy.isEmpty();
		boolean hasAttrs = hasMappedBy
				|| o2o.fetch() != FetchType.EAGER
				|| !o2o.optional()
				|| o2o.cascade().length > 0
				|| o2o.orphanRemoval();
		if (!hasAttrs) {
			return;
		}
		sb.append("(");
		boolean needComma = false;
		if (hasMappedBy) {
			sb.append("mappedBy = \"").append(mappedBy).append("\"");
			needComma = true;
		}
		if (o2o.fetch() != FetchType.EAGER) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(o2o.fetch().name());
			needComma = true;
		}
		if (!o2o.optional()) {
			if (needComma) sb.append(", ");
			sb.append("optional = false");
			needComma = true;
		}
		if (o2o.cascade().length > 0) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.CascadeType");
			sb.append("cascade = ");
			appendCascade(sb, o2o.cascade());
			needComma = true;
		}
		if (o2o.orphanRemoval()) {
			if (needComma) sb.append(", ");
			sb.append("orphanRemoval = true");
		}
		sb.append(")");
	}

	private void appendMapsId(StringBuilder sb, FieldDetails field) {
		if (templateHelper.fieldHasAnnotation(field, MapsId.class)) {
			sb.append("\n    ");
			importType("jakarta.persistence.MapsId");
			sb.append("@MapsId");
		}
	}

	// --- @ManyToMany ---

	public String generateManyToManyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		ManyToMany m2m = templateHelper.fieldGetAnnotation(field, ManyToMany.class);
		if (m2m == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToMany");
		sb.append("@ManyToMany");
		appendManyToManyAttributes(sb, m2m);
		appendJoinTable(sb, field);
		return sb.toString();
	}

	private void appendManyToManyAttributes(StringBuilder sb, ManyToMany m2m) {
		String mappedBy = m2m.mappedBy();
		boolean hasMappedBy = mappedBy != null && !mappedBy.isEmpty();
		boolean hasAttrs = hasMappedBy
				|| m2m.fetch() != FetchType.LAZY
				|| m2m.cascade().length > 0;
		if (!hasAttrs) {
			return;
		}
		sb.append("(");
		boolean needComma = false;
		if (hasMappedBy) {
			sb.append("mappedBy = \"").append(mappedBy).append("\"");
			needComma = true;
		}
		if (m2m.fetch() != FetchType.LAZY) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(m2m.fetch().name());
			needComma = true;
		}
		if (m2m.cascade().length > 0) {
			if (needComma) sb.append(", ");
			importType("jakarta.persistence.CascadeType");
			sb.append("cascade = ");
			appendCascade(sb, m2m.cascade());
		}
		sb.append(")");
	}

	private void appendJoinTable(StringBuilder sb, FieldDetails field) {
		JoinTable jt = templateHelper.fieldGetAnnotation(field, JoinTable.class);
		if (jt == null) {
			return;
		}
		sb.append("\n    ");
		importType("jakarta.persistence.JoinTable");
		importType("jakarta.persistence.JoinColumn");
		sb.append("@JoinTable(name = \"").append(jt.name()).append("\"");
		if (jt.joinColumns().length > 0) {
			appendJoinTableColumns(sb, "joinColumns", jt.joinColumns());
		}
		if (jt.inverseJoinColumns().length > 0) {
			appendJoinTableColumns(sb, "inverseJoinColumns", jt.inverseJoinColumns());
		}
		sb.append(")");
	}

	// --- @EmbeddedId ---

	public String generateEmbeddedIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		if (!templateHelper.fieldHasAnnotation(field, EmbeddedId.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.EmbeddedId");
		sb.append("@EmbeddedId");
		appendAttributeOverrides(sb, field);
		return sb.toString();
	}

	// --- @Embedded ---

	public String generateEmbeddedAnnotation(FieldDetails field) {
		if (!annotated) return "";
		if (!templateHelper.fieldHasAnnotation(field, Embedded.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Embedded");
		sb.append("@Embedded");
		appendAttributeOverrides(sb, field);
		return sb.toString();
	}

	// --- Shared helpers ---

	private void appendSingleJoinColumnOrColumns(StringBuilder sb, FieldDetails field) {
		JoinColumns jcs = templateHelper.fieldGetAnnotation(field, JoinColumns.class);
		if (jcs != null && jcs.value().length > 0) {
			appendMultipleJoinColumns(sb, jcs.value());
			return;
		}
		JoinColumn jc = templateHelper.fieldGetAnnotation(field, JoinColumn.class);
		if (jc != null) {
			appendSingleJoinColumn(sb, jc);
		}
	}

	private void appendMultipleJoinColumns(StringBuilder sb, JoinColumn[] columns) {
		sb.append("\n    ");
		importType("jakarta.persistence.JoinColumn");
		importType("jakarta.persistence.JoinColumns");
		sb.append("@JoinColumns({");
		for (int i = 0; i < columns.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append("\n        @JoinColumn(name = \"").append(columns[i].name()).append("\"");
			appendJoinColumnAttributes(sb, columns[i]);
			sb.append(")");
		}
		sb.append("\n    })");
	}

	private void appendSingleJoinColumn(StringBuilder sb, JoinColumn jc) {
		sb.append("\n    ");
		importType("jakarta.persistence.JoinColumn");
		sb.append("@JoinColumn(name = \"").append(jc.name()).append("\"");
		appendJoinColumnAttributes(sb, jc);
		sb.append(")");
	}

	private void appendJoinColumnAttributes(StringBuilder sb, JoinColumn jc) {
		if (jc.referencedColumnName() != null && !jc.referencedColumnName().isEmpty()) {
			sb.append(", referencedColumnName = \"")
					.append(jc.referencedColumnName()).append("\"");
		}
		if (!jc.insertable()) {
			sb.append(", insertable = false");
		}
		if (!jc.updatable()) {
			sb.append(", updatable = false");
		}
	}

	private void appendJoinTableColumns(
			StringBuilder sb, String attributeName, JoinColumn[] columns) {
		if (columns.length == 1) {
			sb.append(",\n            ").append(attributeName)
					.append(" = @JoinColumn(name = \"")
					.append(columns[0].name()).append("\")");
		} else {
			sb.append(",\n            ").append(attributeName).append(" = {\n");
			for (int i = 0; i < columns.length; i++) {
				if (i > 0) sb.append(",\n");
				sb.append("                @JoinColumn(name = \"")
						.append(columns[i].name()).append("\")");
			}
			sb.append("\n            }");
		}
	}

	private void appendCascade(StringBuilder sb, CascadeType[] types) {
		if (types.length == 1) {
			sb.append("CascadeType.").append(types[0].name());
		} else {
			sb.append("{ ");
			for (int i = 0; i < types.length; i++) {
				if (i > 0) sb.append(", ");
				sb.append("CascadeType.").append(types[i].name());
			}
			sb.append(" }");
		}
	}

	private void appendAttributeOverrides(StringBuilder sb, FieldDetails field) {
		AttributeOverrides overrides = templateHelper.fieldGetAnnotation(
				field, AttributeOverrides.class);
		if (overrides == null || overrides.value().length == 0) {
			return;
		}
		sb.append("\n    ");
		importType("jakarta.persistence.AttributeOverrides");
		importType("jakarta.persistence.AttributeOverride");
		importType("jakarta.persistence.Column");
		sb.append("@AttributeOverrides({\n");
		for (int i = 0; i < overrides.value().length; i++) {
			AttributeOverride ao = overrides.value()[i];
			sb.append("        @AttributeOverride(name = \"").append(ao.name())
					.append("\", column = @Column(name = \"")
					.append(ao.column().name()).append("\"))");
			if (i < overrides.value().length - 1) {
				sb.append(",");
			}
			sb.append("\n");
		}
		sb.append("    })");
	}

	private String importType(String fqcn) {
		return importContext.importType(fqcn);
	}
}
