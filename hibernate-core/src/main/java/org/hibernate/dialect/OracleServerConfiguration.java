/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_APPLICATION_CONTINUITY;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_AUTONOMOUS_DATABASE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_EXTENDED_STRING_SIZE;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Utility class that extract some initial configuration from the database for {@link OracleDialect}.
 *
 * @author Marco Belladelli
 * @author Loïc Lefèvre
 */
@Internal
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
		this( autonomous, extended, false, driverMajorVersion, driverMinorVersion );
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

		// default to the dialect-specific configuration settings
		final Map<String, Object> configuration = info.getConfigurationValues();
		final boolean defaultExtended = getBoolean( ORACLE_EXTENDED_STRING_SIZE, configuration, false );
		final boolean defaultAutonomous =  getBoolean( ORACLE_AUTONOMOUS_DATABASE, configuration, false );
		final boolean defaultContinuity = getBoolean( ORACLE_APPLICATION_CONTINUITY, configuration, false );

		boolean extended;
		boolean autonomous;
		boolean applicationContinuity;

		int majorVersion;
		int minorVersion;
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData == null ) {
			extended = defaultExtended;
			autonomous = defaultAutonomous;
			applicationContinuity = defaultContinuity;
			try {
				final Driver driver = DriverManager.getDriver( "jdbc:oracle:thin:" );
				majorVersion = driver.getMajorVersion();
				minorVersion = driver.getMinorVersion();
			}
			catch (SQLException ex) {
				majorVersion = 19;
				minorVersion = 0;
			}
		}
		else {
			majorVersion = databaseMetaData.getDriverMajorVersion();
			minorVersion = databaseMetaData.getDriverMinorVersion();
			try {
				final Connection connection = databaseMetaData.getConnection(); // we should not close this
				try ( final Statement statement = connection.createStatement() ) {
					applicationContinuity = determineApplicationContinuity( connection, statement );
					autonomous = isAutonomous( statement );
					extended = isExtended( statement );
				}
			}
			catch (SQLException sqle) {
				extended = defaultExtended;
				autonomous = defaultAutonomous;
				applicationContinuity = defaultContinuity;
			}
		}

		return new OracleServerConfiguration( autonomous, extended, applicationContinuity, majorVersion, minorVersion );
	}

	private static boolean isExtended(Statement statement) {
		try ( final ResultSet resultSet =
					statement.executeQuery( "select cast('string' as varchar2(32000)) from dual" ) ) {
			resultSet.next();
			// succeeded, so MAX_STRING_SIZE == EXTENDED
			return true;
		}
		catch (SQLException ex) {
			// failed, so MAX_STRING_SIZE == STANDARD, still need to check autonomous
			return false;
		}
	}

	private static Boolean determineApplicationContinuity(Connection connection, Statement statement) {
		// Use Oracle JDBC replay statistics information to determine if this
		// connection is protected by Application Continuity
		try {
			final Class<?> statisticReportTypeEnum =
					Class.forName( "oracle.jdbc.replay.ReplayableConnection$StatisticsReportType",
							false, Thread.currentThread().getContextClassLoader() );
			final Object forCurrentConnection =
					statisticReportTypeEnum.getField( "FOR_CURRENT_CONNECTION" ).get( null );
			final Method getReplayStatistics =
					connection.getClass().getMethod( "getReplayStatistics", statisticReportTypeEnum );
			final Class<?> replayStatistics = getReplayStatistics.getReturnType();
			final Method getTotalRequests = replayStatistics.getMethod("getTotalRequests");
			final Method getTotalProtectedCalls = replayStatistics.getMethod("getTotalProtectedCalls");

			final Object before = getReplayStatistics.invoke( connection, forCurrentConnection );
			final Long totalRequestsBefore = (Long) getTotalRequests.invoke( before );
			final Long protectedCallsBefore = (Long) getTotalProtectedCalls.invoke( before );

			try ( final ResultSet resultSet = statement.executeQuery("select 1") ) {
				resultSet.next();
			}

			final Object after = getReplayStatistics.invoke( connection, forCurrentConnection );
			final Long totalRequestsAfter = (Long) getTotalRequests.invoke( after );
			final Long protectedCallsAfter = (Long) getTotalProtectedCalls.invoke( after );

			// Application continuity is enabled on this database service if the number of
			// total requests and the number of protected calls for this connection have
			// both increased.
			return totalRequestsAfter > totalRequestsBefore
				&& protectedCallsAfter > protectedCallsBefore;
		}
		catch (Exception e) {
			// A ClassCastException or a NullPointerException are expected here in the case
			// the Connection Factory is not the right one (not Replayable: ClassCastException)
			// or if the database service has not been configured (server side) to enable
			// application continuity (NullPointerException).
			return false;
		}
	}

	private static boolean isAutonomous(Statement statement) {
		try ( final ResultSet resultSet =
					statement.executeQuery( "select sys_context('USERENV','CLOUD_SERVICE') from dual" ) ) {
			return resultSet.next()
				&& isAutonomous( resultSet.getString(1) );
		}
		catch (SQLException ex) {
			return false;
		}
	}

	private static boolean isAutonomous(String type) {
		return type != null && switch ( type ) {
			case "OLTP", "DWCS", "JDCS" -> true;
			default -> false;
		};
	}
}
