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
package org.hibernate.tool.internal.reveng.models.exporter.cfg;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.cfg.Environment;
import org.hibernate.models.spi.ClassDetails;

/**
 * Generates {@code hibernate.cfg.xml} from a {@code List<ClassDetails>}.
 *
 * @author Koen Aers
 */
public class CfgXmlExporter {

	private final List<ClassDetails> entities;

	private CfgXmlExporter(List<ClassDetails> entities) {
		this.entities = entities;
	}

	public static CfgXmlExporter create(List<ClassDetails> entities) {
		return new CfgXmlExporter(entities);
	}

	public void export(Writer output, Properties properties) {
		PrintWriter pw = new PrintWriter(output);
		boolean ejb3 = Boolean.parseBoolean((String) properties.get("ejb3"));
		Map<Object, Object> props = new TreeMap<>(properties);
		String sfname = (String) props.get(Environment.SESSION_FACTORY_NAME);
		pw.println("""
				<?xml version="1.0" encoding="UTF-8"?>
				<!DOCTYPE hibernate-configuration PUBLIC\r
						"-//Hibernate/Hibernate Configuration DTD 3.0//EN"\r
						"https://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">\r
				<hibernate-configuration>""");
		pw.println("    <session-factory" + (sfname == null ? "" : " name=\"" + sfname + "\"") + ">");
		Map<Object, Object> ignoredProperties = new HashMap<>();
		ignoredProperties.put(Environment.SESSION_FACTORY_NAME, null);
		ignoredProperties.put(Environment.HBM2DDL_AUTO, "false");
		ignoredProperties.put("hibernate.temp.use_jdbc_metadata_defaults", null);
		ignoredProperties.put(
				Environment.TRANSACTION_COORDINATOR_STRATEGY,
				"org.hibernate.console.FakeTransactionManagerLookup");
		Set<Map.Entry<Object, Object>> set = props.entrySet();
		for (Map.Entry<Object, Object> element : set) {
			String key = (String) element.getKey();
			if (ignoredProperties.containsKey(key)) {
				Object ignoredValue = ignoredProperties.get(key);
				if (ignoredValue == null || element.getValue().equals(ignoredValue)) {
					continue;
				}
			}
			if (key.startsWith("hibernate.")) {
				pw.println("        <property name=\"" + key + "\">" + forXML(
						element.getValue().toString()) + "</property>");
			}
		}
		for (ClassDetails entity : entities) {
			if (!isSubclass(entity)) {
				dump(pw, ejb3, entity);
			}
		}
		pw.println("    </session-factory>\r\n" +
				"</hibernate-configuration>");
		pw.flush();
	}

	private void dump(PrintWriter pw, boolean useClass, ClassDetails entity) {
		String qualifiedName = entity.getClassName();
		if (useClass) {
			pw.println("<mapping class=\"" + qualifiedName + "\"/>");
		} else {
			pw.println("<mapping resource=\"" + qualifiedName.replace('.', '/') + ".hbm.xml\"/>");
		}
		for (ClassDetails child : entities) {
			if (isChildOf(child, entity)) {
				dump(pw, useClass, child);
			}
		}
	}

	private boolean isSubclass(ClassDetails entity) {
		ClassDetails superClass = entity.getSuperClass();
		return superClass != null && !"java.lang.Object".equals(superClass.getClassName());
	}

	private boolean isChildOf(ClassDetails child, ClassDetails parent) {
		ClassDetails superClass = child.getSuperClass();
		return superClass != null && parent.getClassName().equals(superClass.getClassName());
	}

	static String forXML(String text) {
		if (text == null) return null;
		final StringBuilder result = new StringBuilder();
		char[] chars = text.toCharArray();
		for (char character : chars) {
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>') {
				result.append("&gt;");
			} else {
				result.append(character);
			}
		}
		return result.toString();
	}

}
