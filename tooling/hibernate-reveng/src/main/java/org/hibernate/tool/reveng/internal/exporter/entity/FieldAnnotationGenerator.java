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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

import org.hibernate.models.spi.FieldDetails;

import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FilterInfo;

public class FieldAnnotationGenerator {

	private final ImportContext importContext;
	private final TemplateHelper templateHelper;
	private final boolean annotated;

	FieldAnnotationGenerator(ImportContext importContext, TemplateHelper templateHelper) {
		this.importContext = importContext;
		this.templateHelper = templateHelper;
		this.annotated = templateHelper.isAnnotated();
	}

	public String generateIdAnnotations(FieldDetails field) {
		if (!annotated) return "";
		StringBuilder sb = new StringBuilder();
		if (!templateHelper.fieldHasAnnotation(field, Id.class)) {
			return "";
		}
		importType("jakarta.persistence.Id");
		sb.append("@Id\n");
		GeneratedValue gv = templateHelper.fieldGetAnnotation(field, GeneratedValue.class);
		if (gv != null) {
			appendGeneratedValue(sb, gv);
			appendSequenceGenerator(sb, field);
			appendTableGenerator(sb, field);
		}
		return sb.toString().stripTrailing();
	}

	private void appendGeneratedValue(StringBuilder sb, GeneratedValue gv) {
		importType("jakarta.persistence.GeneratedValue");
		importType("jakarta.persistence.GenerationType");
		sb.append("    @GeneratedValue(strategy = GenerationType.")
				.append(gv.strategy().name());
		if (gv.generator() != null && !gv.generator().isEmpty()) {
			sb.append(", generator = \"").append(gv.generator()).append("\"");
		}
		sb.append(")\n");
	}

	private void appendSequenceGenerator(StringBuilder sb, FieldDetails field) {
		SequenceGenerator sg = templateHelper.fieldGetAnnotation(field, SequenceGenerator.class);
		if (sg == null) {
			return;
		}
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

	private void appendTableGenerator(StringBuilder sb, FieldDetails field) {
		TableGenerator tg = templateHelper.fieldGetAnnotation(field, TableGenerator.class);
		if (tg == null) {
			return;
		}
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

	public String generateVersionAnnotation() {
		if (!annotated) return "";
		importType("jakarta.persistence.Version");
		return "@Version";
	}

	public String generateBasicAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Basic basic = templateHelper.fieldGetAnnotation(field, Basic.class);
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
		if (!annotated) return "";
		OptimisticLock ol = templateHelper.fieldGetAnnotation(field, OptimisticLock.class);
		if (ol == null || !ol.excluded()) {
			return "";
		}
		importType("org.hibernate.annotations.OptimisticLock");
		return "@OptimisticLock(excluded = true)";
	}

	public String generateAccessAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Access access = templateHelper.fieldGetAnnotation(field, Access.class);
		if (access == null || access.value() == AccessType.FIELD) {
			return "";
		}
		importType("jakarta.persistence.Access");
		importType("jakarta.persistence.AccessType");
		return "@Access(AccessType." + access.value().name() + ")";
	}

	public String generateTemporalAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Temporal temporal = templateHelper.fieldGetAnnotation(field, Temporal.class);
		if (temporal == null) {
			return "";
		}
		importType("jakarta.persistence.Temporal");
		importType("jakarta.persistence.TemporalType");
		return "@Temporal(TemporalType." + temporal.value().name() + ")";
	}

	public String generateLobAnnotation() {
		if (!annotated) return "";
		importType("jakarta.persistence.Lob");
		return "@Lob";
	}

	public String generateFormulaAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Formula formula = templateHelper.fieldGetAnnotation(field, Formula.class);
		if (formula == null) {
			return "";
		}
		importType("org.hibernate.annotations.Formula");
		return "@Formula(\"" + formula.value() + "\")";
	}

	public String generateElementCollectionAnnotation(FieldDetails field) {
		if (!annotated) return "";
		ElementCollection ec = templateHelper.fieldGetAnnotation(field, ElementCollection.class);
		if (ec == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("jakarta.persistence.ElementCollection");
		sb.append("@ElementCollection");
		if (ec.fetch() != FetchType.LAZY) {
			importType("jakarta.persistence.FetchType");
			sb.append("(fetch = FetchType.").append(ec.fetch().name()).append(")");
		}
		appendCollectionTable(sb, field);
		appendElementColumn(sb, field);
		return sb.toString();
	}

	private void appendCollectionTable(StringBuilder sb, FieldDetails field) {
		CollectionTable ct = templateHelper.fieldGetAnnotation(field, CollectionTable.class);
		if (ct == null) {
			return;
		}
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

	private void appendElementColumn(StringBuilder sb, FieldDetails field) {
		Column col = templateHelper.fieldGetAnnotation(field, Column.class);
		if (col == null) {
			return;
		}
		sb.append("\n    ");
		importType("jakarta.persistence.Column");
		sb.append("@Column(name = \"").append(col.name()).append("\")");
	}

	public String generateNaturalIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		NaturalId nid = templateHelper.fieldGetAnnotation(field, NaturalId.class);
		if (nid == null) {
			return "";
		}
		importType("org.hibernate.annotations.NaturalId");
		if (nid.mutable()) {
			return "@NaturalId(mutable = true)";
		}
		return "@NaturalId";
	}

	public String generateColumnAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Column col = templateHelper.fieldGetAnnotation(field, Column.class);
		if (col == null) {
			return "";
		}
		importType("jakarta.persistence.Column");
		StringBuilder sb = new StringBuilder("@Column(name = \"");
		sb.append(col.name()).append("\"");
		appendColumnAttributes(sb, col);
		sb.append(")");
		return sb.toString();
	}

	private void appendColumnAttributes(StringBuilder sb, Column col) {
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
	}

	public String generateColumnTransformerAnnotation(FieldDetails field) {
		if (!annotated) return "";
		ColumnTransformer ct = templateHelper.fieldGetAnnotation(field, ColumnTransformer.class);
		if (ct == null) {
			return "";
		}
		boolean hasRead = ct.read() != null && !ct.read().isEmpty();
		boolean hasWrite = ct.write() != null && !ct.write().isEmpty();
		if (!hasRead && !hasWrite) {
			return "";
		}
		importType("org.hibernate.annotations.ColumnTransformer");
		StringBuilder sb = new StringBuilder("@ColumnTransformer(");
		boolean needComma = false;
		if (ct.forColumn() != null && !ct.forColumn().isEmpty()) {
			sb.append("forColumn = \"").append(ct.forColumn()).append("\"");
			needComma = true;
		}
		if (hasRead) {
			if (needComma) sb.append(", ");
			sb.append("read = \"").append(ct.read()).append("\"");
			needComma = true;
		}
		if (hasWrite) {
			if (needComma) sb.append(", ");
			sb.append("write = \"").append(ct.write()).append("\"");
		}
		sb.append(")");
		return sb.toString();
	}

	public String generateConvertAnnotation(FieldDetails field) {
		if (!annotated) return "";
		Convert convert = templateHelper.fieldGetAnnotation(field, Convert.class);
		if (convert == null || convert.disableConversion()) {
			return "";
		}
		Class<?> converterClass = convert.converter();
		if (converterClass == null
				|| converterClass == jakarta.persistence.AttributeConverter.class) {
			return "";
		}
		importType("jakarta.persistence.Convert");
		String simpleType = importType(converterClass.getName());
		return "@Convert(converter = " + simpleType + ".class)";
	}

	public String generateOrderByAnnotation(FieldDetails field) {
		if (!annotated) return "";
		OrderBy ob = templateHelper.fieldGetAnnotation(field, OrderBy.class);
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
		if (!annotated) return "";
		OrderColumn oc = templateHelper.fieldGetAnnotation(field, OrderColumn.class);
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
		if (!annotated) return "";
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
		Filter single = templateHelper.fieldGetAnnotation(field, Filter.class);
		if (single != null) {
			result.add(new FilterInfo(single.name(), single.condition()));
		}
		Filters container = templateHelper.fieldGetAnnotation(field, Filters.class);
		if (container != null) {
			for (Filter f : container.value()) {
				result.add(new FilterInfo(f.name(), f.condition()));
			}
		}
		return result;
	}

	public String generateBagAnnotation(FieldDetails field) {
		if (!annotated) return "";
		if (!templateHelper.fieldHasAnnotation(field, Bag.class)) {
			return "";
		}
		importType("org.hibernate.annotations.Bag");
		return "@Bag";
	}

	public String generateCollectionIdAnnotation(FieldDetails field) {
		if (!annotated) return "";
		CollectionId cid = templateHelper.fieldGetAnnotation(field, CollectionId.class);
		if (cid == null) {
			return "";
		}
		importType("org.hibernate.annotations.CollectionId");
		StringBuilder sb = new StringBuilder("@CollectionId(");
		if (cid.column() != null && cid.column().name() != null
				&& !cid.column().name().isEmpty()) {
			importType("jakarta.persistence.Column");
			sb.append("column = @Column(name = \"")
					.append(cid.column().name()).append("\"), ");
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
		if (!annotated) return "";
		if (templateHelper.fieldHasAnnotation(field, SortNatural.class)) {
			importType("org.hibernate.annotations.SortNatural");
			return "@SortNatural";
		}
		SortComparator sc = templateHelper.fieldGetAnnotation(field, SortComparator.class);
		if (sc != null) {
			importType("org.hibernate.annotations.SortComparator");
			String simpleType = importType(sc.value().getName());
			return "@SortComparator(" + simpleType + ".class)";
		}
		return "";
	}

	public String generateMapKeyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		MapKey mk = templateHelper.fieldGetAnnotation(field, MapKey.class);
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
		if (!annotated) return "";
		MapKeyColumn mkc = templateHelper.fieldGetAnnotation(field, MapKeyColumn.class);
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
		if (!annotated) return "";
		Fetch fetch = templateHelper.fieldGetAnnotation(field, Fetch.class);
		if (fetch == null) {
			return "";
		}
		importType("org.hibernate.annotations.Fetch");
		importType("org.hibernate.annotations.FetchMode");
		return "@Fetch(FetchMode." + fetch.value().name() + ")";
	}

	public String generateNotFoundAnnotation(FieldDetails field) {
		if (!annotated) return "";
		NotFound nf = templateHelper.fieldGetAnnotation(field, NotFound.class);
		if (nf == null || nf.action() == NotFoundAction.EXCEPTION) {
			return "";
		}
		importType("org.hibernate.annotations.NotFound");
		importType("org.hibernate.annotations.NotFoundAction");
		return "@NotFound(action = NotFoundAction." + nf.action().name() + ")";
	}

	public String generateAnyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		if (!templateHelper.fieldHasAnnotation(field, Any.class)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		importType("org.hibernate.annotations.Any");
		sb.append("@Any\n");
		sb.append(generateAnyDiscriminatorAnnotations(field));
		return sb.toString().stripTrailing();
	}

	public String generateManyToAnyAnnotation(FieldDetails field) {
		if (!annotated) return "";
		if (!templateHelper.fieldHasAnnotation(field, ManyToAny.class)) {
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
		appendAnyDiscriminator(sb, field);
		appendAnyDiscriminatorValues(sb, field);
		appendAnyKeyJavaClass(sb, field);
		return sb.toString();
	}

	private void appendAnyDiscriminator(StringBuilder sb, FieldDetails field) {
		AnyDiscriminator ad = templateHelper.fieldGetAnnotation(field, AnyDiscriminator.class);
		if (ad != null) {
			importType("org.hibernate.annotations.AnyDiscriminator");
			importType("jakarta.persistence.DiscriminatorType");
			sb.append("    @AnyDiscriminator(DiscriminatorType.")
					.append(ad.value().name()).append(")\n");
		}
	}

	private void appendAnyDiscriminatorValues(StringBuilder sb, FieldDetails field) {
		List<AnyDiscriminatorValue> values = new ArrayList<>();
		AnyDiscriminatorValue single = templateHelper.fieldGetAnnotation(
				field, AnyDiscriminatorValue.class);
		if (single != null) {
			values.add(single);
		}
		AnyDiscriminatorValues container = templateHelper.fieldGetAnnotation(
				field, AnyDiscriminatorValues.class);
		if (container != null) {
			for (AnyDiscriminatorValue adv : container.value()) {
				values.add(adv);
			}
		}
		for (AnyDiscriminatorValue adv : values) {
			importType("org.hibernate.annotations.AnyDiscriminatorValue");
			String simpleEntity = importType(adv.entity().getName());
			sb.append("    @AnyDiscriminatorValue(discriminator = \"")
					.append(adv.discriminator()).append("\", entity = ")
					.append(simpleEntity).append(".class)\n");
		}
	}

	private void appendAnyKeyJavaClass(StringBuilder sb, FieldDetails field) {
		AnyKeyJavaClass akjc = templateHelper.fieldGetAnnotation(
				field, AnyKeyJavaClass.class);
		if (akjc != null) {
			importType("org.hibernate.annotations.AnyKeyJavaClass");
			String simpleType = importType(akjc.value().getName());
			sb.append("    @AnyKeyJavaClass(").append(simpleType).append(".class)\n");
		}
	}

	private String importType(String fqcn) {
		return importContext.importType(fqcn);
	}
}
