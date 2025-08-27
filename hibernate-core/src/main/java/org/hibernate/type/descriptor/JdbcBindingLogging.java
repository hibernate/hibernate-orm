/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

@SubSystemLogging(
		name = JdbcBindingLogging.NAME,
		description = "Logging of JDBC parameter value binding"
)
@Internal
public interface JdbcBindingLogging {
	String NAME = SubSystemLogging.BASE + ".jdbc.bind";

	Logger LOGGER = Logger.getLogger( NAME );

	static void logBinding(int jdbcPosition, int typeCode, Object value) {
		if ( LOGGER.isTraceEnabled() ) {
			LOGGER.tracef(
					"binding parameter (%s:%s) <- [%s]",
					jdbcPosition,
					JdbcTypeNameMapper.getTypeName( typeCode ),
					value
			);
		}
	}

	static void logNullBinding(int jdbcPosition, int typeCode) {
		if ( LOGGER.isTraceEnabled() ) {
			LOGGER.tracef(
					"binding parameter (%s:%s) <- [null]",
					jdbcPosition,
					JdbcTypeNameMapper.getTypeName( typeCode )
			);
		}
	}

	static void logBinding(String callableParameterName, int typeCode, Object value) {
		if ( LOGGER.isTraceEnabled() ) {
			LOGGER.tracef(
					"binding parameter (%s:%s) <- [%s]",
					callableParameterName,
					JdbcTypeNameMapper.getTypeName( typeCode ),
					value
			);
		}
	}

	static void logNullBinding(String callableParameterName, int typeCode) {
		if ( LOGGER.isTraceEnabled() ) {
			LOGGER.tracef(
					"binding parameter (%s:%s) <- [null]",
					callableParameterName,
					JdbcTypeNameMapper.getTypeName( typeCode )
			);
		}
	}

}
