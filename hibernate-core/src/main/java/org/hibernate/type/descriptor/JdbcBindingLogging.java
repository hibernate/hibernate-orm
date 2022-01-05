/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor;

import org.jboss.logging.Logger;

public interface JdbcBindingLogging {
	String NAME = "org.hibernate.orm.jdbc.bind";

	Logger LOGGER = Logger.getLogger( NAME );

	boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();

	static void logBinding(int jdbcPosition, int typeCode, Object value) {
		assert TRACE_ENABLED;

		LOGGER.tracef(
				"binding parameter [%s] as [%s] - [%s]",
				jdbcPosition,
				JdbcTypeNameMapper.getTypeName( typeCode ),
				value
		);
	}

	static void logNullBinding(int jdbcPosition, int typeCode) {
		assert TRACE_ENABLED;

		LOGGER.tracef(
				"binding parameter [%s] as [%s] - [null]",
				jdbcPosition,
				JdbcTypeNameMapper.getTypeName( typeCode )
		);
	}

	static void logBinding(String callableParameterName, int typeCode, Object value) {
		assert TRACE_ENABLED;

		LOGGER.tracef(
				"binding parameter [%s] as [%s] - [%s]",
				callableParameterName,
				JdbcTypeNameMapper.getTypeName( typeCode ),
				value
		);
	}

	static void logNullBinding(String callableParameterName, int typeCode) {
		assert TRACE_ENABLED;

		LOGGER.tracef(
				"binding parameter [%s] as [%s] - [null]",
				callableParameterName,
				JdbcTypeNameMapper.getTypeName( typeCode )
		);
	}

}
