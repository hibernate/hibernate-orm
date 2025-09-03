/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.HANA_MAX_LOB_PREFETCH_SIZE;

/**
 * Utility class that extracts some initial configuration from the database for {@link HANADialect}.
 */
public class HANAServerConfiguration {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( HANAServerConfiguration.class );

	private static final Pattern CLOUD_VERSION_PATTERN = Pattern.compile( "\\(fa/CE(\\d+)\\.(\\d+)\\)" );
	public static final int MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE = 1024;

	private final DatabaseVersion fullVersion;
	private final int maxLobPrefetchSize;

	public HANAServerConfiguration(DatabaseVersion fullVersion) {
		this( fullVersion, MAX_LOB_PREFETCH_SIZE_DEFAULT_VALUE );
	}

	public HANAServerConfiguration(DatabaseVersion fullVersion, int maxLobPrefetchSize) {
		this.fullVersion = fullVersion;
		this.maxLobPrefetchSize = maxLobPrefetchSize;
	}

	public DatabaseVersion getFullVersion() {
		return fullVersion;
	}

	public int getMaxLobPrefetchSize() {
		return maxLobPrefetchSize;
	}

	public static HANAServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Integer maxLobPrefetchSize = null;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		DatabaseVersion databaseVersion = null;
		if ( databaseMetaData != null ) {
			int databaseMajorVersion = -1;
			try {
				databaseMajorVersion = databaseMetaData.getDatabaseMajorVersion();
			}
			catch (SQLException e) {
				// Ignore
				log.debug(
						"An error occurred while trying to determine the database version.",
						e );
			}

			if (databaseMajorVersion > 0 && databaseMajorVersion < 4) {
				try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
					try ( ResultSet rs = statement.executeQuery(
							"SELECT TOP 1 VALUE,MAP(LAYER_NAME,'DEFAULT',1,'SYSTEM',2,'DATABASE',3,4) AS LAYER FROM SYS.M_CONFIGURATION_PARAMETER_VALUES WHERE FILE_NAME='indexserver.ini' AND SECTION='session' AND KEY='max_lob_prefetch_size' ORDER BY LAYER DESC" ) ) {
						// This only works if the current user has the privilege INIFILE ADMIN
						if ( rs.next() ) {
							maxLobPrefetchSize = rs.getInt( 1 );
						}
					}
				}
				catch (SQLException e) {
					// Ignore
					log.debug(
							"An error occurred while trying to determine the value of the HANA parameter indexserver.ini / session / max_lob_prefetch_size.",
							e );
				}
			}
			else {
				databaseVersion = determineDatabaseVersion( info );
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
		if ( databaseVersion == null ) {
			databaseVersion = staticDetermineDatabaseVersion( info );
		}
		return new HANAServerConfiguration( databaseVersion, maxLobPrefetchSize );
	}

	public static DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		String databaseVersion = null;
		if ( databaseMetaData != null ) {
			try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
				try (ResultSet rs = statement.executeQuery(
						"SELECT VALUE FROM M_SYSTEM_OVERVIEW WHERE NAME='Version'" )) {
					// This only works if the current user has the privilege INIFILE ADMIN
					if ( rs.next() ) {
						databaseVersion = rs.getString( 1 );
					}
				}
			}
			catch (SQLException e) {
				// Ignore
				log.debug( "An error occurred while trying to determine the HANA Cloud version.", e );
			}
		}
		return databaseVersion == null
				? staticDetermineDatabaseVersion( info )
				: determineDatabaseVersion( databaseVersion );
	}

	public static DatabaseVersion determineDatabaseVersion(String versionString) {
		if ( versionString == null ) {
			return HANADialect.MINIMUM_VERSION;
		}
		final String[] components = StringHelper.split( " ", versionString );
		final DatabaseVersion databaseVersion = staticDetermineDatabaseVersion( components[0] );
		if ( components.length == 1 || databaseVersion.isBefore( 4 ) ) {
			return databaseVersion;
		}
		else {
			// Parse the HANA Cloud version
			final Matcher matcher = CLOUD_VERSION_PATTERN.matcher( components[1] );
			if ( matcher.matches() ) {
				final int year = Integer.parseInt( matcher.group( 1 ) );
				final int week = Integer.parseInt( matcher.group( 2 ) );
				return new SimpleDatabaseVersion(
						databaseVersion.getDatabaseMajorVersion(),
						getHanaCloudVersion( LocalDate.of( year, 1, 1 ).plusWeeks( week ) ),
						databaseVersion.getDatabaseMicroVersion()
				);
			}
			else {
				return databaseVersion;
			}
		}
	}

	private static int getHanaCloudVersion(LocalDate date) {
		final int quarter = switch (date.getMonth()) {
			case JANUARY, FEBRUARY, MARCH -> 1;
			case APRIL, MAY, JUNE -> 2;
			case JULY, AUGUST, SEPTEMBER -> 3;
			case OCTOBER, NOVEMBER, DECEMBER -> 4;
		};
		return date.getYear() * 10 + quarter;
	}

	public static DatabaseVersion staticDetermineDatabaseVersion(DialectResolutionInfo info) {
		return staticDetermineDatabaseVersion( info.getDatabaseVersion() );
	}

	public static DatabaseVersion staticDetermineDatabaseVersion(String versionString) {
		// Parse the version according to https://answers.sap.com/questions/9760991/hana-sps-version-check.html
		int majorVersion = 1;
		int minorVersion = 0;
		int patchLevel = 0;
		if ( versionString == null ) {
			return HANADialect.MINIMUM_VERSION;
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
