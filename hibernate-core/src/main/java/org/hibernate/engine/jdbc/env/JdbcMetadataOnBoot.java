/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env;

import org.hibernate.cfg.JdbcSettings;

/**
 * Whether access to {@linkplain java.sql.DatabaseMetaData JDBC metadata} is allowed during bootstrap.
 * Typically, Hibernate accesses this metadata to understand the capabilities of the underlying
 * database to help minimize needed configuration.
 *
 * @apiNote The default value is {@linkplain #ALLOW}.
 *
 * @see JdbcSettings#ALLOW_METADATA_ON_BOOT
 *
 * @author Steve Ebersole
 */
public enum JdbcMetadataOnBoot {
	/**
	 * Access to the {@linkplain java.sql.DatabaseMetaData JDBC metadata} is disallowed.
	 * At a bare minimum, this requires specifying the {@linkplain JdbcSettings#DIALECT dialect}
	 * or {@linkplain JdbcSettings#JAKARTA_HBM2DDL_DB_NAME database} being used.
	 * Specifying the {@linkplain JdbcSettings#JAKARTA_HBM2DDL_DB_VERSION database version} is
	 * recommended as well.
	 *
	 * @apiNote The specified Dialect may also provide defaults into the "explicit" settings.
	 */
	DISALLOW,
	/**
	 * Access to the {@linkplain java.sql.DatabaseMetaData JDBC metadata} is allowed.
	 *
	 * @apiNote This is the default.
	 * @implNote When errors occur accessing the {@linkplain java.sql.DatabaseMetaData JDBC metadata},
	 * implicit values will be used as needed.
	 */
	ALLOW,
	/**
	 * Access to the {@linkplain java.sql.DatabaseMetaData JDBC metadata} is required.
	 *
	 * @implNote Functions like {@linkplain #ALLOW}, except that errors which occur when accessing the
	 * JDBC metadata will be propagated back to the application.
	 */
	REQUIRE
}
