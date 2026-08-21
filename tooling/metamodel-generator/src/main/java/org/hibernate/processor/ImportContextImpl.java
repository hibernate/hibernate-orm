/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.hibernate.processor.model.ImportContext;

import static java.lang.Character.isWhitespace;
import static java.lang.System.lineSeparator;


/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public class ImportContextImpl implements ImportContext {

	private final Set<String> imports = new TreeSet<>();
	private final Set<String> staticImports = new TreeSet<>();
	private final Map<String, String> simpleNames = new HashMap<>();

	private String basePackage = "";

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

	public ImportContextImpl(String basePackage) {
		this.basePackage = basePackage;
	}

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * <p>
	 * e.g.
	 * {@code java.util.Collection<org.marvel.Hulk>} imports {@code java.util.Collection} and returns {@code Collection}
	 * {@code org.marvel.Hulk[]} imports {@code org.marvel.Hulk} and returns {@code Hulk}
	 *
	 * @param typeExpression A type expression
	 *
	 * @return import string
	 */
	public String importType(String typeExpression) {
		String result = typeExpression;

		// strip off type annotations and '? super' or '? extends'
		String preamble = "";
		if ( result.startsWith( "@" ) ) {
			int index = result.lastIndexOf(' ');
			if ( index > 0 ) {
				preamble = result.substring( 0, index+1 );
				result = result.substring( index+1 );
			}
		}
		else if ( result.startsWith( "?" ) ) {
			int index = 1;
			while ( index < result.length() && isWhitespace( result.charAt( index ) ) ) {
				index++;
			}
			if ( index < result.length() ) {
				int nextIndex = -1;
				if ( result.substring( index ).startsWith( "extends" ) ) {
					nextIndex = index + 7;
				}
				else if ( result.substring( index ).startsWith( "super" ) ) {
					nextIndex = index + 5;
				}
				if ( nextIndex > 0 && nextIndex < result.length() && isWhitespace( result.charAt( nextIndex ) ) ) {
					index = nextIndex;
					while ( isWhitespace( result.charAt( index ) ) ) {
						index++;
					}
					preamble = result.substring( 0, index );
					result = importType( result.substring( index ) );
				}
			}
		}

		String appendices = "";
		if ( result.indexOf( '<' ) >= 0 ) {
			int startIndex = result.indexOf('<');
			int endIndex = result.lastIndexOf('>');
			appendices = '<' + importTypes( result.substring( startIndex + 1, endIndex ) ) + '>'
					+ result.substring( endIndex + 1 );
			result = result.substring( 0, startIndex );
		}
		else if ( result.indexOf( '[' ) >= 0 ) {
			int index = result.indexOf('[');
			appendices = result.substring( index );
			result = result.substring( 0, index );
		}
		else if ( result.endsWith( "..." ) ) {
			appendices = "...";
			int index = result.indexOf("...");
			result = result.substring( 0, index );
		}

		return ( preamble + unqualifyName( result ) + appendices )
				.replace( '$', '.' );
	}

	private String unqualifyName(String qualifiedName) {
		final String sourceQualifiedName = qualifiedName.replace( '$', '.' );
		final String simpleName = unqualify( qualifiedName );
		boolean canBeSimple;
		if ( simpleNames.containsKey( simpleName ) ) {
			String existing = simpleNames.get( simpleName );
			canBeSimple = existing.equals( sourceQualifiedName );
		}
		else {
			canBeSimple = true;
			simpleNames.put( simpleName, sourceQualifiedName );
			imports.add( sourceQualifiedName );
		}

		if ( inSamePackage( qualifiedName ) || inJavaLang( qualifiedName )
				|| canBeSimple && imports.contains( sourceQualifiedName ) ) {
			return unqualify( qualifiedName );
		}
		else {
			return qualifiedName;
		}
	}

	private String importTypes(String originalArgList) {
		StringBuilder argList = new StringBuilder();
		StringBuilder acc = new StringBuilder();
		StringTokenizer args = new StringTokenizer( originalArgList, "," );
		while ( args.hasMoreTokens() ) {
			if ( !acc.isEmpty() ) {
				acc.append( ',' );
			}
			acc.append( args.nextToken() );
			int nesting = 0;
			for ( int i = 0; i<acc.length(); i++ ) {
				switch ( acc.charAt(i) ) {
					case '<':
						nesting++;
						break;
					case '>':
						nesting--;
						break;
				}
			}
			if ( nesting == 0 ) {
				if ( !argList.isEmpty() ) {
					argList.append(',');
				}
				argList.append( importType( acc.toString() ) );
				acc.setLength( 0 );
			}
		}
		return argList.toString();
	}

	public String staticImport(String fqcn, String member) {
		final String local = fqcn + "." + member;
		imports.add( local );
		staticImports.add( local );
		return "*".equals(member) ? "" : member;
	}

	private boolean inDefaultPackage(String className) {
		return className.indexOf( '.' ) < 0;
	}

	private boolean isPrimitive(String className) {
		return PRIMITIVES.containsKey( className );
	}

	private boolean inSamePackage(String className) {
		return Objects.equals( qualifier( className ), basePackage );
	}

	private boolean inJavaLang(String className) {
		return "java.lang".equals( qualifier( className ) );
	}

	public String generateImports() {
		final StringBuilder builder = new StringBuilder();
		for ( String next : imports ) {
			// don't add automatically "imported" stuff
			if ( !isAutoImported( next ) ) {
				if ( staticImports.contains( next ) ) {
					builder.append( "import static " ).append( next ).append( ";" ).append( lineSeparator() );
				}
				else {
					builder.append( "import " ).append( next ).append( ";" ).append( lineSeparator() );
				}
			}
		}
		return builder.toString();
	}

	private boolean isAutoImported(String next) {
		return isPrimitive( next )
			|| inDefaultPackage( next )
			|| inJavaLang( next )
			|| inSamePackage( next );
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( qualifiedName.lastIndexOf( '.' ) + 1 );
	}

	public static String qualifier(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? "" : qualifiedName.substring( 0, loc );
	}
}
