/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.boot.jaxb.Origin;

/**
 * Thrown when a mapping is found to be invalid.
 * <p>
 * Similar to {@link MappingException}, but contains more information
 * about the path and type of mapping (e.g. file, resource or URL).
 *
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class InvalidMappingException extends MappingException {
	private final String path;
	private final String type;

	/**
	 * Constructs an InvalidMappingException using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 * @param cause The underlying cause
	 */
	public InvalidMappingException(String customMessage, String type, String path, Throwable cause) {
		super( customMessage, cause );
		this.type = type;
		this.path = path;
	}

	/**
	 * Constructs an {@code InvalidMappingException} using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param type The type of invalid mapping document
	 * @param path The path (type specific) of the invalid mapping document
	 */
	public InvalidMappingException(String customMessage, String type, String path) {
		super( customMessage );
		this.type=type;
		this.path=path;
	}

	/**
	 * Constructs an {@code InvalidMappingException} using the given information.
	 *
	 * @param customMessage The custom message explaining the exception condition
	 * @param origin The origin of the invalid mapping document
	 */
	public InvalidMappingException(String customMessage, Origin origin) {
		this( customMessage, origin.getType().getLegacyTypeText(), origin.getName() );
	}

	public String getType() {
		return type;
	}

	public String getPath() {
		return path;
	}
}
