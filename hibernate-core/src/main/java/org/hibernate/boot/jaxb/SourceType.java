/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb;

/**
 * From what type of source did we obtain the data
 *
 * @author Steve Ebersole
 */
public enum SourceType {
	RESOURCE,
	FILE,
	INPUT_STREAM,
	URL,
	STRING,
	DOM,
	JAR,
	ANNOTATION,
	OTHER;

	public String getLegacyTypeText() {
		return switch ( this ) {
			case RESOURCE -> "resource";
			case FILE -> "file";
			case INPUT_STREAM -> "input stream";
			case URL -> "URL";
			case STRING -> "string";
			case DOM -> "xml";
			case JAR -> "jar";
			case ANNOTATION -> "annotation";
			case OTHER -> "other";
		};
	}
}
