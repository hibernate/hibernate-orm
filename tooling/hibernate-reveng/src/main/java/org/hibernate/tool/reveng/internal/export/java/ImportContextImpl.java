/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.hibernate.internal.util.StringHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ImportContextImpl implements ImportContext {

	Set<String> imports = new TreeSet<String>();
	Set<String> staticImports = new TreeSet<String>();
	Map<String, String> simpleNames = new HashMap<String, String>();

	String basePackage = "";

	// TODO: share this somehow, redundant from Cfg2JavaTool
	private static final Map<String, String> PRIMITIVES = new HashMap<String, String>();
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

	public ImportContextImpl(String basePackage) {
		this.basePackage = basePackage;
	}

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * <p>
	 * e.g.
	 * java.util.Collection<org.marvel.Hulk> imports java.util.Collection and returns Collection
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
		} else if(fqcn.indexOf('[')>=0) {
			additionalTypePart = result.substring(fqcn.indexOf('['));
			result = result.substring(0,fqcn.indexOf('['));
			fqcn = result;
		}

		String pureFqcn = fqcn.replace( '$', '.' );

		boolean canBeSimple = true;


		String simpleName = StringHelper.unqualify(fqcn);
		if(simpleNames.containsKey(simpleName)) {
			String existingFqcn = (String) simpleNames.get(simpleName);
			canBeSimple = existingFqcn.equals(pureFqcn);
		} else {
			simpleNames.put(simpleName, pureFqcn);
			imports.add( pureFqcn );
		}


		if ( inSamePackage(fqcn) || (imports.contains( pureFqcn ) && canBeSimple) ) {
			result = StringHelper.unqualify( result ); // dequalify
		} else if ( inJavaLang( fqcn ) ) {
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
		} else {
			return member;
		}
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

	public String generateImports() {
		StringBuilder buf = new StringBuilder();
		for (String next : imports) {
			if (!(isPrimitive(next) || inDefaultPackage(next) || inJavaLang(next) || inSamePackage(next))) {
				if (staticImports.contains(next)) {
					buf.append("import static ").append(next).append(";\r\n");
				} else {
					buf.append("import ").append(next).append(";\r\n");
				}
			}
		}

		if(buf.indexOf( "$" )>=0) {
			return buf.toString();
		}
		return buf.toString();
	}
}
