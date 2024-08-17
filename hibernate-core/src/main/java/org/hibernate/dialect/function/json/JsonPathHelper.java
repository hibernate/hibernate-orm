/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.QueryException;

public class JsonPathHelper {

	public static List<JsonPathElement> parseJsonPathElements(String jsonPath) {
		if ( jsonPath.charAt( 0 ) != '$' || jsonPath.charAt( 1 ) != '.' ) {
			throw new QueryException( "Json path expression expression emulation only supports absolute paths i.e. must start with a '$.' but got: " + jsonPath );
		}
		final var jsonPathElements = new ArrayList<JsonPathElement>();
		int startIndex = 2;
		int dotIndex;

		try {
			while ( ( dotIndex = jsonPath.indexOf( '.', startIndex ) ) != -1 ) {
				parseAttribute( jsonPath, startIndex, dotIndex, jsonPathElements );
				startIndex = dotIndex + 1;
			}
			parseAttribute( jsonPath, startIndex, jsonPath.length(), jsonPathElements );
		}
		catch (Exception ex) {
			throw new QueryException( "Can't emulate non-simple json path expression: " + jsonPath, ex );
		}
		return jsonPathElements;
	}

	private static void parseAttribute(String jsonPath, int startIndex, int endIndex, ArrayList<JsonPathElement> jsonPathElements) {
		final int bracketIndex = jsonPath.indexOf( '[', startIndex );
		if ( bracketIndex != -1 && bracketIndex < endIndex ) {
			jsonPathElements.add( new JsonAttribute( jsonPath.substring( startIndex, bracketIndex ) ) );
			parseBracket( jsonPath, bracketIndex, endIndex, jsonPathElements );
		}
		else {
			jsonPathElements.add( new JsonAttribute( jsonPath.substring( startIndex, endIndex ) ) );
		}
	}

	private static void parseBracket(String jsonPath, int bracketStartIndex, int endIndex, ArrayList<JsonPathElement> jsonPathElements) {
		assert jsonPath.charAt( bracketStartIndex ) == '[';
		final int bracketEndIndex = jsonPath.lastIndexOf( ']', endIndex );
		if ( bracketEndIndex < bracketStartIndex ) {
			throw new QueryException( "Can't emulate non-simple json path expression: " + jsonPath );
		}
		final int index = Integer.parseInt( jsonPath, bracketStartIndex + 1, bracketEndIndex, 10 );
		jsonPathElements.add( new JsonIndexAccess( index ) );
	}

	public sealed interface JsonPathElement {}
	public record JsonAttribute(String attribute) implements JsonPathElement {}
	public record JsonIndexAccess(int index) implements JsonPathElement {}
}
