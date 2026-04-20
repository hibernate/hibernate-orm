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
package org.hibernate.tool.internal.exporter.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ManyToOne;

import org.hibernate.models.spi.FieldDetails;

import org.hibernate.tool.internal.exporter.entity.TemplateHelper.FullConstructorProperty;

class ConstructorHelper {

	private final TemplateHelper templateHelper;

	ConstructorHelper(TemplateHelper templateHelper) {
		this.templateHelper = templateHelper;
	}

	boolean needsFullConstructor() {
		return !getFullConstructorProperties().isEmpty();
	}

	List<FullConstructorProperty> getFullConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		addCompositeId(props);
		addBasicFieldsFull(props);
		addManyToOneFields(props);
		addOneToOneFields(props);
		addOneToManyFields(props);
		addManyToManyFields(props);
		addEmbeddedFields(props);
		return props;
	}

	boolean needsMinimalConstructor() {
		int minTotal = getMinimalConstructorProperties().size()
				+ getSuperclassMinimalConstructorProperties().size();
		int fullTotal = getFullConstructorProperties().size()
				+ getSuperclassFullConstructorProperties().size();
		return minTotal > 0 && minTotal < fullTotal;
	}

	List<FullConstructorProperty> getMinimalConstructorProperties() {
		List<FullConstructorProperty> props = new ArrayList<>();
		addCompositeId(props);
		addBasicFieldsMinimal(props);
		addManyToOneFieldsMinimal(props);
		addEmbeddedFields(props);
		return props;
	}

	String getMinimalConstructorParameterList() {
		return buildParameterList(
				getSuperclassMinimalConstructorProperties(),
				getMinimalConstructorProperties());
	}

	String getFullConstructorParameterList() {
		return buildParameterList(
				getSuperclassFullConstructorProperties(),
				getFullConstructorProperties());
	}

	List<FullConstructorProperty> getSuperclassFullConstructorProperties() {
		if (!templateHelper.isSubclass() || templateHelper.isSuperclassInterface()) {
			return Collections.emptyList();
		}
		return createSuperclassConstructorHelper().getFullConstructorProperties();
	}

	String getSuperclassFullConstructorArgumentList() {
		return buildArgumentList(getSuperclassFullConstructorProperties());
	}

	List<FullConstructorProperty> getSuperclassMinimalConstructorProperties() {
		if (!templateHelper.isSubclass() || templateHelper.isSuperclassInterface()) {
			return Collections.emptyList();
		}
		return createSuperclassConstructorHelper().getMinimalConstructorProperties();
	}

	String getSuperclassMinimalConstructorArgumentList() {
		return buildArgumentList(getSuperclassMinimalConstructorProperties());
	}

	// --- Full constructor field collectors ---

	private void addCompositeId(List<FullConstructorProperty> props) {
		FieldDetails cid = templateHelper.getCompositeIdField();
		if (cid != null) {
			props.add(new FullConstructorProperty(
					templateHelper.getJavaTypeName(cid),
					templateHelper.getFieldName(cid)));
		}
	}

	private void addBasicFieldsFull(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getBasicFields()) {
			if (!templateHelper.isVersion(field)
					&& templateHelper.isGenProperty(field)
					&& !templateHelper.hasFormula(field)
					&& !isGeneratedId(field)) {
				props.add(new FullConstructorProperty(
						templateHelper.getJavaTypeName(field),
						templateHelper.getFieldName(field)));
			}
		}
	}

	private void addManyToOneFields(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getManyToOneFields()) {
			props.add(new FullConstructorProperty(
					templateHelper.getJavaTypeName(field),
					templateHelper.getFieldName(field)));
		}
	}

	private void addOneToOneFields(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getOneToOneFields()) {
			props.add(new FullConstructorProperty(
					templateHelper.getJavaTypeName(field),
					templateHelper.getFieldName(field)));
		}
	}

	private void addOneToManyFields(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getOneToManyFields()) {
			props.add(new FullConstructorProperty(
					templateHelper.getCollectionTypeName(field),
					templateHelper.getFieldName(field)));
		}
	}

	private void addManyToManyFields(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getManyToManyFields()) {
			props.add(new FullConstructorProperty(
					templateHelper.getCollectionTypeName(field),
					templateHelper.getFieldName(field)));
		}
	}

	private void addEmbeddedFields(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getEmbeddedFields()) {
			props.add(new FullConstructorProperty(
					templateHelper.getJavaTypeName(field),
					templateHelper.getFieldName(field)));
		}
	}

	// --- Minimal constructor field collectors ---

	private void addBasicFieldsMinimal(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getBasicFields()) {
			if (templateHelper.isVersion(field)
					|| !templateHelper.isGenProperty(field)
					|| templateHelper.hasFieldDefaultValue(field)
					|| templateHelper.hasFormula(field)) {
				continue;
			}
			if (templateHelper.isPrimaryKey(field)) {
				GeneratedValue gv = templateHelper.fieldGetAnnotation(
						field, GeneratedValue.class);
				if (gv != null) {
					continue;
				}
				props.add(new FullConstructorProperty(
						templateHelper.getJavaTypeName(field),
						templateHelper.getFieldName(field)));
			} else {
				Column col = templateHelper.fieldGetAnnotation(field, Column.class);
				if (col != null && !col.nullable()) {
					props.add(new FullConstructorProperty(
							templateHelper.getJavaTypeName(field),
							templateHelper.getFieldName(field)));
				}
			}
		}
	}

	private void addManyToOneFieldsMinimal(List<FullConstructorProperty> props) {
		for (FieldDetails field : templateHelper.getManyToOneFields()) {
			ManyToOne m2o = templateHelper.fieldGetAnnotation(field, ManyToOne.class);
			if (m2o != null && !m2o.optional()) {
				props.add(new FullConstructorProperty(
						templateHelper.getJavaTypeName(field),
						templateHelper.getFieldName(field)));
			}
		}
	}

	// --- Private helpers ---

	private boolean isGeneratedId(FieldDetails field) {
		return templateHelper.isPrimaryKey(field)
				&& templateHelper.fieldHasAnnotation(field, GeneratedValue.class);
	}

	private ConstructorHelper createSuperclassConstructorHelper() {
		TemplateHelper superHelper = new TemplateHelper(
				templateHelper.getClassDetails().getSuperClass(),
				templateHelper);
		return new ConstructorHelper(superHelper);
	}

	private String buildParameterList(
			List<FullConstructorProperty> superProps,
			List<FullConstructorProperty> ownProps) {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : superProps) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		for (FullConstructorProperty prop : ownProps) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.typeName()).append(" ").append(prop.fieldName());
		}
		return sb.toString();
	}

	private String buildArgumentList(List<FullConstructorProperty> props) {
		StringBuilder sb = new StringBuilder();
		for (FullConstructorProperty prop : props) {
			if (!sb.isEmpty()) sb.append(", ");
			sb.append(prop.fieldName());
		}
		return sb.toString();
	}
}
