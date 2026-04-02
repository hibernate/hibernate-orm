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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLDeletes;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLInserts;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SQLUpdates;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Subselect;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MethodDetails;
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
		// Process extra-import meta-attribute
		List<String> extraImports = this.classMetaAttributes.getOrDefault("extra-import", Collections.emptyList());
		for (String fqcn : extraImports) {
			importType(fqcn);
		}
	}

	// --- Package / class ---

	public String getPackageDeclaration() {
		String pkg = getGeneratedPackageName();
		if (pkg != null && !pkg.isEmpty()) {
			return "package " + pkg + ";";
		}
		return "";
	}

	public String getDeclarationName() {
		if (hasClassMetaAttribute("generated-class")) {
			String generatedClass = getClassMetaAttribute("generated-class");
			int lastDot = generatedClass.lastIndexOf('.');
			return lastDot > 0 ? generatedClass.substring(lastDot + 1) : generatedClass;
		}
		return classDetails.getName();
	}

	public String getExtendsDeclaration() {
		ClassDetails superClass = classDetails.getSuperClass();
		if (superClass != null && !"java.lang.Object".equals(superClass.getClassName())) {
			importType(superClass.getClassName());
			return "extends " + superClass.getName() + " ";
		}
		if (hasClassMetaAttribute("extends")) {
			String extendsFqcn = getClassMetaAttribute("extends");
			String simpleName = importType(extendsFqcn);
			return "extends " + simpleName + " ";
		}
		return "";
	}

	public String getImplementsDeclaration() {
		importType(Serializable.class.getName());
		List<String> interfaces = new ArrayList<>();
		if (hasClassMetaAttribute("implements")) {
			for (String fqcn : classMetaAttributes.get("implements")) {
				String simpleName = importType(fqcn);
				interfaces.add(simpleName);
			}
		}
		interfaces.add("Serializable");
		return "implements " + String.join(", ", interfaces);
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

	public List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
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
		if (hasFieldMetaAttribute(field, "property-type")) {
			return importType(getFieldMetaAttribute(field, "property-type"));
		}
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
		// @SecondaryTable(s)
		for (SecondaryTableInfo st : getSecondaryTables()) {
			importType("jakarta.persistence.SecondaryTable");
			sb.append("@SecondaryTable(name = \"").append(st.tableName()).append("\"");
			if (!st.keyColumns().isEmpty()) {
				importType("jakarta.persistence.PrimaryKeyJoinColumn");
				if (st.keyColumns().size() == 1) {
					sb.append(", pkJoinColumns = @PrimaryKeyJoinColumn(name = \"")
							.append(st.keyColumns().get(0)).append("\")");
				} else {
					sb.append(", pkJoinColumns = {");
					for (int i = 0; i < st.keyColumns().size(); i++) {
						if (i > 0) sb.append(", ");
						sb.append("@PrimaryKeyJoinColumn(name = \"")
								.append(st.keyColumns().get(i)).append("\")");
					}
					sb.append("}");
				}
			}
			sb.append(")\n");
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
		// @OptimisticLocking
		OptimisticLocking ol = classDetails.getDirectAnnotationUsage(OptimisticLocking.class);
		if (ol != null && ol.type() != OptimisticLockType.VERSION) {
			importType("org.hibernate.annotations.OptimisticLocking");
			importType("org.hibernate.annotations.OptimisticLockType");
			sb.append("@OptimisticLocking(type = OptimisticLockType.").append(ol.type().name()).append(")\n");
		}
		// @RowId
		RowId rowId = classDetails.getDirectAnnotationUsage(RowId.class);
		if (rowId != null && rowId.value() != null && !rowId.value().isEmpty()) {
			importType("org.hibernate.annotations.RowId");
			sb.append("@RowId(\"").append(rowId.value()).append("\")\n");
		}
		// @Subselect
		Subselect subselect = classDetails.getDirectAnnotationUsage(Subselect.class);
		if (subselect != null) {
			importType("org.hibernate.annotations.Subselect");
			sb.append("@Subselect(\"").append(subselect.value()).append("\")\n");
		}
		// @ConcreteProxy
		if (classDetails.hasDirectAnnotationUsage(ConcreteProxy.class)) {
			importType("org.hibernate.annotations.ConcreteProxy");
			sb.append("@ConcreteProxy\n");
		}
		// @SQLRestriction
		SQLRestriction sqlRestriction = classDetails.getDirectAnnotationUsage(SQLRestriction.class);
		if (sqlRestriction != null) {
			importType("org.hibernate.annotations.SQLRestriction");
			sb.append("@SQLRestriction(\"").append(sqlRestriction.value()).append("\")\n");
		}
		// @Access (class-level)
		Access classAccess = classDetails.getDirectAnnotationUsage(Access.class);
		if (classAccess != null && classAccess.value() != AccessType.FIELD) {
			importType("jakarta.persistence.Access");
			importType("jakarta.persistence.AccessType");
			sb.append("@Access(AccessType.").append(classAccess.value().name()).append(")\n");
		}
		// @NamedQuery / @NamedNativeQuery
		for (NamedQueryInfo nq : getNamedQueries()) {
			importType("jakarta.persistence.NamedQuery");
			sb.append("@NamedQuery(name = \"").append(nq.name())
					.append("\", query = \"").append(nq.query()).append("\")\n");
		}
		for (SqlResultSetMappingInfo mapping : getSqlResultSetMappings()) {
			importType("jakarta.persistence.SqlResultSetMapping");
			sb.append("@SqlResultSetMapping(name = \"").append(mapping.name()).append("\"");
			if (!mapping.entityResults().isEmpty()) {
				importType("jakarta.persistence.EntityResult");
				sb.append(", entities = {");
				for (int i = 0; i < mapping.entityResults().size(); i++) {
					if (i > 0) sb.append(", ");
					EntityResultInfo er = mapping.entityResults().get(i);
					String simpleEntityClass = importType(er.entityClass());
					sb.append("@EntityResult(entityClass = ").append(simpleEntityClass).append(".class");
					if (er.discriminatorColumn() != null) {
						sb.append(", discriminatorColumn = \"").append(er.discriminatorColumn()).append("\"");
					}
					if (!er.fieldResults().isEmpty()) {
						importType("jakarta.persistence.FieldResult");
						sb.append(", fields = {");
						for (int j = 0; j < er.fieldResults().size(); j++) {
							if (j > 0) sb.append(", ");
							FieldResultInfo fr = er.fieldResults().get(j);
							sb.append("@FieldResult(name = \"").append(fr.name())
									.append("\", column = \"").append(fr.column()).append("\")");
						}
						sb.append("}");
					}
					sb.append(")");
				}
				sb.append("}");
			}
			if (!mapping.columnResults().isEmpty()) {
				importType("jakarta.persistence.ColumnResult");
				sb.append(", columns = {");
				for (int i = 0; i < mapping.columnResults().size(); i++) {
					if (i > 0) sb.append(", ");
					sb.append("@ColumnResult(name = \"").append(mapping.columnResults().get(i).name()).append("\")");
				}
				sb.append("}");
			}
			sb.append(")\n");
		}
		for (NamedNativeQueryInfo nnq : getNamedNativeQueries()) {
			importType("jakarta.persistence.NamedNativeQuery");
			sb.append("@NamedNativeQuery(name = \"").append(nnq.name())
					.append("\", query = \"").append(nnq.query()).append("\"");
			if (nnq.resultClass() != null) {
				String simpleResultClass = importType(nnq.resultClass());
				sb.append(", resultClass = ").append(simpleResultClass).append(".class");
			}
			if (nnq.resultSetMapping() != null) {
				sb.append(", resultSetMapping = \"").append(nnq.resultSetMapping()).append("\"");
			}
			sb.append(")\n");
		}
		// @FilterDef / @Filter
		for (FilterDefInfo fd : getFilterDefs()) {
			importType("org.hibernate.annotations.FilterDef");
			sb.append("@FilterDef(name = \"").append(fd.name()).append("\"");
			if (!fd.defaultCondition().isEmpty()) {
				sb.append(", defaultCondition = \"").append(fd.defaultCondition()).append("\"");
			}
			if (!fd.parameters().isEmpty()) {
				importType("org.hibernate.annotations.ParamDef");
				sb.append(", parameters = {");
				boolean first = true;
				for (Map.Entry<String, Class<?>> entry : fd.parameters().entrySet()) {
					if (!first) sb.append(", ");
					first = false;
					String simpleType = importType(entry.getValue().getName());
					sb.append("@ParamDef(name = \"").append(entry.getKey())
							.append("\", type = ").append(simpleType).append(".class)");
				}
				sb.append("}");
			}
			sb.append(")\n");
		}
		for (FilterInfo fi : getFilters()) {
			importType("org.hibernate.annotations.Filter");
			sb.append("@Filter(name = \"").append(fi.name()).append("\"");
			if (!fi.condition().isEmpty()) {
				sb.append(", condition = \"").append(fi.condition()).append("\"");
			}
			sb.append(")\n");
		}
		// @SQLInsert / @SQLUpdate / @SQLDelete / @SQLDeleteAll
		for (SQLInsert si : getSQLInserts()) {
			importType("org.hibernate.annotations.SQLInsert");
			sb.append("@SQLInsert(sql = \"").append(si.sql()).append("\"");
			if (si.callable()) {
				sb.append(", callable = true");
			}
			sb.append(")\n");
		}
		for (SQLUpdate su : getSQLUpdates()) {
			importType("org.hibernate.annotations.SQLUpdate");
			sb.append("@SQLUpdate(sql = \"").append(su.sql()).append("\"");
			if (su.callable()) {
				sb.append(", callable = true");
			}
			sb.append(")\n");
		}
		for (SQLDelete sd : getSQLDeletes()) {
			importType("org.hibernate.annotations.SQLDelete");
			sb.append("@SQLDelete(sql = \"").append(sd.sql()).append("\"");
			if (sd.callable()) {
				sb.append(", callable = true");
			}
			sb.append(")\n");
		}
		SQLDeleteAll sda = classDetails.getDirectAnnotationUsage(SQLDeleteAll.class);
		if (sda != null) {
			importType("org.hibernate.annotations.SQLDeleteAll");
			sb.append("@SQLDeleteAll(sql = \"").append(sda.sql()).append("\"");
			if (sda.callable()) {
				sb.append(", callable = true");
			}
			sb.append(")\n");
		}
		// @FetchProfile
		for (FetchProfile fp : getFetchProfiles()) {
			importType("org.hibernate.annotations.FetchProfile");
			sb.append("@FetchProfile(name = \"").append(fp.name()).append("\"");
			if (fp.fetchOverrides().length > 0) {
				importType("org.hibernate.annotations.FetchMode");
				sb.append(", fetchOverrides = {");
				for (int i = 0; i < fp.fetchOverrides().length; i++) {
					if (i > 0) sb.append(", ");
					FetchProfile.FetchOverride fo = fp.fetchOverrides()[i];
					String simpleEntity = importType(fo.entity().getName());
					sb.append("@FetchProfile.FetchOverride(entity = ").append(simpleEntity)
							.append(".class, association = \"").append(fo.association())
							.append("\", mode = FetchMode.").append(fo.mode().name()).append(")");
				}
				sb.append("}");
			}
			sb.append(")\n");
		}
		// @EntityListeners
		EntityListeners el = classDetails.getDirectAnnotationUsage(EntityListeners.class);
		if (el != null && el.value() != null && el.value().length > 0) {
			importType("jakarta.persistence.EntityListeners");
			sb.append("@EntityListeners(");
			if (el.value().length == 1) {
				String simpleType = importType(el.value()[0].getName());
				sb.append(simpleType).append(".class");
			} else {
				sb.append("{");
				for (int i = 0; i < el.value().length; i++) {
					if (i > 0) sb.append(", ");
					String simpleType = importType(el.value()[i].getName());
					sb.append(simpleType).append(".class");
				}
				sb.append("}");
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
		UniqueConstraint[] ucs = table.uniqueConstraints();
		if (ucs != null && ucs.length > 0) {
			importType("jakarta.persistence.UniqueConstraint");
			sb.append(", uniqueConstraints = ");
			if (ucs.length == 1) {
				sb.append(formatUniqueConstraint(ucs[0]));
			} else {
				sb.append("{ ");
				for (int i = 0; i < ucs.length; i++) {
					if (i > 0) sb.append(", ");
					sb.append(formatUniqueConstraint(ucs[i]));
				}
				sb.append(" }");
			}
		}
		sb.append(")\n");
		return sb.toString();
	}

	private String formatUniqueConstraint(UniqueConstraint uc) {
		StringBuilder sb = new StringBuilder();
		sb.append("@UniqueConstraint(");
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

	public String generateOptimisticLockAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		OptimisticLock ol = field.getDirectAnnotationUsage(OptimisticLock.class);
		if (ol == null || !ol.excluded()) {
			return "";
		}
		importType("org.hibernate.annotations.OptimisticLock");
		return "@OptimisticLock(excluded = true)";
	}

	public String generateAccessAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Access access = field.getDirectAnnotationUsage(Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return "";
		}
		importType("jakarta.persistence.Access");
		importType("jakarta.persistence.AccessType");
		return "@Access(AccessType." + access.value().name() + ")";
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

	public String generateFilterAnnotations(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		List<FilterInfo> filters = getFieldFilters(field);
		if (filters.isEmpty()) {
			return "";
		}
		importType("org.hibernate.annotations.Filter");
		StringBuilder sb = new StringBuilder();
		for (FilterInfo fi : filters) {
			sb.append("@Filter(name = \"").append(fi.name()).append("\"");
			if (!fi.condition().isEmpty()) {
				sb.append(", condition = \"").append(fi.condition()).append("\"");
			}
			sb.append(")\n    ");
		}
		return sb.toString().stripTrailing();
	}

	public List<FilterInfo> getFieldFilters(FieldDetails field) {
		List<FilterInfo> result = new ArrayList<>();
		Filter single = field.getDirectAnnotationUsage(Filter.class);
		if (single != null) {
			result.add(new FilterInfo(single.name(), single.condition()));
		}
		Filters container = field.getDirectAnnotationUsage(Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public String generateBagAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (!field.hasDirectAnnotationUsage(Bag.class)) {
			return "";
		}
		importType("org.hibernate.annotations.Bag");
		return "@Bag";
	}

	public String generateCollectionIdAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		CollectionId cid = field.getDirectAnnotationUsage(CollectionId.class);
		if (cid == null) {
			return "";
		}
		importType("org.hibernate.annotations.CollectionId");
		StringBuilder sb = new StringBuilder("@CollectionId(");
		if (cid.column() != null && cid.column().name() != null && !cid.column().name().isEmpty()) {
			importType("jakarta.persistence.Column");
			sb.append("column = @Column(name = \"").append(cid.column().name()).append("\"), ");
		}
		if (cid.generator() != null && !cid.generator().isEmpty()) {
			sb.append("generator = \"").append(cid.generator()).append("\"");
		}
		String result = sb.toString();
		if (result.endsWith(", ")) {
			result = result.substring(0, result.length() - 2);
		}
		return result + ")";
	}

	public String generateSortAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (field.hasDirectAnnotationUsage(SortNatural.class)) {
			importType("org.hibernate.annotations.SortNatural");
			return "@SortNatural";
		}
		SortComparator sc = field.getDirectAnnotationUsage(SortComparator.class);
		if (sc != null) {
			importType("org.hibernate.annotations.SortComparator");
			String simpleType = importType(sc.value().getName());
			return "@SortComparator(" + simpleType + ".class)";
		}
		return "";
	}

	public String generateMapKeyAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		MapKey mk = field.getDirectAnnotationUsage(MapKey.class);
		if (mk == null) {
			return "";
		}
		importType("jakarta.persistence.MapKey");
		if (mk.name() != null && !mk.name().isEmpty()) {
			return "@MapKey(name = \"" + mk.name() + "\")";
		}
		return "@MapKey";
	}

	public String generateMapKeyColumnAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		MapKeyColumn mkc = field.getDirectAnnotationUsage(MapKeyColumn.class);
		if (mkc == null) {
			return "";
		}
		importType("jakarta.persistence.MapKeyColumn");
		if (mkc.name() != null && !mkc.name().isEmpty()) {
			return "@MapKeyColumn(name = \"" + mkc.name() + "\")";
		}
		return "@MapKeyColumn";
	}

	public String generateFetchAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Fetch fetch = field.getDirectAnnotationUsage(Fetch.class);
		if (fetch == null) {
			return "";
		}
		importType("org.hibernate.annotations.Fetch");
		importType("org.hibernate.annotations.FetchMode");
		return "@Fetch(FetchMode." + fetch.value().name() + ")";
	}

	public String generateNotFoundAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		NotFound nf = field.getDirectAnnotationUsage(NotFound.class);
		if (nf == null || nf.action() == NotFoundAction.EXCEPTION) {
			return "";
		}
		importType("org.hibernate.annotations.NotFound");
		importType("org.hibernate.annotations.NotFoundAction");
		return "@NotFound(action = NotFoundAction." + nf.action().name() + ")";
	}

	public String generateAnyAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (!field.hasDirectAnnotationUsage(Any.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("org.hibernate.annotations.Any");
		sb.append("@Any\n");
		sb.append(generateAnyDiscriminatorAnnotations(field));
		return sb.toString().stripTrailing();
	}

	public String generateManyToAnyAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		if (!field.hasDirectAnnotationUsage(ManyToAny.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("org.hibernate.annotations.ManyToAny");
		sb.append("@ManyToAny\n");
		sb.append(generateAnyDiscriminatorAnnotations(field));
		return sb.toString().stripTrailing();
	}

	private String generateAnyDiscriminatorAnnotations(FieldDetails field) {
		StringBuilder sb = new StringBuilder();
		// @AnyDiscriminator
		AnyDiscriminator ad = field.getDirectAnnotationUsage(AnyDiscriminator.class);
		if (ad != null) {
			importType("org.hibernate.annotations.AnyDiscriminator");
			importType("jakarta.persistence.DiscriminatorType");
			sb.append("    @AnyDiscriminator(DiscriminatorType.").append(ad.value().name()).append(")\n");
		}
		// @AnyDiscriminatorValue(s)
		List<AnyDiscriminatorValue> values = new ArrayList<>();
		AnyDiscriminatorValue single = field.getDirectAnnotationUsage(AnyDiscriminatorValue.class);
		if (single != null) {
			values.add(single);
		}
		AnyDiscriminatorValues container = field.getDirectAnnotationUsage(AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				values.add(adv);
			}
		}
		for (AnyDiscriminatorValue adv : values) {
			importType("org.hibernate.annotations.AnyDiscriminatorValue");
			String simpleEntity = importType(adv.entity().getName());
			sb.append("    @AnyDiscriminatorValue(discriminator = \"").append(adv.discriminator())
					.append("\", entity = ").append(simpleEntity).append(".class)\n");
		}
		// @AnyKeyJavaClass
		AnyKeyJavaClass akjc = field.getDirectAnnotationUsage(AnyKeyJavaClass.class);
		if (akjc != null) {
			importType("org.hibernate.annotations.AnyKeyJavaClass");
			String simpleType = importType(akjc.value().getName());
			sb.append("    @AnyKeyJavaClass(").append(simpleType).append(".class)\n");
		}
		return sb.toString();
	}

	public String generateConvertAnnotation(FieldDetails field) {
		if (!annotated) {
			return "";
		}
		Convert convert = field.getDirectAnnotationUsage(Convert.class);
		if (convert == null || convert.disableConversion()) {
			return "";
		}
		Class<?> converterClass = convert.converter();
		if (converterClass == null || converterClass == jakarta.persistence.AttributeConverter.class) {
			return "";
		}
		importType("jakarta.persistence.Convert");
		String simpleType = importType(converterClass.getName());
		return "@Convert(converter = " + simpleType + ".class)";
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
		int minTotal = getMinimalConstructorProperties().size()
				+ getSuperclassMinimalConstructorProperties().size();
		int fullTotal = getFullConstructorProperties().size()
				+ getSuperclassFullConstructorProperties().size();
		return minTotal > 0 && minTotal < fullTotal;
	}

	public List<FullConstructorProperty> getMinimalConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		// Basic fields: non-nullable, non-version, non-generated-id, respects gen-property
		for (FieldDetails field : getBasicFields()) {
			if (isVersion(field) || !isGenProperty(field) || hasFieldDefaultValue(field)) {
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
		for (FullConstructorProperty prop : getSuperclassMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		for (FullConstructorProperty prop : getMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	public String getFullConstructorParameterList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		for (FullConstructorProperty prop : getFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	public List<FullConstructorProperty> getSuperclassFullConstructorProperties() {
		if (!isSubclass()) {
			return Collections.emptyList();
		}
		return createSuperclassHelper().getFullConstructorProperties();
	}

	public String getSuperclassFullConstructorArgumentList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassFullConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.fieldName());
		}
		return sb.toString();
	}

	public List<FullConstructorProperty> getSuperclassMinimalConstructorProperties() {
		if (!isSubclass()) {
			return Collections.emptyList();
		}
		return createSuperclassHelper().getMinimalConstructorProperties();
	}

	public String getSuperclassMinimalConstructorArgumentList() {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : getSuperclassMinimalConstructorProperties()) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.fieldName());
		}
		return sb.toString();
	}

	private TemplateHelper createSuperclassHelper() {
		ClassDetails superClass = classDetails.getSuperClass();
		return new TemplateHelper(superClass, modelsContext, importContext, annotated);
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
		NamedNativeQuery single = classDetails.getDirectAnnotationUsage(NamedNativeQuery.class);
		if (single != null) {
			result.add(toNamedNativeQueryInfo(single));
		}
		NamedNativeQueries container = classDetails.getDirectAnnotationUsage(NamedNativeQueries.class);
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
		String resultSetMapping = nnq.resultSetMapping() != null && !nnq.resultSetMapping().isEmpty()
				? nnq.resultSetMapping() : null;
		return new NamedNativeQueryInfo(nnq.name(), nnq.query(), resultClassName, resultSetMapping);
	}

	public List<SqlResultSetMappingInfo> getSqlResultSetMappings() {
		List<SqlResultSetMappingInfo> result = new ArrayList<>();
		SqlResultSetMapping single = classDetails.getDirectAnnotationUsage(SqlResultSetMapping.class);
		if (single != null) {
			result.add(toSqlResultSetMappingInfo(single));
		}
		SqlResultSetMappings container = classDetails.getDirectAnnotationUsage(SqlResultSetMappings.class);
		if (container != null) {
			for (SqlResultSetMapping mapping : container.value()) {
				result.add(toSqlResultSetMappingInfo(mapping));
			}
		}
		return result;
	}

	private SqlResultSetMappingInfo toSqlResultSetMappingInfo(SqlResultSetMapping mapping) {
		List<EntityResultInfo> entityResults = new ArrayList<>();
		for (EntityResult er : mapping.entities()) {
			List<FieldResultInfo> fieldResults = new ArrayList<>();
			for (FieldResult fr : er.fields()) {
				fieldResults.add(new FieldResultInfo(fr.name(), fr.column()));
			}
			String discriminator = er.discriminatorColumn() != null && !er.discriminatorColumn().isEmpty()
					? er.discriminatorColumn() : null;
			entityResults.add(new EntityResultInfo(er.entityClass().getName(), discriminator, fieldResults));
		}
		List<ColumnResultInfo> columnResults = new ArrayList<>();
		for (ColumnResult cr : mapping.columns()) {
			columnResults.add(new ColumnResultInfo(cr.name()));
		}
		return new SqlResultSetMappingInfo(mapping.name(), entityResults, columnResults);
	}

	public record NamedQueryInfo(String name, String query) {}

	public record NamedNativeQueryInfo(String name, String query,
									   String resultClass, String resultSetMapping) {}

	public record SqlResultSetMappingInfo(String name, List<EntityResultInfo> entityResults,
										  List<ColumnResultInfo> columnResults) {}

	public record EntityResultInfo(String entityClass, String discriminatorColumn,
								   List<FieldResultInfo> fieldResults) {}

	public record FieldResultInfo(String name, String column) {}

	public record ColumnResultInfo(String name) {}

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
		Map<String, Class<?>> params = new java.util.LinkedHashMap<>();
		if (fd.parameters() != null) {
			for (ParamDef pd : fd.parameters()) {
				params.put(pd.name(), pd.type());
			}
		}
		return new FilterDefInfo(fd.name(), fd.defaultCondition(), params);
	}

	public record FilterInfo(String name, String condition) {}

	public record FilterDefInfo(String name, String defaultCondition, Map<String, Class<?>> parameters) {}

	public record SecondaryTableInfo(String tableName, List<String> keyColumns) {}

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

	public List<FetchProfile> getFetchProfiles() {
		List<FetchProfile> result = new ArrayList<>();
		FetchProfile single = classDetails.getDirectAnnotationUsage(FetchProfile.class);
		if (single != null) {
			result.add(single);
		}
		FetchProfiles container = classDetails.getDirectAnnotationUsage(FetchProfiles.class);
		if (container != null) {
			for (FetchProfile fp : container.value()) {
				result.add(fp);
			}
		}
		return result;
	}

	public List<SecondaryTableInfo> getSecondaryTables() {
		List<SecondaryTableInfo> result = new ArrayList<>();
		SecondaryTable single = classDetails.getDirectAnnotationUsage(SecondaryTable.class);
		if (single != null) {
			result.add(toSecondaryTableInfo(single));
		}
		SecondaryTables container = classDetails.getDirectAnnotationUsage(SecondaryTables.class);
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

	public String getFieldModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-field")) {
			return getFieldMetaAttribute(field, "scope-field");
		}
		return "private";
	}

	public String getPropertyGetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-get")) {
			return getFieldMetaAttribute(field, "scope-get");
		}
		return "public";
	}

	public String getPropertySetModifiers(FieldDetails field) {
		if (hasFieldMetaAttribute(field, "scope-set")) {
			return getFieldMetaAttribute(field, "scope-set");
		}
		return "public";
	}

	public boolean hasClassDescription() {
		return hasClassMetaAttribute("class-description");
	}

	public String getClassDescription() {
		return getClassMetaAttribute("class-description");
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

	public boolean hasFieldDefaultValue(FieldDetails field) {
		return hasFieldMetaAttribute(field, "default-value");
	}

	public String getFieldDefaultValue(FieldDetails field) {
		return getFieldMetaAttribute(field, "default-value");
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

	// --- Lifecycle callbacks ---

	@SuppressWarnings("unchecked")
	private static final Class<? extends java.lang.annotation.Annotation>[] LIFECYCLE_ANNOTATIONS = new Class[] {
			PrePersist.class, PostPersist.class,
			PreRemove.class, PostRemove.class,
			PreUpdate.class, PostUpdate.class,
			PostLoad.class
	};

	public List<LifecycleCallbackInfo> getLifecycleCallbacks() {
		List<LifecycleCallbackInfo> result = new ArrayList<>();
		for (MethodDetails method : classDetails.getMethods()) {
			for (Class<? extends java.lang.annotation.Annotation> ann : LIFECYCLE_ANNOTATIONS) {
				if (method.hasDirectAnnotationUsage(ann)) {
					result.add(new LifecycleCallbackInfo(ann.getSimpleName(), method.getName()));
				}
			}
		}
		return result;
	}

	public String generateLifecycleCallbackAnnotation(LifecycleCallbackInfo callback) {
		if (!annotated) {
			return "";
		}
		String fqcn = switch (callback.annotationType()) {
			case "PrePersist" -> "jakarta.persistence.PrePersist";
			case "PostPersist" -> "jakarta.persistence.PostPersist";
			case "PreRemove" -> "jakarta.persistence.PreRemove";
			case "PostRemove" -> "jakarta.persistence.PostRemove";
			case "PreUpdate" -> "jakarta.persistence.PreUpdate";
			case "PostUpdate" -> "jakarta.persistence.PostUpdate";
			case "PostLoad" -> "jakarta.persistence.PostLoad";
			default -> throw new IllegalArgumentException("Unknown lifecycle annotation: " + callback.annotationType());
		};
		importType(fqcn);
		return "@" + callback.annotationType();
	}

	public record LifecycleCallbackInfo(String annotationType, String methodName) {}

	// --- Private helpers ---

	private String getPackageName() {
		String className = classDetails.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	private String getGeneratedPackageName() {
		if (hasClassMetaAttribute("generated-class")) {
			String generatedClass = getClassMetaAttribute("generated-class");
			int lastDot = generatedClass.lastIndexOf('.');
			return lastDot > 0 ? generatedClass.substring(0, lastDot) : "";
		}
		return getPackageName();
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
