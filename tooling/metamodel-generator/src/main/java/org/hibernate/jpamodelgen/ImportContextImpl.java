/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.jpamodelgen.model.ImportContext;


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
		if ( result.startsWith("@") || result.startsWith("?") ) {
			int index = result.lastIndexOf(' ');
			if ( index > 0 ) {
				preamble = result.substring( 0, index+1 );
				result = result.substring( index+1 );
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
		String[] args = originalArgList.split(",");
		StringBuilder argList = new StringBuilder();
		for ( String arg : args ) {
			if ( argList.length() > 0 ) {
				argList.append(',');
			}
			argList.append( importType( arg ) );
		}
		return argList.toString();
	}

	public String staticImport(String fqcn, String member) {
		String local = fqcn + "." + member;
		imports.add( local );
		staticImports.add( local );

		if ( member.equals( "*" ) ) {
			return "";
		}
		else {
			return member;
		}
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
		StringBuilder builder = new StringBuilder();

		for ( String next : imports ) {
			// don't add automatically "imported" stuff
			if ( !isAutoImported( next ) ) {
				if ( staticImports.contains( next ) ) {
					builder.append( "import static " ).append( next ).append( ";" ).append( System.lineSeparator() );
				}
				else {
					builder.append( "import " ).append( next ).append( ";" ).append( System.lineSeparator() );
				}
			}
		}

		if ( builder.indexOf( "$" ) >= 0 ) {
			return builder.toString();
		}
		return builder.toString();
	}

	private boolean isAutoImported(String next) {
		return isPrimitive( next ) || inDefaultPackage( next ) || inJavaLang( next ) || inSamePackage( next );
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
