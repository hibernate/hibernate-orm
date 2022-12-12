/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class MySQLServerConfiguration {

	private final int bytesPerCharacter;
	private final boolean noBackslashEscapesEnabled;

	public MySQLServerConfiguration(int bytesPerCharacter, boolean noBackslashEscapesEnabled) {
		this.bytesPerCharacter = bytesPerCharacter;
		this.noBackslashEscapesEnabled = noBackslashEscapesEnabled;
	}

	public int getBytesPerCharacter() {
		return bytesPerCharacter;
	}

	public boolean isNoBackslashEscapesEnabled() {
		return noBackslashEscapesEnabled;
	}

	public static MySQLServerConfiguration fromDatabaseMetadata(DatabaseMetaData databaseMetaData) {
		int bytesPerCharacter = 4;
		boolean noBackslashEscapes = false;
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement()) {
				final ResultSet rs = s.executeQuery( "SELECT @@character_set_database, @@sql_mode" );
				if ( rs.next() ) {
					final String characterSet = rs.getString( 1 );
					final int collationIndex = characterSet.indexOf( '_' );
					// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
					switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
						case "utf16":
						case "utf16le":
						case "utf32":
						case "utf8mb4":
						case "gb18030":
							break;
						case "utf8":
						case "utf8mb3":
						case "eucjpms":
						case "ujis":
							bytesPerCharacter = 3;
							break;
						case "ucs2":
						case "cp932":
						case "big5":
						case "euckr":
						case "gb2312":
						case "gbk":
						case "sjis":
							bytesPerCharacter = 2;
							break;
						default:
							bytesPerCharacter = 1;
					}
					// NO_BACKSLASH_ESCAPES
					final String sqlMode = rs.getString( 2 );
					if ( sqlMode.toLowerCase().contains( "no_backslash_escapes" ) ) {
						noBackslashEscapes = true;
					}
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return new MySQLServerConfiguration(bytesPerCharacter, noBackslashEscapes);
	}
}
