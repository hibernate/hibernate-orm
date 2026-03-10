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
package org.hibernate.tool.internal.reveng.models.exporter.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;

import org.hibernate.tool.internal.export.java.ImportContext;
import org.hibernate.tool.internal.reveng.models.metadata.AttributeOverrideMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Wraps a {@link TableMetadata} and provides template-friendly methods
 * for generating JPA-annotated Java entity source code.
 *
 * @author Koen Aers
 */
public class EntityTemplateHelper {

	private final TableMetadata table;
	private final ImportContext importContext;

	EntityTemplateHelper(TableMetadata table, ImportContext importContext) {
		this.table = table;
		this.importContext = importContext;
	}

	// --- Package / class ---

	public String getPackageDeclaration() {
		String pkg = table.getEntityPackage();
		if (pkg != null && !pkg.isEmpty()) {
			return "package " + pkg + ";";
		}
		return "";
	}

	public String getDeclarationName() {
		return table.getEntityClassName();
	}

	public String getExtendsDeclaration() {
		String parent = table.getParentEntityClassName();
		if (parent != null && !parent.isEmpty()) {
			String parentPkg = table.getParentEntityPackage();
			if (parentPkg != null && !parentPkg.isEmpty()) {
				importType(parentPkg + "." + parent);
			}
			return "extends " + parent + " ";
		}
		return "";
	}

	public String getImplementsDeclaration() {
		importType(Serializable.class.getName());
		return "implements Serializable";
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	// --- Import delegation ---

	public String importType(String fqcn) {
		return importContext.importType(fqcn);
	}

	// --- Table metadata access ---

	public TableMetadata getTable() {
		return table;
	}

	// --- Type name resolution ---

	public String getJavaTypeName(ColumnMetadata col) {
		return importType(col.getJavaType().getName());
	}

	public String getFieldTypeName(ForeignKeyMetadata fk) {
		String fqcn = fk.getTargetEntityPackage() + "." + fk.getTargetEntityClassName();
		return importType(fqcn);
	}

	public String getFieldTypeName(OneToOneMetadata o2o) {
		String fqcn = o2o.getTargetEntityPackage() + "." + o2o.getTargetEntityClassName();
		return importType(fqcn);
	}

	public String getCollectionTypeName(OneToManyMetadata o2m) {
		importType("java.util.Set");
		String elementFqcn = o2m.getElementEntityPackage() + "." + o2m.getElementEntityClassName();
		importType(elementFqcn);
		return "Set<" + o2m.getElementEntityClassName() + ">";
	}

	public String getCollectionTypeName(ManyToManyMetadata m2m) {
		importType("java.util.Set");
		String targetFqcn = m2m.getTargetEntityPackage() + "." + m2m.getTargetEntityClassName();
		importType(targetFqcn);
		return "Set<" + m2m.getTargetEntityClassName() + ">";
	}

	public String getEmbeddedTypeName(EmbeddedFieldMetadata e) {
		String fqcn = e.getEmbeddablePackage() + "." + e.getEmbeddableClassName();
		return importType(fqcn);
	}

	public String getCompositeIdTypeName(CompositeIdMetadata c) {
		String fqcn = c.getIdClassPackage() + "." + c.getIdClassName();
		return importType(fqcn);
	}

	// --- Getter/setter names ---

	public String getGetterName(String fieldName) {
		return "get" + capitalize(fieldName);
	}

	public String getSetterName(String fieldName) {
		return "set" + capitalize(fieldName);
	}

	// --- Annotation generation ---

	public String generateClassAnnotations() {
		StringBuilder sb = new StringBuilder();
		// @Entity
		sb.append(importType("jakarta.persistence.Entity")).append("\n");
		sb.append("@Entity\n");
		// @Table
		sb.append(generateTableAnnotation());
		// Inheritance
		InheritanceMetadata inh = table.getInheritance();
		if (inh != null) {
			sb.append(generateInheritanceAnnotation(inh));
		}
		// Discriminator value for subclass
		String discVal = table.getDiscriminatorValue();
		if (discVal != null) {
			importType("jakarta.persistence.DiscriminatorValue");
			sb.append("@DiscriminatorValue(\"").append(discVal).append("\")\n");
		}
		// PrimaryKeyJoinColumn for JOINED subclass
		String pkjc = table.getPrimaryKeyJoinColumnName();
		if (pkjc != null) {
			importType("jakarta.persistence.PrimaryKeyJoinColumn");
			sb.append("@PrimaryKeyJoinColumn(name = \"").append(pkjc).append("\")\n");
		}
		return sb.toString().stripTrailing();
	}

	private String generateTableAnnotation() {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Table");
		sb.append("@Table(name = \"").append(table.getTableName()).append("\"");
		if (table.getSchema() != null && !table.getSchema().isEmpty()) {
			sb.append(", schema = \"").append(table.getSchema()).append("\"");
		}
		if (table.getCatalog() != null && !table.getCatalog().isEmpty()) {
			sb.append(", catalog = \"").append(table.getCatalog()).append("\"");
		}
		sb.append(")\n");
		return sb.toString();
	}

	private String generateInheritanceAnnotation(InheritanceMetadata inh) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Inheritance");
		importType("jakarta.persistence.InheritanceType");
		sb.append("@Inheritance(strategy = InheritanceType.").append(inh.getStrategy().name()).append(")\n");
		if (inh.getDiscriminatorColumnName() != null) {
			importType("jakarta.persistence.DiscriminatorColumn");
			sb.append("@DiscriminatorColumn(name = \"").append(inh.getDiscriminatorColumnName()).append("\"");
			if (inh.getDiscriminatorType() != null) {
				importType("jakarta.persistence.DiscriminatorType");
				sb.append(", discriminatorType = DiscriminatorType.").append(inh.getDiscriminatorType().name());
			}
			if (inh.getDiscriminatorColumnLength() > 0) {
				sb.append(", length = ").append(inh.getDiscriminatorColumnLength());
			}
			sb.append(")\n");
		}
		return sb.toString();
	}

	public String generateIdAnnotations(ColumnMetadata col) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Id");
		sb.append("@Id\n");
		GenerationType gen = col.getGenerationType();
		if (gen != null) {
			importType("jakarta.persistence.GeneratedValue");
			importType("jakarta.persistence.GenerationType");
			sb.append("    @GeneratedValue(strategy = GenerationType.").append(gen.name()).append(")\n");
		}
		return sb.toString().stripTrailing();
	}

	public String generateVersionAnnotation() {
		importType("jakarta.persistence.Version");
		return "@Version";
	}

	public String generateBasicAnnotation(ColumnMetadata col) {
		FetchType fetch = col.getBasicFetchType();
		boolean optionalSet = col.isBasicOptionalSet();
		if (fetch == null && !optionalSet) {
			return "";
		}
		importType("jakarta.persistence.Basic");
		StringBuilder sb = new StringBuilder("@Basic(");
		boolean needComma = false;
		if (fetch != null) {
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(fetch.name());
			needComma = true;
		}
		if (optionalSet) {
			if (needComma) sb.append(", ");
			sb.append("optional = ").append(col.isBasicOptional());
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateTemporalAnnotation(ColumnMetadata col) {
		TemporalType tt = col.getTemporalType();
		if (tt == null) {
			return "";
		}
		importType("jakarta.persistence.Temporal");
		importType("jakarta.persistence.TemporalType");
		return "@Temporal(TemporalType." + tt.name() + ")";
	}

	public String generateLobAnnotation() {
		importType("jakarta.persistence.Lob");
		return "@Lob";
	}

	public String generateColumnAnnotation(ColumnMetadata col) {
		importType("jakarta.persistence.Column");
		StringBuilder sb = new StringBuilder("@Column(name = \"");
		sb.append(col.getColumnName()).append("\"");
		if (!col.isNullable()) {
			sb.append(", nullable = false");
		}
		if (col.isUnique()) {
			sb.append(", unique = true");
		}
		if (col.getLength() > 0) {
			sb.append(", length = ").append(col.getLength());
		}
		if (col.getPrecision() > 0) {
			sb.append(", precision = ").append(col.getPrecision());
		}
		if (col.getScale() > 0) {
			sb.append(", scale = ").append(col.getScale());
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateManyToOneAnnotation(ForeignKeyMetadata fk) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToOne");
		sb.append("@ManyToOne");
		boolean hasAttrs = fk.getFetchType() != null || !fk.isOptional();
		if (hasAttrs) {
			sb.append("(");
			boolean needComma = false;
			if (fk.getFetchType() != null) {
				importType("jakarta.persistence.FetchType");
				sb.append("fetch = FetchType.").append(fk.getFetchType().name());
				needComma = true;
			}
			if (!fk.isOptional()) {
				if (needComma) sb.append(", ");
				sb.append("optional = false");
			}
			sb.append(")");
		}
		sb.append("\n    ");
		// @JoinColumn
		importType("jakarta.persistence.JoinColumn");
		sb.append("@JoinColumn(name = \"").append(fk.getForeignKeyColumnName()).append("\"");
		if (fk.getReferencedColumnName() != null) {
			sb.append(", referencedColumnName = \"").append(fk.getReferencedColumnName()).append("\"");
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateOneToManyAnnotation(OneToManyMetadata o2m) {
		importType("jakarta.persistence.OneToMany");
		StringBuilder sb = new StringBuilder("@OneToMany(mappedBy = \"");
		sb.append(o2m.getMappedBy()).append("\"");
		if (o2m.getFetchType() != null) {
			importType("jakarta.persistence.FetchType");
			sb.append(", fetch = FetchType.").append(o2m.getFetchType().name());
		}
		if (o2m.getCascadeTypes() != null && o2m.getCascadeTypes().length > 0) {
			importType("jakarta.persistence.CascadeType");
			sb.append(", cascade = ");
			appendCascade(sb, o2m.getCascadeTypes());
		}
		if (o2m.isOrphanRemoval()) {
			sb.append(", orphanRemoval = true");
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateOneToOneAnnotation(OneToOneMetadata o2o) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.OneToOne");
		sb.append("@OneToOne");
		boolean hasAttrs = o2o.getMappedBy() != null
				|| o2o.getFetchType() != null
				|| !o2o.isOptional()
				|| (o2o.getCascadeTypes() != null && o2o.getCascadeTypes().length > 0)
				|| o2o.isOrphanRemoval();
		if (hasAttrs) {
			sb.append("(");
			boolean needComma = false;
			if (o2o.getMappedBy() != null) {
				sb.append("mappedBy = \"").append(o2o.getMappedBy()).append("\"");
				needComma = true;
			}
			if (o2o.getFetchType() != null) {
				if (needComma) sb.append(", ");
				importType("jakarta.persistence.FetchType");
				sb.append("fetch = FetchType.").append(o2o.getFetchType().name());
				needComma = true;
			}
			if (!o2o.isOptional()) {
				if (needComma) sb.append(", ");
				sb.append("optional = false");
				needComma = true;
			}
			if (o2o.getCascadeTypes() != null && o2o.getCascadeTypes().length > 0) {
				if (needComma) sb.append(", ");
				importType("jakarta.persistence.CascadeType");
				sb.append("cascade = ");
				appendCascade(sb, o2o.getCascadeTypes());
				needComma = true;
			}
			if (o2o.isOrphanRemoval()) {
				if (needComma) sb.append(", ");
				sb.append("orphanRemoval = true");
			}
			sb.append(")");
		}
		// @JoinColumn for owning side
		if (o2o.getForeignKeyColumnName() != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.JoinColumn");
			sb.append("@JoinColumn(name = \"").append(o2o.getForeignKeyColumnName()).append("\"");
			if (o2o.getReferencedColumnName() != null) {
				sb.append(", referencedColumnName = \"").append(o2o.getReferencedColumnName()).append("\"");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String generateManyToManyAnnotation(ManyToManyMetadata m2m) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToMany");
		sb.append("@ManyToMany");
		boolean hasAttrs = m2m.getMappedBy() != null
				|| m2m.getFetchType() != null
				|| (m2m.getCascadeTypes() != null && m2m.getCascadeTypes().length > 0);
		if (hasAttrs) {
			sb.append("(");
			boolean needComma = false;
			if (m2m.getMappedBy() != null) {
				sb.append("mappedBy = \"").append(m2m.getMappedBy()).append("\"");
				needComma = true;
			}
			if (m2m.getFetchType() != null) {
				if (needComma) sb.append(", ");
				importType("jakarta.persistence.FetchType");
				sb.append("fetch = FetchType.").append(m2m.getFetchType().name());
				needComma = true;
			}
			if (m2m.getCascadeTypes() != null && m2m.getCascadeTypes().length > 0) {
				if (needComma) sb.append(", ");
				importType("jakarta.persistence.CascadeType");
				sb.append("cascade = ");
				appendCascade(sb, m2m.getCascadeTypes());
			}
			sb.append(")");
		}
		// @JoinTable for owning side
		if (m2m.getJoinTableName() != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.JoinTable");
			importType("jakarta.persistence.JoinColumn");
			sb.append("@JoinTable(name = \"").append(m2m.getJoinTableName()).append("\"");
			if (m2m.getJoinColumnName() != null) {
				sb.append(",\n            joinColumns = @JoinColumn(name = \"").append(m2m.getJoinColumnName()).append("\")");
			}
			if (m2m.getInverseJoinColumnName() != null) {
				sb.append(",\n            inverseJoinColumns = @JoinColumn(name = \"").append(m2m.getInverseJoinColumnName()).append("\")");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String generateEmbeddedIdAnnotation(CompositeIdMetadata cid) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.EmbeddedId");
		sb.append("@EmbeddedId");
		List<AttributeOverrideMetadata> overrides = cid.getAttributeOverrides();
		if (!overrides.isEmpty()) {
			sb.append("\n    ");
			appendAttributeOverrides(sb, overrides);
		}
		return sb.toString();
	}

	public String generateEmbeddedAnnotation(EmbeddedFieldMetadata emb) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Embedded");
		sb.append("@Embedded");
		List<AttributeOverrideMetadata> overrides = emb.getAttributeOverrides();
		if (!overrides.isEmpty()) {
			sb.append("\n    ");
			appendAttributeOverrides(sb, overrides);
		}
		return sb.toString();
	}

	// --- Subclass check ---

	public boolean isSubclass() {
		return table.getParentEntityClassName() != null;
	}

	// --- Constructor support ---

	public boolean needsFullConstructor() {
		return !getFullConstructorProperties().isEmpty();
	}

	public List<FullConstructorProperty> getFullConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Composite ID
		CompositeIdMetadata cid = table.getCompositeId();
		if (cid != null) {
			props.add(new FullConstructorProperty(
					getCompositeIdTypeName(cid), cid.getFieldName()));
		}
		// Basic columns (skip FK columns, skip version)
		for (ColumnMetadata col : table.getColumns()) {
			if (!isForeignKeyColumn(col.getColumnName()) && !col.isVersion()) {
				props.add(new FullConstructorProperty(
						getJavaTypeName(col), col.getFieldName()));
			}
		}
		// ManyToOne
		for (ForeignKeyMetadata fk : table.getForeignKeys()) {
			props.add(new FullConstructorProperty(
					getFieldTypeName(fk), fk.getFieldName()));
		}
		// OneToOne
		for (OneToOneMetadata o2o : table.getOneToOnes()) {
			props.add(new FullConstructorProperty(
					getFieldTypeName(o2o), o2o.getFieldName()));
		}
		// OneToMany
		for (OneToManyMetadata o2m : table.getOneToManys()) {
			props.add(new FullConstructorProperty(
					getCollectionTypeName(o2m), o2m.getFieldName()));
		}
		// ManyToMany
		for (ManyToManyMetadata m2m : table.getManyToManys()) {
			props.add(new FullConstructorProperty(
					getCollectionTypeName(m2m), m2m.getFieldName()));
		}
		// Embedded
		for (EmbeddedFieldMetadata emb : table.getEmbeddedFields()) {
			props.add(new FullConstructorProperty(
					getEmbeddedTypeName(emb), emb.getFieldName()));
		}
		return props;
	}

	public String getFullConstructorParameterList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	// --- toString support ---

	public List<ToStringProperty> getToStringProperties() {
		List<ToStringProperty> props = new ArrayList<>();
		// Composite ID
		CompositeIdMetadata cid = table.getCompositeId();
		if (cid != null) {
			props.add(new ToStringProperty(cid.getFieldName(), getGetterName(cid.getFieldName())));
		}
		// Basic columns (skip FK columns)
		for (ColumnMetadata col : table.getColumns()) {
			if (!isForeignKeyColumn(col.getColumnName())) {
				props.add(new ToStringProperty(col.getFieldName(), getGetterName(col.getFieldName())));
			}
		}
		return props;
	}

	// --- equals/hashCode support ---

	public boolean hasCompositeId() {
		return table.getCompositeId() != null;
	}

	public List<ColumnMetadata> getIdentifierColumns() {
		List<ColumnMetadata> result = new ArrayList<>();
		for (ColumnMetadata col : table.getColumns()) {
			if (col.isPrimaryKey()) {
				result.add(col);
			}
		}
		return result;
	}

	public String generateEqualsExpression(ColumnMetadata col) {
		String getter = getGetterName(col.getFieldName()) + "()";
		Class<?> type = col.getJavaType();
		if (type.isPrimitive()) {
			return "this." + getter + " == other." + getter;
		}
		return "((this." + getter + " == other." + getter + ") || "
				+ "(this." + getter + " != null && other." + getter + " != null && "
				+ "this." + getter + ".equals(other." + getter + ")))";
	}

	public String generateHashCodeExpression(ColumnMetadata col) {
		String getter = "this." + getGetterName(col.getFieldName()) + "()";
		Class<?> type = col.getJavaType();
		if (type == int.class || type == char.class || type == short.class || type == byte.class) {
			return getter;
		}
		if (type == boolean.class) {
			return "(" + getter + " ? 1 : 0)";
		}
		if (type == long.class) {
			return "(int) " + getter;
		}
		if (type == float.class) {
			importType("java.lang.Float");
			return "Float.floatToIntBits(" + getter + ")";
		}
		if (type == double.class) {
			importType("java.lang.Double");
			return "(int) Double.doubleToLongBits(" + getter + ")";
		}
		return "(" + getter + " == null ? 0 : " + getter + ".hashCode())";
	}

	// --- Utility ---

	public boolean isForeignKeyColumn(String columnName) {
		return table.isForeignKeyColumn(columnName);
	}

	// --- Inner record types for template data ---

	public record FullConstructorProperty(String typeName, String fieldName) {}

	public record ToStringProperty(String fieldName, String getterName) {}

	// --- Private helpers ---

	private void appendCascade(StringBuilder sb, jakarta.persistence.CascadeType[] types) {
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

	private void appendAttributeOverrides(StringBuilder sb, List<AttributeOverrideMetadata> overrides) {
		importType("jakarta.persistence.AttributeOverrides");
		importType("jakarta.persistence.AttributeOverride");
		importType("jakarta.persistence.Column");
		sb.append("@AttributeOverrides({\n");
		for (int i = 0; i < overrides.size(); i++) {
			AttributeOverrideMetadata ao = overrides.get(i);
			sb.append("        @AttributeOverride(name = \"").append(ao.getFieldName())
					.append("\", column = @Column(name = \"").append(ao.getColumnName()).append("\"))");
			if (i < overrides.size() - 1) {
				sb.append(",");
			}
			sb.append("\n");
		}
		sb.append("    })");
	}

	private static String capitalize(String name) {
		if (name == null || name.isEmpty()) return name;
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
