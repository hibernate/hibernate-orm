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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.tool.internal.export.java.ImportContext;

/**
 * Wraps a {@link ClassDetails} and provides template-friendly methods
 * for generating JPA-annotated Java entity source code.
 *
 * @author Koen Aers
 */
public class TemplateHelper {

	private final ClassDetails classDetails;
	private final ModelsContext modelsContext;
	private final ImportContext importContext;
	private final boolean annotated;
	private final Map<String, List<String>> classMetaAttributes;
	private final Map<String, Map<String, List<String>>> fieldMetaAttributes;

	TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated) {
		this(classDetails, modelsContext, importContext, annotated,
				Collections.emptyMap(), Collections.emptyMap());
	}

	TemplateHelper(ClassDetails classDetails, ModelsContext modelsContext,
				   ImportContext importContext, boolean annotated,
				   Map<String, List<String>> classMetaAttributes,
				   Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		this.classDetails = classDetails;
		this.modelsContext = modelsContext;
		this.importContext = importContext;
		this.annotated = annotated;
		this.classMetaAttributes = classMetaAttributes != null ? classMetaAttributes : Collections.emptyMap();
		this.fieldMetaAttributes = fieldMetaAttributes != null ? fieldMetaAttributes : Collections.emptyMap();
	}

	// --- Package / class ---

	public String getPackageDeclaration() {
		String pkg = getPackageName();
		if (pkg != null && !pkg.isEmpty()) {
			return "package " + pkg + ";";
		}
		return "";
	}

	public String getDeclarationName() {
		return classDetails.getName();
	}

	public String getExtendsDeclaration() {
		ClassDetails superClass = classDetails.getSuperClass();
		if (superClass != null && !"java.lang.Object".equals(superClass.getClassName())) {
			importType(superClass.getClassName());
			return "extends " + superClass.getName() + " ";
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

	// --- ClassDetails access ---

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public boolean isEmbeddable() {
		return classDetails.hasDirectAnnotationUsage(Embeddable.class);
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

	public List<FieldDetails> getBasicFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (!isRelationshipField(field) && !isEmbeddedField(field) && !isEmbeddedIdField(field)
					&& !field.hasDirectAnnotationUsage(ElementCollection.class)) {
				result.add(field);
			}
		}
		return result;
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

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	// --- Field info methods ---

	public boolean isPrimaryKey(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Id.class);
	}

	public boolean isVersion(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Version.class);
	}

	public boolean isLob(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Lob.class);
	}

	// --- Type name resolution ---

	public String getJavaTypeName(FieldDetails field) {
		return importType(field.getType().determineRawClass().getClassName());
	}

	public String getCollectionTypeName(FieldDetails field) {
		String rawClassName = field.getType().determineRawClass().getClassName();
		String simpleName = importType(rawClassName);
		TypeDetails elementType = field.getElementType();
		if (elementType != null) {
			String elementClassName = elementType.determineRawClass().getClassName();
			importType(elementClassName);
			return simpleName + "<" + elementType.determineRawClass().getName() + ">";
		}
		return simpleName + "<?>";
	}

	public String getCollectionInitializerType(FieldDetails field) {
		String rawClassName = field.getType().determineRawClass().getClassName();
		return switch (rawClassName) {
			case "java.util.List" -> importType("java.util.ArrayList");
			case "java.util.Map" -> importType("java.util.HashMap");
			case "java.util.Collection" -> importType("java.util.ArrayList");
			default -> importType("java.util.HashSet");
		};
	}

	// --- Getter/setter names ---

	public String getGetterName(String fieldName) {
		return "get" + capitalize(fieldName);
	}

	public String getGetterName(FieldDetails field) {
		String prefix = "boolean".equals(
				field.getType().determineRawClass().getClassName()) ? "is" : "get";
		return prefix + capitalize(field.getName());
	}

	public String getSetterName(String fieldName) {
		return "set" + capitalize(fieldName);
	}

	// --- Annotation generation ---

	public String generateClassAnnotations() {
		if (!annotated) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		// @Embeddable
		if (classDetails.hasDirectAnnotationUsage(Embeddable.class)) {
			importType("jakarta.persistence.Embeddable");
			sb.append("@Embeddable\n");
			return sb.toString().stripTrailing();
		}
		// @Entity
		if (classDetails.hasDirectAnnotationUsage(Entity.class)) {
			importType("jakarta.persistence.Entity");
			sb.append("@Entity\n");
		}
		// @Table
		Table table = classDetails.getDirectAnnotationUsage(Table.class);
		if (table != null) {
			sb.append(generateTableAnnotation(table));
		}
		// @Inheritance
		Inheritance inh = classDetails.getDirectAnnotationUsage(Inheritance.class);
		if (inh != null) {
			sb.append(generateInheritanceAnnotation(inh));
		}
		// @DiscriminatorValue
		DiscriminatorValue dv = classDetails.getDirectAnnotationUsage(DiscriminatorValue.class);
		if (dv != null) {
			importType("jakarta.persistence.DiscriminatorValue");
			sb.append("@DiscriminatorValue(\"").append(dv.value()).append("\")\n");
		}
		// @PrimaryKeyJoinColumn
		PrimaryKeyJoinColumn pkjc = classDetails.getDirectAnnotationUsage(PrimaryKeyJoinColumn.class);
		if (pkjc != null) {
			importType("jakarta.persistence.PrimaryKeyJoinColumn");
			sb.append("@PrimaryKeyJoinColumn(name = \"").append(pkjc.name()).append("\")\n");
		}
		// Hibernate-specific class annotations
		if (classDetails.hasDirectAnnotationUsage(Immutable.class)) {
			importType("org.hibernate.annotations.Immutable");
			sb.append("@Immutable\n");
		}
		if (classDetails.hasDirectAnnotationUsage(DynamicInsert.class)) {
			importType("org.hibernate.annotations.DynamicInsert");
			sb.append("@DynamicInsert\n");
		}
		if (classDetails.hasDirectAnnotationUsage(DynamicUpdate.class)) {
			importType("org.hibernate.annotations.DynamicUpdate");
			sb.append("@DynamicUpdate\n");
		}
		BatchSize bs = classDetails.getDirectAnnotationUsage(BatchSize.class);
		if (bs != null) {
			importType("org.hibernate.annotations.BatchSize");
			sb.append("@BatchSize(size = ").append(bs.size()).append(")\n");
		}
		Cache cache = classDetails.getDirectAnnotationUsage(Cache.class);
		if (cache != null && cache.usage() != CacheConcurrencyStrategy.NONE) {
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
		return sb.toString().stripTrailing();
	}

	private String generateTableAnnotation(Table table) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Table");
		sb.append("@Table(name = \"").append(table.name()).append("\"");
		if (table.schema() != null && !table.schema().isEmpty()) {
			sb.append(", schema = \"").append(table.schema()).append("\"");
		}
		if (table.catalog() != null && !table.catalog().isEmpty()) {
			sb.append(", catalog = \"").append(table.catalog()).append("\"");
		}
		sb.append(")\n");
		return sb.toString();
	}

	private String generateInheritanceAnnotation(Inheritance inh) {
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Inheritance");
		importType("jakarta.persistence.InheritanceType");
		sb.append("@Inheritance(strategy = InheritanceType.").append(inh.strategy().name()).append(")\n");
		DiscriminatorColumn dc = classDetails.getDirectAnnotationUsage(DiscriminatorColumn.class);
		if (dc != null) {
			importType("jakarta.persistence.DiscriminatorColumn");
			sb.append("@DiscriminatorColumn(name = \"").append(dc.name()).append("\"");
			if (dc.discriminatorType() != DiscriminatorType.STRING) {
				importType("jakarta.persistence.DiscriminatorType");
				sb.append(", discriminatorType = DiscriminatorType.").append(dc.discriminatorType().name());
			}
			if (dc.length() != 31) {
				sb.append(", length = ").append(dc.length());
			}
			sb.append(")\n");
		}
		return sb.toString();
	}

	public String generateIdAnnotations(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (field.hasDirectAnnotationUsage(Id.class)) {
			importType("jakarta.persistence.Id");
			sb.append("@Id\n");
			GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
			if (gv != null) {
				importType("jakarta.persistence.GeneratedValue");
				importType("jakarta.persistence.GenerationType");
				sb.append("    @GeneratedValue(strategy = GenerationType.")
						.append(gv.strategy().name());
				if (gv.generator() != null && !gv.generator().isEmpty()) {
					sb.append(", generator = \"").append(gv.generator()).append("\"");
				}
				sb.append(")\n");
				// @SequenceGenerator
				SequenceGenerator sg = field.getDirectAnnotationUsage(SequenceGenerator.class);
				if (sg != null) {
					importType("jakarta.persistence.SequenceGenerator");
					sb.append("    @SequenceGenerator(name = \"").append(sg.name()).append("\"");
					if (sg.sequenceName() != null && !sg.sequenceName().isEmpty()) {
						sb.append(", sequenceName = \"").append(sg.sequenceName()).append("\"");
					}
					if (sg.allocationSize() != 50) {
						sb.append(", allocationSize = ").append(sg.allocationSize());
					}
					if (sg.initialValue() != 1) {
						sb.append(", initialValue = ").append(sg.initialValue());
					}
					sb.append(")\n");
				}
				// @TableGenerator
				TableGenerator tg = field.getDirectAnnotationUsage(TableGenerator.class);
				if (tg != null) {
					importType("jakarta.persistence.TableGenerator");
					sb.append("    @TableGenerator(name = \"").append(tg.name()).append("\"");
					if (tg.table() != null && !tg.table().isEmpty()) {
						sb.append(", table = \"").append(tg.table()).append("\"");
					}
					if (tg.pkColumnName() != null && !tg.pkColumnName().isEmpty()) {
						sb.append(", pkColumnName = \"").append(tg.pkColumnName()).append("\"");
					}
					if (tg.valueColumnName() != null && !tg.valueColumnName().isEmpty()) {
						sb.append(", valueColumnName = \"").append(tg.valueColumnName()).append("\"");
					}
					if (tg.allocationSize() != 50) {
						sb.append(", allocationSize = ").append(tg.allocationSize());
					}
					sb.append(")\n");
				}
			}
		}
		return sb.toString().stripTrailing();
	}

	public String generateVersionAnnotation() {
		if (!annotated) {
			return "";
		}
		importType("jakarta.persistence.Version");
		return "@Version";
	}

	public String generateBasicAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Basic basic = field.getDirectAnnotationUsage(Basic.class);
		if (basic == null) {
			return "";
		}
		boolean hasFetch = basic.fetch() != FetchType.EAGER;
		boolean hasOptional = !basic.optional();
		if (!hasFetch && !hasOptional) {
			return "";
		}
		importType("jakarta.persistence.Basic");
		StringBuilder sb = new StringBuilder("@Basic(");
		boolean needComma = false;
		if (hasFetch) {
			importType("jakarta.persistence.FetchType");
			sb.append("fetch = FetchType.").append(basic.fetch().name());
			needComma = true;
		}
		if (hasOptional) {
			if (needComma) sb.append(", ");
			sb.append("optional = ").append(basic.optional());
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateTemporalAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Temporal temporal = field.getDirectAnnotationUsage(Temporal.class);
		if (temporal == null) {
			return "";
		}
		importType("jakarta.persistence.Temporal");
		importType("jakarta.persistence.TemporalType");
		return "@Temporal(TemporalType." + temporal.value().name() + ")";
	}

	public String generateLobAnnotation() {
		if (!annotated) {
			return "";
		}
		importType("jakarta.persistence.Lob");
		return "@Lob";
	}

	public String generateFormulaAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		if (formula == null) {
			return "";
		}
		importType("org.hibernate.annotations.Formula");
		return "@Formula(\"" + formula.value() + "\")";
	}

	public boolean hasFormula(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Formula.class);
	}

	public String generateElementCollectionAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		ElementCollection ec = field.getDirectAnnotationUsage(ElementCollection.class);
		if (ec == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ElementCollection");
		sb.append("@ElementCollection");
		if (ec.fetch() != FetchType.LAZY) {
			sb.append("(fetch = ");
			importType("jakarta.persistence.FetchType");
			sb.append("FetchType.").append(ec.fetch().name()).append(")");
		}
		// @CollectionTable
		CollectionTable ct = field.getDirectAnnotationUsage(CollectionTable.class);
		if (ct != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.CollectionTable");
			sb.append("@CollectionTable(name = \"").append(ct.name()).append("\"");
			if (ct.joinColumns() != null && ct.joinColumns().length > 0) {
				importType("jakarta.persistence.JoinColumn");
				sb.append(",\n            joinColumns = @JoinColumn(name = \"")
						.append(ct.joinColumns()[0].name()).append("\")");
			}
			sb.append(")");
		}
		// @Column for element
		Column col = field.getDirectAnnotationUsage(Column.class);
		if (col != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.Column");
			sb.append("@Column(name = \"").append(col.name()).append("\")");
		}
		return sb.toString();
	}

	public String generateNaturalIdAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		NaturalId nid = field.getDirectAnnotationUsage(NaturalId.class);
		if (nid == null) {
			return "";
		}
		importType("org.hibernate.annotations.NaturalId");
		if (nid.mutable()) {
			return "@NaturalId(mutable = true)";
		}
		return "@NaturalId";
	}

	public String generateOrderByAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		OrderBy ob = field.getDirectAnnotationUsage(OrderBy.class);
		if (ob == null) {
			return "";
		}
		importType("jakarta.persistence.OrderBy");
		if (ob.value() != null && !ob.value().isEmpty()) {
			return "@OrderBy(\"" + ob.value() + "\")";
		}
		return "@OrderBy";
	}

	public String generateOrderColumnAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		OrderColumn oc = field.getDirectAnnotationUsage(OrderColumn.class);
		if (oc == null) {
			return "";
		}
		importType("jakarta.persistence.OrderColumn");
		if (oc.name() != null && !oc.name().isEmpty()) {
			return "@OrderColumn(name = \"" + oc.name() + "\")";
		}
		return "@OrderColumn";
	}

	public String generateColumnAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Column col = field.getDirectAnnotationUsage(Column.class);
		if (col == null) {
			return "";
		}
		importType("jakarta.persistence.Column");
		StringBuilder sb = new StringBuilder("@Column(name = \"");
		sb.append(col.name()).append("\"");
		if (!col.nullable()) {
			sb.append(", nullable = false");
		}
		if (col.unique()) {
			sb.append(", unique = true");
		}
		if (col.length() != 255) {
			sb.append(", length = ").append(col.length());
		}
		if (col.precision() != 0) {
			sb.append(", precision = ").append(col.precision());
		}
		if (col.scale() != 0) {
			sb.append(", scale = ").append(col.scale());
		}
		if (!col.insertable()) {
			sb.append(", insertable = false");
		}
		if (!col.updatable()) {
			sb.append(", updatable = false");
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateManyToOneAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
		if (m2o == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToOne");
		sb.append("@ManyToOne");
		boolean hasAttrs = m2o.fetch() != FetchType.EAGER || !m2o.optional();
		if (hasAttrs) {
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
		// @JoinColumn
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.JoinColumn");
			sb.append("@JoinColumn(name = \"").append(jc.name()).append("\"");
			if (jc.referencedColumnName() != null && !jc.referencedColumnName().isEmpty()) {
				sb.append(", referencedColumnName = \"")
						.append(jc.referencedColumnName()).append("\"");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String generateOneToManyAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		OneToMany o2m = field.getDirectAnnotationUsage(OneToMany.class);
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

	public String generateOneToOneAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		OneToOne o2o = field.getDirectAnnotationUsage(OneToOne.class);
		if (o2o == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.OneToOne");
		sb.append("@OneToOne");
		String mappedBy = o2o.mappedBy();
		boolean hasMappedBy = mappedBy != null && !mappedBy.isEmpty();
		boolean hasAttrs = hasMappedBy
				|| o2o.fetch() != FetchType.EAGER
				|| !o2o.optional()
				|| o2o.cascade().length > 0
				|| o2o.orphanRemoval();
		if (hasAttrs) {
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
		// @JoinColumn for owning side
		JoinColumn jc = field.getDirectAnnotationUsage(JoinColumn.class);
		if (jc != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.JoinColumn");
			sb.append("@JoinColumn(name = \"").append(jc.name()).append("\"");
			if (jc.referencedColumnName() != null && !jc.referencedColumnName().isEmpty()) {
				sb.append(", referencedColumnName = \"")
						.append(jc.referencedColumnName()).append("\"");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String generateManyToManyAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		ManyToMany m2m = field.getDirectAnnotationUsage(ManyToMany.class);
		if (m2m == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ManyToMany");
		sb.append("@ManyToMany");
		String mappedBy = m2m.mappedBy();
		boolean hasMappedBy = mappedBy != null && !mappedBy.isEmpty();
		boolean hasAttrs = hasMappedBy
				|| m2m.fetch() != FetchType.LAZY
				|| m2m.cascade().length > 0;
		if (hasAttrs) {
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
		// @JoinTable for owning side
		JoinTable jt = field.getDirectAnnotationUsage(JoinTable.class);
		if (jt != null) {
			sb.append("\n    ");
			importType("jakarta.persistence.JoinTable");
			importType("jakarta.persistence.JoinColumn");
			sb.append("@JoinTable(name = \"").append(jt.name()).append("\"");
			if (jt.joinColumns().length > 0) {
				sb.append(",\n            joinColumns = @JoinColumn(name = \"")
						.append(jt.joinColumns()[0].name()).append("\")");
			}
			if (jt.inverseJoinColumns().length > 0) {
				sb.append(",\n            inverseJoinColumns = @JoinColumn(name = \"")
						.append(jt.inverseJoinColumns()[0].name()).append("\")");
			}
			sb.append(")");
		}
		return sb.toString();
	}

	public String generateEmbeddedIdAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (!field.hasDirectAnnotationUsage(EmbeddedId.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.EmbeddedId");
		sb.append("@EmbeddedId");
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null && overrides.value().length > 0) {
			sb.append("\n    ");
			appendAttributeOverrides(sb, overrides.value());
		}
		return sb.toString();
	}

	public String generateEmbeddedAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (!field.hasDirectAnnotationUsage(Embedded.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.Embedded");
		sb.append("@Embedded");
		AttributeOverrides overrides = field.getDirectAnnotationUsage(AttributeOverrides.class);
		if (overrides != null && overrides.value().length > 0) {
			sb.append("\n    ");
			appendAttributeOverrides(sb, overrides.value());
		}
		return sb.toString();
	}

	// --- Subclass check ---

	public boolean isSubclass() {
		ClassDetails superClass = classDetails.getSuperClass();
		return superClass != null && !"java.lang.Object".equals(superClass.getClassName());
	}

	// --- Constructor support ---

	public boolean needsFullConstructor() {
		return !getFullConstructorProperties().isEmpty();
	}

	public List<FullConstructorProperty> getFullConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Composite ID
		FieldDetails cid = getCompositeIdField();
		if (cid != null) {
			props.add(new FullConstructorProperty(getJavaTypeName(cid), cid.getName()));
		}
		// Basic fields (skip version, respect gen-property)
		for (FieldDetails field : getBasicFields()) {
			if (!isVersion(field) && isGenProperty(field)) {
				props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
			}
		}
		// ManyToOne
		for (FieldDetails field : getManyToOneFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
		}
		// OneToOne
		for (FieldDetails field : getOneToOneFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
		}
		// OneToMany
		for (FieldDetails field : getOneToManyFields()) {
			props.add(new FullConstructorProperty(getCollectionTypeName(field), field.getName()));
		}
		// ManyToMany
		for (FieldDetails field : getManyToManyFields()) {
			props.add(new FullConstructorProperty(getCollectionTypeName(field), field.getName()));
		}
		// Embedded
		for (FieldDetails field : getEmbeddedFields()) {
			props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
		}
		return props;
	}

	public boolean needsMinimalConstructor() {
		List<FullConstructorProperty> minProps = getMinimalConstructorProperties();
		List<FullConstructorProperty> fullProps = getFullConstructorProperties();
		return !minProps.isEmpty() && minProps.size() < fullProps.size();
	}

	public List<FullConstructorProperty> getMinimalConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Basic fields: non-nullable, non-version, non-generated-id, respects gen-property
		for (FieldDetails field : getBasicFields()) {
			if (isVersion(field) || !isGenProperty(field)) {
				continue;
			}
			if (isPrimaryKey(field)) {
				// Include ID only if no generator (assigned)
				GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
				if (gv != null) {
					continue;
				}
				props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
			} else {
				Column col = field.getDirectAnnotationUsage(Column.class);
				if (col != null && !col.nullable()) {
					props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
				}
			}
		}
		// ManyToOne: non-optional
		for (FieldDetails field : getManyToOneFields()) {
			ManyToOne m2o = field.getDirectAnnotationUsage(ManyToOne.class);
			if (m2o != null && !m2o.optional()) {
				props.add(new FullConstructorProperty(getJavaTypeName(field), field.getName()));
			}
		}
		return props;
	}

	public String getMinimalConstructorParameterList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
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
		List<FieldDetails> basicFields = getBasicFields();
		boolean hasExplicitToString = basicFields.stream()
				.anyMatch(f -> hasFieldMetaAttribute(f, "use-in-tostring"));
		List<ToStringProperty> props = new ArrayList<>();
		if (hasExplicitToString) {
			for (FieldDetails field : basicFields) {
				if (getFieldMetaAsBool(field, "use-in-tostring", false)) {
					props.add(new ToStringProperty(field.getName(), getGetterName(field)));
				}
			}
		} else {
			FieldDetails cid = getCompositeIdField();
			if (cid != null) {
				props.add(new ToStringProperty(cid.getName(), getGetterName(cid)));
			}
			for (FieldDetails field : basicFields) {
				if (isGenProperty(field)) {
					props.add(new ToStringProperty(field.getName(), getGetterName(field)));
				}
			}
		}
		return props;
	}

	// --- equals/hashCode support ---

	public boolean hasCompositeId() {
		return getCompositeIdField() != null;
	}

	public boolean needsEqualsHashCode() {
		if (isEmbeddable()) return true;
		if (hasNaturalId()) return true;
		boolean hasExplicitEquals = getBasicFields().stream()
				.anyMatch(f -> hasFieldMetaAttribute(f, "use-in-equals"));
		if (hasExplicitEquals) return true;
		return hasCompositeId() || !getIdentifierFields().isEmpty();
	}

	public boolean hasNaturalId() {
		return getBasicFields().stream()
				.anyMatch(f -> f.hasDirectAnnotationUsage(NaturalId.class));
	}

	public List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (field.hasDirectAnnotationUsage(NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public boolean hasExplicitEqualsColumns() {
		return getBasicFields().stream()
				.anyMatch(f -> getFieldMetaAsBool(f, "use-in-equals", false));
	}

	public List<FieldDetails> getEqualsFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (getFieldMetaAsBool(field, "use-in-equals", false)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getIdentifierFields() {
		if (isEmbeddable()) {
			return getBasicFields();
		}
		// Prefer @NaturalId fields for equals/hashCode
		List<FieldDetails> naturalIdFields = getNaturalIdFields();
		if (!naturalIdFields.isEmpty()) {
			return naturalIdFields;
		}
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : getBasicFields()) {
			if (isPrimaryKey(field)) {
				result.add(field);
			}
		}
		return result;
	}

	public String generateEqualsExpression(FieldDetails field) {
		String getter = getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		if (isPrimitiveType(typeName)) {
			return "this." + getter + " == other." + getter;
		}
		return "((this." + getter + " == other." + getter + ") || "
				+ "(this." + getter + " != null && other." + getter + " != null && "
				+ "this." + getter + ".equals(other." + getter + ")))";
	}

	public String generateHashCodeExpression(FieldDetails field) {
		String getter = "this." + getGetterName(field) + "()";
		String typeName = field.getType().determineRawClass().getClassName();
		return switch (typeName) {
			case "int", "char", "short", "byte" -> getter;
			case "boolean" -> "(" + getter + " ? 1 : 0)";
			case "long" -> "(int) " + getter;
			case "float" -> {
				importType("java.lang.Float");
				yield "Float.floatToIntBits(" + getter + ")";
			}
			case "double" -> {
				importType("java.lang.Double");
				yield "(int) Double.doubleToLongBits(" + getter + ")";
			}
			default -> "(" + getter + " == null ? 0 : " + getter + ".hashCode())";
		};
	}

	// --- Meta-attribute support ---

	public boolean hasClassMetaAttribute(String name) {
		return classMetaAttributes.containsKey(name);
	}

	public String getClassMetaAttribute(String name) {
		List<String> values = classMetaAttributes.getOrDefault(name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	public boolean hasFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		return attrs.containsKey(name);
	}

	public boolean getFieldMetaAsBool(FieldDetails field, String name, boolean defaultValue) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		if (values.isEmpty()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(values.get(0));
	}

	public String getFieldMetaAttribute(FieldDetails field, String name) {
		Map<String, List<String>> attrs = fieldMetaAttributes.getOrDefault(
				field.getName(), Collections.emptyMap());
		List<String> values = attrs.getOrDefault(name, Collections.emptyList());
		return values.isEmpty() ? "" : String.join("\n", values);
	}

	public boolean isGenProperty(FieldDetails field) {
		return getFieldMetaAsBool(field, "gen-property", true);
	}

	public boolean hasFieldDescription(FieldDetails field) {
		return hasFieldMetaAttribute(field, "field-description");
	}

	public String getFieldDescription(FieldDetails field) {
		return getFieldMetaAttribute(field, "field-description");
	}

	public boolean hasExtraClassCode() {
		return hasClassMetaAttribute("class-code");
	}

	public String getExtraClassCode() {
		return getClassMetaAttribute("class-code");
	}

	// --- Inner record types for template data ---

	public record FullConstructorProperty(String typeName, String fieldName) {}

	public record ToStringProperty(String fieldName, String getterName) {}

	// --- Private helpers ---

	private String getPackageName() {
		String className = classDetails.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	private boolean isRelationshipField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(ManyToOne.class)
				|| field.hasDirectAnnotationUsage(OneToMany.class)
				|| field.hasDirectAnnotationUsage(OneToOne.class)
				|| field.hasDirectAnnotationUsage(ManyToMany.class);
	}

	private boolean isEmbeddedField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Embedded.class);
	}

	private boolean isEmbeddedIdField(FieldDetails field) {
		return field.hasDirectAnnotationUsage(EmbeddedId.class);
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

	private boolean isPrimitiveType(String className) {
		return switch (className) {
			case "int", "long", "short", "byte", "char", "boolean", "float", "double" -> true;
			default -> false;
		};
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

	private void appendAttributeOverrides(StringBuilder sb, AttributeOverride[] overrides) {
		importType("jakarta.persistence.AttributeOverrides");
		importType("jakarta.persistence.AttributeOverride");
		importType("jakarta.persistence.Column");
		sb.append("@AttributeOverrides({\n");
		for (int i = 0; i < overrides.length; i++) {
			AttributeOverride ao = overrides[i];
			sb.append("        @AttributeOverride(name = \"").append(ao.name())
					.append("\", column = @Column(name = \"").append(ao.column().name()).append("\"))");
			if (i < overrides.length - 1) {
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
