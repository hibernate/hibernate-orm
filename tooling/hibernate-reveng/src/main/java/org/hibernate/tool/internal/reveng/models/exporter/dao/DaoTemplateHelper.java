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
package org.hibernate.tool.internal.reveng.models.exporter.dao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import org.hibernate.annotations.NaturalId;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.internal.export.java.ImportContext;

/**
 * Template-friendly helper for generating DAO Home classes from
 * {@link ClassDetails} metadata.
 *
 * @author Koen Aers
 */
public class DaoTemplateHelper {

	private final ClassDetails classDetails;
	private final ImportContext importContext;
	private final boolean ejb3;
	private final String sessionFactoryName;

	public DaoTemplateHelper(ClassDetails classDetails, ImportContext importContext,
							 boolean ejb3, String sessionFactoryName) {
		this.classDetails = classDetails;
		this.importContext = importContext;
		this.ejb3 = ejb3;
		this.sessionFactoryName = sessionFactoryName;
	}

	// --- Package / class ---

	public String getPackageDeclaration() {
		String pkg = getPackageName();
		if (pkg != null && !pkg.isEmpty()) {
			return "package " + pkg + ";";
		}
		return "";
	}

	public String getPackageName() {
		String className = classDetails.getClassName();
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	public String getDeclarationName() {
		return classDetails.getName();
	}

	public String getQualifiedDeclarationName() {
		return classDetails.getClassName();
	}

	public String getEntityName() {
		return classDetails.getClassName();
	}

	public String generateImports() {
		return importContext.generateImports();
	}

	public String importType(String fqcn) {
		return importContext.importType(fqcn);
	}

	// --- Mode ---

	public boolean isEjb3() {
		return ejb3;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	// --- Identifier ---

	public boolean hasIdentifier() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return true;
			}
		}
		return false;
	}

	public String getIdTypeName() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return importType(field.getType().determineRawClass().getClassName());
			}
		}
		return "Object";
	}

	// --- Natural ID ---

	public boolean hasNaturalId() {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(NaturalId.class)) {
				return true;
			}
		}
		return false;
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

	public String getNaturalIdParameterList() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (FieldDetails field : getNaturalIdFields()) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(importType(field.getType().determineRawClass().getClassName()));
			sb.append(" ");
			sb.append(field.getName());
			first = false;
		}
		return sb.toString();
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

	public List<NamedQueryInfo> getEntityNamedQueries() {
		String entityName = getEntityName();
		String prefix = entityName + ".";
		List<NamedQueryInfo> result = new ArrayList<>();
		for (NamedQueryInfo nq : getNamedQueries()) {
			if (nq.name().startsWith(prefix)) {
				result.add(nq);
			}
		}
		return result;
	}

	public String unqualify(String name) {
		int lastDot = name.lastIndexOf('.');
		return lastDot >= 0 ? name.substring(lastDot + 1) : name;
	}

	private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":(\\w+)");

	public List<String> getQueryParameterNames(NamedQueryInfo query) {
		Set<String> params = new LinkedHashSet<>();
		Matcher matcher = NAMED_PARAM_PATTERN.matcher(query.query());
		while (matcher.find()) {
			params.add(matcher.group(1));
		}
		return new ArrayList<>(params);
	}

	public String getQueryParameterList(NamedQueryInfo query) {
		List<String> params = getQueryParameterNames(query);
		if (params.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(importType("java.lang.Object"));
			sb.append(" ");
			sb.append(params.get(i));
		}
		return sb.toString();
	}

	public record NamedQueryInfo(String name, String query) {}
}
