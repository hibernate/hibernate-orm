/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Convenience base implementation of {@link JdbcValueExtractor}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcValueExtractor<J> implements JdbcValueExtractor<J> {
	private static final Logger log = CoreLogging.logger( AbstractJdbcValueExtractor.class );

	private final JavaTypeDescriptor<J> javaDescriptor;
	private final SqlTypeDescriptor sqlDescriptor;

	public AbstractJdbcValueExtractor(JavaTypeDescriptor<J> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.sqlDescriptor = sqlDescriptor;
	}

	public JavaTypeDescriptor<J> getJavaDescriptor() {
		return javaDescriptor;
	}

	public SqlTypeDescriptor getSqlDescriptor() {
		return sqlDescriptor;
	}

	@Override
	public J extract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException {
		final J value = doExtract( rs, position, executionContext );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || rs.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [null]",
						position,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [%s]",
						position,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 *
	 * @implSpec Null checking of the value (including {@link ResultSet#wasNull} checking) is done in caller.
	 */
	protected abstract J doExtract(ResultSet rs, int position, ExecutionContext executionContext) throws SQLException;

	@Override
	public J extract(CallableStatement statement, int index, ExecutionContext executionContext) throws SQLException {
		final J value = doExtract( statement, index, executionContext );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || statement.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted procedure output  parameter ([%s] : [%s]) - [null]",
						index,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted procedure output  parameter ([%s] : [%s]) - [%s]",
						index,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 *
	 * @implSpec Null checking of the value (including {@link ResultSet#wasNull} checking) is done in caller.
	 */
	protected abstract J doExtract(CallableStatement statement, int position, ExecutionContext executionContext) throws SQLException;

	@Override
	public J extract(CallableStatement statement, String jdbcParameterName, ExecutionContext executionContext) throws SQLException {
		final J value = doExtract( statement, jdbcParameterName, executionContext );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || statement.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [null]",
						jdbcParameterName,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [%s]",
						jdbcParameterName,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 *
	 * @implSpec Null checking of the value (including {@link ResultSet#wasNull} checking) is done in caller.
	 */
	protected abstract J doExtract(CallableStatement statement, String name, ExecutionContext executionContext) throws SQLException;
}
