/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.internal.util.StringHelper;

public class ImportContext {

	private final Set<String> imports = new TreeSet<>();
	private final Set<String> staticImports = new TreeSet<>();
	private final Map<String, String> simpleNames = new HashMap<>();
	private final String basePackage;

	private static final Map<String, String> PRIMITIVES = new HashMap<>();
	static {
		PRIMITIVES.put( "char", "Character" );
		PRIMITIVES.put( "byte", "Byte" );
		PRIMITIVES.put( "short", "Short" );
		PRIMITIVES.put( "int", "Integer" );
		PRIMITIVES.put( "long", "Long" );
		PRIMITIVES.put( "boolean", "Boolean" );
		PRIMITIVES.put( "float", "Float" );
		PRIMITIVES.put( "double", "Double" );
	}

	public ImportContext(String basePackage) {
		this.basePackage = basePackage;
	}

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * <p>
	 * e.g.
	 * {@code java.util.Collection<org.marvel.Hulk>} imports java.util.Collection and returns Collection
	 * org.marvel.Hulk[] imports org.marvel.Hulk and returns Hulk
	 *
	 *
	 * @return import string
	 */
	public String importType(String fqcn) {
		String result = fqcn;

		String additionalTypePart = null;
		if(fqcn.indexOf('<')>=0) {
			additionalTypePart = result.substring(fqcn.indexOf('<'));
			result = result.substring(0,fqcn.indexOf('<'));
			fqcn = result;
		}
		else if(fqcn.indexOf('[')>=0) {
			additionalTypePart = result.substring(fqcn.indexOf('['));
			result = result.substring(0,fqcn.indexOf('['));
			fqcn = result;
		}

		String pureFqcn = fqcn.replace( '$', '.' );

		boolean canBeSimple = true;

		String simpleName = StringHelper.unqualify(fqcn);
		if(simpleNames.containsKey(simpleName)) {
			String existingFqcn = simpleNames.get(simpleName);
			canBeSimple = existingFqcn.equals(pureFqcn);
		}
		else {
			simpleNames.put(simpleName, pureFqcn);
			imports.add( pureFqcn );
		}

		if ( inSamePackage(fqcn) || (imports.contains( pureFqcn ) && canBeSimple) ) {
			result = StringHelper.unqualify( result );
		}
		else if ( inJavaLang( fqcn ) ) {
			result = result.substring( "java.lang.".length() );
		}

		if(additionalTypePart!=null) {
			result = result + additionalTypePart;
		}

		result = result.replace( '$', '.' );
		return result;
	}

	public String staticImport(String fqcn, String member) {
		String local = fqcn + "." + member;
		imports.add(local);
		staticImports.add(local);

		if(member.equals("*")) {
			return "";
		}
		else {
			return member;
		}
	}

	public String generateImports() {
		StringBuilder buf = new StringBuilder();
		for (String next : imports) {
			if (!(isPrimitive(next) || inDefaultPackage(next) || inJavaLang(next) || inSamePackage(next))) {
				if (staticImports.contains(next)) {
					buf.append("import static ").append(next).append(";\r\n");
				}
				else {
					buf.append("import ").append(next).append(";\r\n");
				}
			}
		}
		return buf.toString();
	}

	private boolean inDefaultPackage(String className) {
		return !className.contains(".");
	}

	private boolean isPrimitive(String className) {
		return PRIMITIVES.containsKey( className );
	}

	private boolean inSamePackage(String className) {
		return StringHelper.qualifier( className ).equals(basePackage);
	}

	private boolean inJavaLang(String className) {
		return "java.lang".equals( StringHelper.qualifier( className ) );
	}
}
