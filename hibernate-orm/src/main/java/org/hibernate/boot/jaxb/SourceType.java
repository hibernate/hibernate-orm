/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.boot.jaxb;

/**
 * From what type of source did we obtain the data
 *
 * @author Steve Ebersole
 */
public enum SourceType {
	RESOURCE( "resource" ),
	FILE( "file" ),
	INPUT_STREAM( "input stream" ),
	URL( "URL" ),
	STRING( "string" ),
	DOM( "xml" ),
	JAR( "jar" ),
	ANNOTATION( "annotation" ),
	OTHER( "other" );

	private final String legacyTypeText;

	SourceType(String legacyTypeText) {
		this.legacyTypeText = legacyTypeText;
	}

	public String getLegacyTypeText() {
		return legacyTypeText;
	}
}
