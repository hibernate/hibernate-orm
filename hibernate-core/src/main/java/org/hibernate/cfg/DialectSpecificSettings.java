/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * Settings used as fallback to configure aspects of specific {@link org.hibernate.dialect.Dialect}s
 * when the boot process does not have access to a {@link java.sql.DatabaseMetaData} object or
 * its underlying JDBC {@link java.sql.Connection}.
 *
 * @author Marco Belladelli
 * @author Loïc Lefèvre
 */
public interface DialectSpecificSettings {
	/**
	 * Specifies whether this database is running on an Autonomous Database Cloud Service.
	 * <p>
	 * Ignored if Hibernate is able to determine this by querying the Oracle server at startup.
	 *
	 * @settingDefault {@code false}
	 */
	String ORACLE_AUTONOMOUS_DATABASE = "hibernate.dialect.oracle.is_autonomous";

	/**
	 * Specifies whether {@code MAX_STRING_SIZE} is set to {@code EXTENDED} on Oracle.
	 * <p>
	 * Ignored if Hibernate is able to determine the value of {@code MAX_STRING_SIZE} by
	 * querying the Oracle server at startup.
	 *
	 * @settingDefault {@code false}
	 */
	String ORACLE_EXTENDED_STRING_SIZE = "hibernate.dialect.oracle.extended_string_size";

	/**
	 * Specifies whether this database is accessed using a database service protected by Application Continuity.
	 * <p>
	 * Ignored if Hibernate is able to determine this by querying the Oracle server at startup.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/jjdbc/application-continuity.html">Application Continuity for Java</a>
	 */
	String ORACLE_APPLICATION_CONTINUITY = "hibernate.dialect.oracle.application_continuity";

	/**
	 * Specifies whether the dialect should use the binary IEEE Oracle SQL types {@code binary_float}/{@code binary_double}
	 * over {@code float(p)}/{@code real}/{@code double precision} when generating DDL or SQL casts for float types.
	 *
	 * @settingDefault {@code true}
	 * @since 7.0
	 */
	String ORACLE_USE_BINARY_FLOATS = "hibernate.dialect.oracle.use_binary_floats";

	/**
	 * Specifies whether usage of the Oracle JSON binary format (also known as OSON) should be disabled.
	 * <p>
	 * Starting in 21c, if the ojdbc-provider-jackson-oson extension is available, JSON data in an oracle
	 * database is stored using the OSON binary format. This setting can be used to fallback to the old implementation
	 * based on String serialization.
	 *
	 * @settingDefault {@code false}
	 * @since 7.0
	 *
	 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-oracle-database.html">Orace OSON format</a>
	 * @see <a href="https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-jackson-oson/README.md">Jackson OSON provider</a>
	 */
	String ORACLE_OSON_DISABLED = "hibernate.dialect.oracle.oson_format_disabled";

	/**
	 * Specifies whether the {@code ansinull} setting is enabled on Sybase.
	 * <p>
	 * Ignored if Hibernate is able to determine the value of {@code ansinull}
	 * by querying the server {@code @@options} at startup.
	 *
	 * @settingDefault {@code false}
	 */
	String SYBASE_ANSI_NULL = "hibernate.dialect.sybase.extended_string_size";

	/**
	 * Specifies the maximum page size on Sybase.
	 * <p>
	 * Ignored if Hibernate is able to determine the page size by querying the
	 * server {@code @@maxpagesize} at startup.
	 *
	 * @settingDefault {@value org.hibernate.dialect.SybaseASEDialect#MAX_PAGE_SIZE}
	 */
	String SYBASE_PAGE_SIZE = "hibernate.dialect.sybase.page_size";

	/**
	 * Specifies the bytes per character to use based on the database's configured
	 * <a href="https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html">charset</a>.
	 * <p>
	 * Ignored if Hibernate is able to determine the character set by querying the
	 * server {@code @@character_set_database} at startup.
	 *
	 * @settingDefault {@code 4}
	 */
	String MYSQL_BYTES_PER_CHARACTER = "hibernate.dialect.mysql.bytes_per_character";

	/**
	 * Specifies whether the {@code NO_BACKSLASH_ESCAPES} sql mode is enabled.
	 * <p>
	 * Ignored if Hibernate is able to determine this by querying the server
	 * {@code @@sql_mode} at startup.
	 *
	 * @settingDefault {@code false}
	 */
	String MYSQL_NO_BACKSLASH_ESCAPES = "hibernate.dialect.mysql.no_backslash_escapes";

	/**
	 * Specifies a custom CockroachDB version string. The expected format of the string is
	 * the one returned from the {@code version()} function, e.g.:
	 * {@code "CockroachDB CCL v23.1.8 (x86_64-pc-linux-gnu, built 2023/08/04 18:11:44, go1.19.10)"}
	 * <p>
	 * Ignored if Hibernate is able to obtain the version string by querying the server at startup.
	 */
	String COCKROACH_VERSION_STRING = "hibernate.dialect.cockroach.version_string";

	/**
	 * Specifies the compatibility level of the SQL Server database as returned by
	 * {@code select compatibility_level from sys.databases}.
	 * <p>
	 * The number has three digits: the first two digits are the major version,
	 * the last digit is the minor version.
	 * <p>
	 * Ignored if Hibernate is able to determine this by querying the {@code sys.databases}
	 * table at startup.
	 */
	String SQL_SERVER_COMPATIBILITY_LEVEL = "hibernate.dialect.sqlserver.compatibility_level";

	/**
	 * Specifies the LOB prefetch size. LOBs larger than this value will be read into
	 * memory as the HANA JDBC driver closes the LOB when the result set is closed.
	 * <p>
	 * Ignored if Hibernate is able to determine this by querying the server at startup.
	 *
	 * @settingDefault {@code 1024}
	 */
	String HANA_MAX_LOB_PREFETCH_SIZE = "hibernate.dialect.hana.max_lob_prefetch_size";

}
