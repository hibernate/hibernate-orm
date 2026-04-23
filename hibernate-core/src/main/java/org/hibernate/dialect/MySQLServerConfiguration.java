/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.Internal;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_BYTES_PER_CHARACTER;
import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_FOREIGN_KEYS_IN_SERVER;
import static org.hibernate.cfg.DialectSpecificSettings.MYSQL_NO_BACKSLASH_ESCAPES;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;

/**
 * Utility class that extract some initial configuration from the database
 * for {@link MySQLDialect} and related dialects.
 *
 * @author Marco Belladelli
 */
@Internal
public class MySQLServerConfiguration {
	private final int bytesPerCharacter;
	private final boolean noBackslashEscapesEnabled;
	private final String storageEngine;
	private final boolean foreignKeysInServer;

	public MySQLServerConfiguration(int bytesPerCharacter, boolean noBackslashEscapesEnabled) {
		this.bytesPerCharacter = bytesPerCharacter;
		this.noBackslashEscapesEnabled = noBackslashEscapesEnabled;
		this.storageEngine = null;
		this.foreignKeysInServer = false;
	}

	public MySQLServerConfiguration(int bytesPerCharacter, boolean noBackslashEscapesEnabled, String storageEngine) {
		this.bytesPerCharacter = bytesPerCharacter;
		this.noBackslashEscapesEnabled = noBackslashEscapesEnabled;
		this.storageEngine = storageEngine;
		this.foreignKeysInServer = false;
	}

	private MySQLServerConfiguration(
			int bytesPerCharacter,
			boolean noBackslashEscapesEnabled,
			String storageEngine,
			boolean foreignKeysInServer) {
		this.bytesPerCharacter = bytesPerCharacter;
		this.noBackslashEscapesEnabled = noBackslashEscapesEnabled;
		this.storageEngine = storageEngine;
		this.foreignKeysInServer = foreignKeysInServer;
	}

	public int getBytesPerCharacter() {
		return bytesPerCharacter;
	}

	public boolean isNoBackslashEscapesEnabled() {
		return noBackslashEscapesEnabled;
	}

	public String getConfiguredStorageEngine() {
		return storageEngine;
	}

	/**
	 * When {@code @@innodb_native_foreign_keys} is OFF, InnoDB delegates
	 * foreign-key handling to the server-level implementation, which does
	 * not support circular cascade deletes.
	 */
	public boolean isForeignKeysInServer() {
		return foreignKeysInServer;
	}

	static Integer getBytesPerCharacter(String characterSet) {
		final int collationIndex = characterSet.indexOf( '_' );
		// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
		return switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
			case "utf16", "utf16le", "utf32", "utf8mb4", "gb18030" -> 4;
			case "utf8", "utf8mb3", "eucjpms", "ujis" -> 3;
			case "ucs2", "cp932", "big5", "euckr", "gb2312", "gbk", "sjis" -> 2;
			default -> 1;
		};
	}

	public static MySQLServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Integer bytesPerCharacter = null;
		Boolean noBackslashEscapes = null;
		Boolean foreignKeysInServer = null;
		final var databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try ( var statement = databaseMetaData.getConnection().createStatement() ) {
				try ( var resultSet = statement.executeQuery(
						"SELECT @@character_set_database, @@sql_mode, @@innodb_native_foreign_keys" ) ) {
					if ( resultSet.next() ) {
						bytesPerCharacter = getBytesPerCharacter( resultSet.getString( 1 ) );
						noBackslashEscapes = resultSet.getString( 2 ).toLowerCase().contains( "no_backslash_escapes" );
						// 0 = foreign keys handled by the server, 1 = handled by InnoDB natively
						foreignKeysInServer = resultSet.getInt( 3 ) == 0;
					}
				}
				catch (SQLException ignored) {
					// @@innodb_native_foreign_keys unavailable on older MySQL: retry without it
					try ( var resultSet = statement.executeQuery( "SELECT @@character_set_database, @@sql_mode" ) ) {
						if ( resultSet.next() ) {
							bytesPerCharacter = getBytesPerCharacter( resultSet.getString( 1 ) );
							noBackslashEscapes = resultSet.getString( 2 ).toLowerCase().contains( "no_backslash_escapes" );
						}
					}
				}
			}
			catch (SQLException ignored) {
			}
		}
		// default to the dialect-specific configuration settings
		if ( bytesPerCharacter == null ) {
			bytesPerCharacter = getInt( MYSQL_BYTES_PER_CHARACTER, info.getConfigurationValues(), 4 );
		}
		if ( noBackslashEscapes == null ) {
			noBackslashEscapes = getBoolean( MYSQL_NO_BACKSLASH_ESCAPES, info.getConfigurationValues() );
		}
		if ( foreignKeysInServer == null ) {
			foreignKeysInServer = getBoolean( MYSQL_FOREIGN_KEYS_IN_SERVER, info.getConfigurationValues() );
		}

		return new MySQLServerConfiguration(
				bytesPerCharacter,
				noBackslashEscapes,
				ConfigurationHelper.getString( AvailableSettings.STORAGE_ENGINE, info.getConfigurationValues() ),
				foreignKeysInServer );
	}

	/**
	 * @deprecated Use {@link #fromDialectResolutionInfo} instead.
	 */
	@Deprecated( since = "6.4", forRemoval = true )
	public static MySQLServerConfiguration fromDatabaseMetadata(DatabaseMetaData databaseMetaData) {
		int bytesPerCharacter = 4;
		boolean noBackslashEscapes = false;
		if ( databaseMetaData != null ) {
			try ( var statement = databaseMetaData.getConnection().createStatement();
					var resultSet = statement.executeQuery( "SELECT @@character_set_database, @@sql_mode" ) ) {
				if ( resultSet.next() ) {
					final String characterSet = resultSet.getString( 1 );
					bytesPerCharacter = getBytesPerCharacter( characterSet );
					// NO_BACKSLASH_ESCAPES
					final String sqlMode = resultSet.getString( 2 );
					if ( sqlMode.toLowerCase().contains( "no_backslash_escapes" ) ) {
						noBackslashEscapes = true;
					}
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return new MySQLServerConfiguration( bytesPerCharacter, noBackslashEscapes );
	}
}
