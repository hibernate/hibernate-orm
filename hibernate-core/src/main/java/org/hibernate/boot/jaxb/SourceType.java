/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
