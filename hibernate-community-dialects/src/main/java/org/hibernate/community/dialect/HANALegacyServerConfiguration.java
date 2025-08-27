/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.HANA_MAX_LOB_PREFETCH_SIZE;

/**
 * Utility class that extracts some initial configuration from the database for {@link HANALegacyDialect}.
 */
public class HANALegacyServerConfiguration {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( HANALegacyServerConfiguration.class );
	public static final int MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE = 1024;

	private final DatabaseVersion fullVersion;
	private final int maxLobPrefetchSize;

	public HANALegacyServerConfiguration(DatabaseVersion fullVersion) {
		this( fullVersion, MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );
	}

	public HANALegacyServerConfiguration(DatabaseVersion fullVersion, int maxLobPrefetchSize) {
		this.fullVersion = fullVersion;
		this.maxLobPrefetchSize = maxLobPrefetchSize;
	}

	public DatabaseVersion getFullVersion() {
		return fullVersion;
	}

	public int getMaxLobPrefetchSize() {
		return maxLobPrefetchSize;
	}

	public static HANALegacyServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Integer maxLobPrefetchSize = null;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
				try ( ResultSet rs = statement.executeQuery(
						"SELECT TOP 1 VALUE,MAP(LAYER_NAME,'DEFAULT',1,'SYSTEM',2,'DATABASE',3,4) AS LAYER FROM SYS.M_INIFILE_CONTENTS WHERE FILE_NAME='indexserver.ini' AND SECTION='session' AND KEY='max_lob_prefetch_size' ORDER BY LAYER DESC" ) ) {
					// This only works if the current user has the privilege INIFILE ADMIN
					if ( rs.next() ) {
						maxLobPrefetchSize = rs.getInt( 1 );
					}
				}
			}
			catch (SQLException e) {
				// Ignore
				LOG.debug(
						"An error occurred while trying to determine the value of the HANA parameter indexserver.ini / session / max_lob_prefetch_size.",
						e );
			}
		}
		// default to the dialect-specific configuration settings
		if ( maxLobPrefetchSize == null ) {
			maxLobPrefetchSize = ConfigurationHelper.getInt(
					HANA_MAX_LOB_PREFETCH_SIZE,
					info.getConfigurationValues(),
					MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE
			);
		}
		return new HANALegacyServerConfiguration( staticDetermineDatabaseVersion( info ), maxLobPrefetchSize );
	}

	static DatabaseVersion staticDetermineDatabaseVersion(DialectResolutionInfo info) {
		// Parse the version according to https://answers.sap.com/questions/9760991/hana-sps-version-check.html
		final String versionString = info.getDatabaseVersion();
		int majorVersion = 1;
		int minorVersion = 0;
		int patchLevel = 0;
		if ( versionString == null ) {
			return HANALegacyDialect.DEFAULT_VERSION;
		}
		final String[] components = StringHelper.split( ".", versionString );
		if ( components.length >= 3 ) {
			try {
				majorVersion = Integer.parseInt( components[0] );
				minorVersion = Integer.parseInt( components[1] );
				patchLevel = Integer.parseInt( components[2] );
			}
			catch (NumberFormatException ex) {
				// Ignore
			}
		}
		return DatabaseVersion.make( majorVersion, minorVersion, patchLevel );
	}
}
