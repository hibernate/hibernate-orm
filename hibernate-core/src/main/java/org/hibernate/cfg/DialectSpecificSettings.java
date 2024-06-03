/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	 *
	 * @settingDefault {@code false}
	 */
	public static final String ORACLE_AUTONOMOUS_DATABASE = "hibernate.dialect.oracle.is_autonomous";

	/**
	 * Specifies whether this database's {@code MAX_STRING_SIZE} is set to {@code EXTENDED}.
	 *
	 * @settingDefault {@code false}
	 */
	public static final String ORACLE_EXTENDED_STRING_SIZE = "hibernate.dialect.oracle.extended_string_size";

	/**
	 * Specifies whether this database is accessed using a database service protected by Application Continuity.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/jjdbc/application-continuity.html">Application Continuity for Java</a>
	 */
	public static final String ORACLE_APPLICATION_CONTINUITY = "hibernate.dialect.oracle.application_continuity";

	/**
	 * Specifies whether this database's {@code ansinull} setting is enabled.
	 *
	 * @settingDefault {@code false}
	 */
	public static final String SYBASE_ANSI_NULL = "hibernate.dialect.sybase.extended_string_size";

	/**
	 * Specifies the bytes per character to use based on the database's configured
	 * <a href="https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html">charset</a>.
	 *
	 * @settingDefault {@code 4}
	 */
	public static final String MYSQL_BYTES_PER_CHARACTER = "hibernate.dialect.mysql.bytes_per_character";

	/**
	 * Specifies whether the {@code NO_BACKSLASH_ESCAPES} sql mode is enabled.
	 *
	 * @settingDefault {@code false}
	 */
	public static final String MYSQL_NO_BACKSLASH_ESCAPES = "hibernate.dialect.mysql.no_backslash_escapes";

	/**
	 * Specifies a custom CockroachDB version string. The expected format of the string is
	 * the one returned from the {@code version()} function, e.g.:
	 * {@code "CockroachDB CCL v23.1.8 (x86_64-pc-linux-gnu, built 2023/08/04 18:11:44, go1.19.10)"}
	 */
	public static final String COCKROACH_VERSION_STRING = "hibernate.dialect.cockroach.version_string";

	/**
	 * Specifies the LOB prefetch size. LOBs larger than this value will be read into memory as the HANA JDBC driver closes
	 * the LOB when the result set is closed.
	 *
	 * @settingDefault {@code 1024}
	 */
	public static final String HANA_MAX_LOB_PREFETCH_SIZE = "hibernate.dialect.hana.max_lob_prefetch_size";

}
