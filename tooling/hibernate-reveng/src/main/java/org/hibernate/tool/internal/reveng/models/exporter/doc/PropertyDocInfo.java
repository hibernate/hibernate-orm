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
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Formula;
import org.hibernate.models.spi.FieldDetails;

/**
 * Adapts a {@link FieldDetails} into a shape compatible with the
 * documentation FreeMarker templates, which expect the old
 * {@code Property} API ({@code name}, {@code getValue()},
 * {@code getPropertyAccessorName()}).
 *
 * @author Koen Aers
 */
public class PropertyDocInfo {

	private final FieldDetails fieldDetails;
	private final ValueDocInfo value;

	public PropertyDocInfo(FieldDetails fieldDetails) {
		this.fieldDetails = fieldDetails;
		this.value = buildValueDocInfo(fieldDetails);
	}

	public String getName() {
		return fieldDetails.getName();
	}

	public ValueDocInfo getValue() {
		return value;
	}

	public String getPropertyAccessorName() {
		return "field";
	}

	/**
	 * Returns the Java type name for this property.
	 */
	public String getTypeName() {
		return fieldDetails.getType().determineRawClass().getName();
	}

	/**
	 * Returns {@code true} if this is a simple (non-relationship,
	 * non-embedded) property.
	 */
	public boolean isSimpleValue() {
		return !isRelationship() && !isComponent();
	}

	/**
	 * Returns {@code true} if this property represents a relationship
	 * ({@code @ManyToOne}, {@code @OneToOne}, {@code @OneToMany},
	 * {@code @ManyToMany}).
	 */
	public boolean isRelationship() {
		return fieldDetails.hasDirectAnnotationUsage(ManyToOne.class)
				|| fieldDetails.hasDirectAnnotationUsage(OneToOne.class)
				|| fieldDetails.hasDirectAnnotationUsage(OneToMany.class)
				|| fieldDetails.hasDirectAnnotationUsage(ManyToMany.class);
	}

	/**
	 * Returns {@code true} if this property is an embedded component
	 * ({@code @Embedded} or {@code @EmbeddedId}).
	 */
	public boolean isComponent() {
		return fieldDetails.hasDirectAnnotationUsage(Embedded.class)
				|| fieldDetails.hasDirectAnnotationUsage(EmbeddedId.class);
	}

	/**
	 * Returns the target entity/type name for relationship and component
	 * properties, or {@code null} for simple properties.
	 */
	public String getRelationshipTargetName() {
		if (isRelationship() || isComponent()) {
			return fieldDetails.getType().determineRawClass().getClassName();
		}
		return null;
	}

	FieldDetails getFieldDetails() {
		return fieldDetails;
	}

	private static ValueDocInfo buildValueDocInfo(FieldDetails field) {
		List<ColumnDocInfo> columns = new ArrayList<>();

		// Check for @Formula
		Formula formula = field.getDirectAnnotationUsage(Formula.class);
		if (formula != null) {
			columns.add(new ColumnDocInfo(formula.value(), true));
			return new ValueDocInfo(columns);
		}

		// Check for @JoinColumn (relationship fields)
		JoinColumn joinColumn = field.getDirectAnnotationUsage(JoinColumn.class);
		if (joinColumn != null && !joinColumn.name().isEmpty()) {
			columns.add(new ColumnDocInfo(joinColumn.name(), false));
			return new ValueDocInfo(columns);
		}

		// Check for @Column
		Column column = field.getDirectAnnotationUsage(Column.class);
		if (column != null && !column.name().isEmpty()) {
			columns.add(new ColumnDocInfo(column.name(), false));
		}
		else {
			// Default column name derived from field name
			columns.add(new ColumnDocInfo(field.getName(), false));
		}
		return new ValueDocInfo(columns);
	}
}
