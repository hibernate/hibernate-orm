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
package org.hibernate.tool.internal.reveng.models.exporter.generic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.models.spi.FieldDetails;

/**
 * Adapter that provides the {@code c2j} template variable API
 * for backwards compatibility with legacy FreeMarker templates.
 * Replaces the old {@code Cfg2JavaTool} class.
 *
 * @author Koen Aers
 */
public class Cfg2JavaToolAdapter {

	private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
			"abstract", "assert", "boolean", "break", "byte", "case", "catch",
			"char", "class", "const", "continue", "default", "do", "double",
			"else", "enum", "extends", "final", "finally", "float", "for",
			"goto", "if", "implements", "import", "instanceof", "int",
			"interface", "long", "native", "new", "package", "private",
			"protected", "public", "return", "short", "static", "strictfp",
			"super", "switch", "synchronized", "this", "throw", "throws",
			"transient", "try", "void", "volatile", "while"
	));

	public String keyWordCheck(String name) {
		if (name == null) return name;
		if (JAVA_KEYWORDS.contains(name)) {
			return name + "_";
		}
		return name;
	}

	public String asParameterList(List<POJOAdapter.PropertyAdapter> properties,
								  boolean jdk5, POJOAdapter pojo) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < properties.size(); i++) {
			if (i > 0) sb.append(", ");
			POJOAdapter.PropertyAdapter prop = properties.get(i);
			sb.append(prop.getTypeName()).append(" ").append(keyWordCheck(prop.getName()));
		}
		return sb.toString();
	}

	public String asArgumentList(List<POJOAdapter.PropertyAdapter> properties) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < properties.size(); i++) {
			if (i > 0) sb.append(", ");
			sb.append(keyWordCheck(properties.get(i).getName()));
		}
		return sb.toString();
	}

	public String toJavaDoc(String text, int indent) {
		if (text == null || text.isEmpty()) return "";
		return text;
	}

	public String getMetaAsString(FieldDetails field, String metaKey) {
		return "";
	}
}
