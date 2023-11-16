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
import java.sql.Statement;
import java.util.List;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_AUTONOMOUS_DATABASE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_EXTENDED_STRING_SIZE;

/**
 * Utility class that extract some initial configuration from the database for {@link OracleDialect}.
 *
 * @author Marco Belladelli
 */
public class OracleServerConfiguration {
	private final boolean autonomous;
	private final boolean extended;

	public boolean isAutonomous() {
		return autonomous;
	}

	public boolean isExtended() {
		return extended;
	}

	public OracleServerConfiguration(boolean autonomous, boolean extended) {
		this.autonomous = autonomous;
		this.extended = extended;
	}

	public static OracleServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Boolean extended = null;
		Boolean autonomous = null;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
				final ResultSet rs = statement.executeQuery(
						"select cast('string' as varchar2(32000)), " +
								"sys_context('USERENV','CLOUD_SERVICE') from dual"
				);
				if ( rs.next() ) {
					// succeeded, so MAX_STRING_SIZE == EXTENDED
					extended = true;
					autonomous = isAutonomous( rs.getString( 2 ) );
				}
			}
			catch (SQLException ex) {
				// failed, so MAX_STRING_SIZE == STANDARD, still need to check autonomous
				extended = false;
				autonomous = isAutonomous( databaseMetaData );
			}
		}
		// default to the dialect-specific configuration settings
		if ( extended == null ) {
			extended = ConfigurationHelper.getBoolean(
					ORACLE_EXTENDED_STRING_SIZE,
					info.getConfigurationValues(),
					false
			);
		}
		if ( autonomous == null ) {
			autonomous = ConfigurationHelper.getBoolean(
					ORACLE_AUTONOMOUS_DATABASE,
					info.getConfigurationValues(),
					false
			);
		}
		return new OracleServerConfiguration( autonomous, extended );
	}

	private static boolean isAutonomous(String cloudServiceParam) {
		return List.of( "OLTP", "DWCS", "JSON" ).contains( cloudServiceParam );
	}

	private static boolean isAutonomous(DatabaseMetaData databaseMetaData) {
		try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
			return statement.executeQuery( "select 1 from dual where sys_context('USERENV','CLOUD_SERVICE') in ('OLTP','DWCS','JSON')" ).next();
		}
		catch (SQLException ex) {
			// Ignore
		}
		return false;
	}
}
