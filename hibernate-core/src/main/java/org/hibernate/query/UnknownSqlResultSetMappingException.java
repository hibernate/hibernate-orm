/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;

import org.hibernate.MappingException;

import jakarta.persistence.NamedNativeQuery;

/**
 * Indicates a request for named ResultSet mapping which could not be found
 *
 * @see NamedNativeQuery#resultSetMapping()
 * @see org.hibernate.Session#createNativeQuery(String, String)
 * @see org.hibernate.Session#createNativeQuery(String, String, Class)
 *
 * @author Steve Ebersole
 */
public class UnknownSqlResultSetMappingException extends MappingException {
	@Nonnull
	private final String unknownSqlResultSetMappingName;

	public UnknownSqlResultSetMappingException(@Nonnull String unknownSqlResultSetMappingName) {
		super( "The given SqlResultSetMapping name [" + unknownSqlResultSetMappingName + "] is unknown" );
		this.unknownSqlResultSetMappingName = unknownSqlResultSetMappingName;
	}

	@Nonnull
	public String getUnknownSqlResultSetMappingName() {
		return unknownSqlResultSetMappingName;
	}
}
