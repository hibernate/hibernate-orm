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
package org.hibernate.tool.internal.exporter.mapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Handles field-level annotation reading for mapping XML template generation:
 * field categorization, column attributes, and generators.
 *
 * @author Koen Aers
 */
public class MappingFieldAnnotationHelper {

	private final ClassDetails classDetails;

	MappingFieldAnnotationHelper(ClassDetails classDetails) {
		this.classDetails = classDetails;
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

	public List<FieldDetails> getIdFields() {
		return getFieldsWithAnnotation(Id.class);
	}

	public List<FieldDetails> getBasicFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (!isRelationshipField(field) && !isEmbeddedField(field)
					&& !field.hasDirectAnnotationUsage(EmbeddedId.class)
					&& !field.hasDirectAnnotationUsage(Id.class)
					&& !field.hasDirectAnnotationUsage(Version.class)
					&& !field.hasDirectAnnotationUsage(Any.class)
					&& !field.hasDirectAnnotationUsage(ManyToAny.class)
					&& !field.hasDirectAnnotationUsage(ElementCollection.class)
					&& !field.hasDirectAnnotationUsage(NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public List<FieldDetails> getNaturalIdFields() {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(NaturalId.class)) {
				result.add(field);
			}
		}
		return result;
	}

	public boolean isNaturalIdMutable() {
		for (FieldDetails field : classDetails.getFields()) {
			NaturalId nid = field.getDirectAnnotationUsage(NaturalId.class);
			if (nid != null) {
				return nid.mutable();
			}
		}
		return false;
	}

	public List<FieldDetails> getVersionFields() {
		return getFieldsWithAnnotation(Version.class);
	}

	public List<FieldDetails> getManyToOneFields() {
		return getFieldsWithAnnotation(ManyToOne.class);
	}

	public List<FieldDetails> getOneToManyFields() {
		return getFieldsWithAnnotation(OneToMany.class);
	}

	public List<FieldDetails> getOneToOneFields() {
		return getFieldsWithAnnotation(OneToOne.class);
	}

	public List<FieldDetails> getManyToManyFields() {
		return getFieldsWithAnnotation(ManyToMany.class);
	}

	public List<FieldDetails> getEmbeddedFields() {
		return getFieldsWithAnnotation(Embedded.class);
	}

	public List<FieldDetails> getAnyFields() {
		return getFieldsWithAnnotation(Any.class);
	}

	public List<FieldDetails> getManyToAnyFields() {
		return getFieldsWithAnnotation(ManyToAny.class);
	}

	public List<FieldDetails> getElementCollectionFields() {
		return getFieldsWithAnnotation(ElementCollection.class);
	}

	// --- Column attributes ---

	public String getColumnName(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.name() : field.getName();
	}

	public boolean isNullable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.nullable();
	}

	public boolean isUnique(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.unique();
	}

	public int getLength(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		if (col == null) return 0;
		return col.length() != 255 ? col.length() : 0;
	}

	public int getPrecision(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.precision() : 0;
	}

	public int getScale(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null ? col.scale() : 0;
	}

	public String getColumnDefinition(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col != null && col.columnDefinition() != null && !col.columnDefinition().isEmpty()
				? col.columnDefinition() : null;
	}

	public boolean isInsertable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.insertable();
	}

	public boolean isUpdatable(FieldDetails field) {
		Column col = field.getDirectAnnotationUsage(Column.class);
		return col == null || col.updatable();
	}

	public boolean isLob(FieldDetails field) {
		return field.hasDirectAnnotationUsage(Lob.class);
	}

	public String getTemporalType(FieldDetails field) {
		Temporal temporal = field.getDirectAnnotationUsage(Temporal.class);
		return temporal != null ? temporal.value().name() : null;
	}

	public String getGenerationType(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null ? gv.strategy().name() : null;
	}

	public String getGeneratorName(FieldDetails field) {
		GeneratedValue gv = field.getDirectAnnotationUsage(GeneratedValue.class);
		return gv != null && gv.generator() != null && !gv.generator().isEmpty()
				? gv.generator() : null;
	}

	public MappingXmlHelper.SequenceGeneratorInfo getSequenceGenerator(FieldDetails field) {
		SequenceGenerator sg = field.getDirectAnnotationUsage(SequenceGenerator.class);
		if (sg == null) {
			return null;
		}
		return new MappingXmlHelper.SequenceGeneratorInfo(
				sg.name(),
				sg.sequenceName() != null && !sg.sequenceName().isEmpty() ? sg.sequenceName() : null,
				sg.allocationSize() != 50 ? sg.allocationSize() : null,
				sg.initialValue() != 1 ? sg.initialValue() : null);
	}

	public MappingXmlHelper.TableGeneratorInfo getTableGenerator(FieldDetails field) {
		TableGenerator tg = field.getDirectAnnotationUsage(TableGenerator.class);
		if (tg == null) {
			return null;
		}
		return new MappingXmlHelper.TableGeneratorInfo(
				tg.name(),
				tg.table() != null && !tg.table().isEmpty() ? tg.table() : null,
				tg.pkColumnName() != null && !tg.pkColumnName().isEmpty() ? tg.pkColumnName() : null,
				tg.valueColumnName() != null && !tg.valueColumnName().isEmpty() ? tg.valueColumnName() : null,
				tg.pkColumnValue() != null && !tg.pkColumnValue().isEmpty() ? tg.pkColumnValue() : null,
				tg.allocationSize() != 50 ? tg.allocationSize() : null,
				tg.initialValue() != 0 ? tg.initialValue() : null);
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

	private <A extends Annotation> List<FieldDetails> getFieldsWithAnnotation(
			Class<A> annotationType) {
		List<FieldDetails> result = new ArrayList<>();
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(annotationType)) {
				result.add(field);
			}
		}
		return result;
	}
}
