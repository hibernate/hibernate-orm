package org.hibernate.orm.test.type.temporal;

import org.hibernate.cfg.AvailableSettings;

import java.time.ZoneId;

/**
 * @param defaultJvmTimeZone The default timezone affects conversions done using java.util,
 * which is why we take it into account even with timezone-independent types such as Instant.
 * @param hibernateJdbcTimeZone The Hibernate setting, {@value AvailableSettings#JDBC_TIME_ZONE},
 * may affect a lot of time-related types,
 * which is why we take it into account even with timezone-independent types such as Instant.
 * @param legacyJdbcSupported Whether this input can be exercised through the legacy JDBC strategy.
 */
public record Environment(
		ZoneId defaultJvmTimeZone,
		ZoneId hibernateJdbcTimeZone,
		Class<? extends AbstractRemappingH2Dialect> remappingDialectClass,
		boolean directJdbc,
		boolean legacyJdbcSupported) {

	public Environment(
			ZoneId defaultJvmTimeZone,
			ZoneId hibernateJdbcTimeZone,
			Class<? extends AbstractRemappingH2Dialect> remappingDialectClass,
			boolean directJdbc) {
		this( defaultJvmTimeZone, hibernateJdbcTimeZone, remappingDialectClass, directJdbc, true );
	}

	@Override
	public String toString() {
		return String.format(
				"[JVM TZ: %s, JDBC TZ: %s, remapping dialect: %s, direct JDBC: %s, legacy JDBC supported: %s]",
				defaultJvmTimeZone,
				hibernateJdbcTimeZone,
				remappingDialectClass == null ? null : remappingDialectClass.getSimpleName(),
				directJdbc,
				legacyJdbcSupported
		);
	}
}
