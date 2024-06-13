/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_APPLICATION_CONTINUITY;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_AUTONOMOUS_DATABASE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_EXTENDED_STRING_SIZE;

/**
 * Utility class that extract some initial configuration from the database for {@link OracleDialect}.
 *
 * @author Marco Belladelli
 * @author Loïc Lefèvre
 */
public class OracleServerConfiguration {
	private final boolean autonomous;
	private final boolean extended;
	private final boolean applicationContinuity;
	private final int driverMajorVersion;
	private final int driverMinorVersion;

	public boolean isAutonomous() {
		return autonomous;
	}

	public boolean isExtended() {
		return extended;
	}

	public boolean isApplicationContinuity() {
		return applicationContinuity;
	}

	public int getDriverMajorVersion() {
		return driverMajorVersion;
	}

	public int getDriverMinorVersion() {
		return driverMinorVersion;
	}

	public OracleServerConfiguration(boolean autonomous, boolean extended) {
		this( autonomous, extended, false, 19, 0 );
	}

	public OracleServerConfiguration(
			boolean autonomous,
			boolean extended,
			int driverMajorVersion,
			int driverMinorVersion) {
		this(autonomous, extended, false, driverMajorVersion, driverMinorVersion);
	}
		
	public OracleServerConfiguration(
			boolean autonomous,
			boolean extended,
			boolean applicationContinuity,
			int driverMajorVersion,
			int driverMinorVersion) {
		this.autonomous = autonomous;
		this.extended = extended;
		this.applicationContinuity = applicationContinuity;
		this.driverMajorVersion = driverMajorVersion;
		this.driverMinorVersion = driverMinorVersion;
	}

	public static OracleServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		Boolean extended = null;
		Boolean autonomous = null;
		Boolean applicationContinuity = null;
		Integer majorVersion = null;
		Integer minorVersion = null;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			majorVersion = databaseMetaData.getDriverMajorVersion();
			minorVersion = databaseMetaData.getDriverMinorVersion();


			try {
				final Connection c = databaseMetaData.getConnection();

				try (final Statement statement = c.createStatement()) {

					// Use Oracle JDBC replay statistics information to determine if this
					// connection is protected by Application Continuity
					try {
						final Class statisticReportTypeEnum = Class.forName("oracle.jdbc.replay.ReplayableConnection$StatisticsReportType",false, Thread.currentThread().getContextClassLoader());
						final Field forCurrentConnection = statisticReportTypeEnum.getField("FOR_CURRENT_CONNECTION");

						final Method getReplayStatistics = c.getClass().getMethod("getReplayStatistics", statisticReportTypeEnum);

						Object stats = getReplayStatistics.invoke(c,forCurrentConnection.get(null));

						final Method getTotalRequests = stats.getClass().getMethod("getTotalRequests");
						final Method getTotalProtectedCalls = stats.getClass().getMethod("getTotalProtectedCalls");

						final Long totalRequestsBefore = (Long)getTotalRequests.invoke(stats);
						final Long protectedCallsBefore = (Long)getTotalProtectedCalls.invoke(stats);

						try (ResultSet r = statement.executeQuery("select 1")) {
							r.next();
						}

						stats = getReplayStatistics.invoke(c,forCurrentConnection.get(null));

						final Long totalRequestsAfter = (Long)getTotalRequests.invoke(stats);
						final Long protectedCallsAfter = (Long)getTotalProtectedCalls.invoke(stats);

						// Application continuity is enabled on this database service if the number of
						// total requests and the number of protected calls for this connection have
						// both increased.
						applicationContinuity = totalRequestsAfter > totalRequestsBefore && protectedCallsAfter > protectedCallsBefore;
					}
					catch(Exception e) {
						// A ClassCastException or a NullPointerException are expected here in the case
						// the Connection Factory is not the right one (not Replayable: ClassCastException)
						// or if the database service has not been configured (server side) to enable
						// application continuity (NullPointerException).
						applicationContinuity = false;
					}

					// continue the checks...
					final ResultSet rs = statement.executeQuery(
							"select cast('string' as varchar2(32000)), " +
									"sys_context('USERENV','CLOUD_SERVICE') from dual"
					);
					if (rs.next()) {
						// succeeded, so MAX_STRING_SIZE == EXTENDED
						extended = true;
						autonomous = isAutonomous(rs.getString(2));
					}
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
		if ( applicationContinuity == null ) {
			applicationContinuity = ConfigurationHelper.getBoolean(
					ORACLE_APPLICATION_CONTINUITY,
					info.getConfigurationValues(),
					false
			);
		}
		if ( majorVersion == null ) {
			try {
				java.sql.Driver driver = java.sql.DriverManager.getDriver( "jdbc:oracle:thin:" );
				majorVersion = driver.getMajorVersion();
				minorVersion = driver.getMinorVersion();
			}
			catch (SQLException ex) {
				majorVersion = 19;
				minorVersion = 0;
			}

		}
		return new OracleServerConfiguration( autonomous, extended, applicationContinuity, majorVersion, minorVersion );
	}

	private static boolean isAutonomous(String cloudServiceParam) {
		return cloudServiceParam != null && List.of( "OLTP", "DWCS", "JDCS" ).contains( cloudServiceParam );
	}

	private static boolean isAutonomous(DatabaseMetaData databaseMetaData) {
		try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
			return statement.executeQuery(
							"select 1 from dual where sys_context('USERENV','CLOUD_SERVICE') in ('OLTP','DWCS','JDCS')" )
					.next();
		}
		catch (SQLException ex) {
			// Ignore
		}
		return false;
	}

}
