/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Convenience base implementation of {@link ValueExtractor}
 *
 * @author Steve Ebersole
 */
public abstract class BasicExtractor<J> implements ValueExtractor<J> {
	private static final Logger log = CoreLogging.logger( BasicExtractor.class );

	private final JavaTypeDescriptor<J> javaDescriptor;
	private final SqlTypeDescriptor sqlDescriptor;

	public BasicExtractor(JavaTypeDescriptor<J> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
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
	public J extract(ResultSet rs, int position, WrapperOptions options) throws SQLException {
		final J value = doExtract( rs, position, options );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || rs.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [null]",
						position,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [%s]",
						position,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 * <p/>
	 * Called from {@link #extract}.  Null checking of the value (as well as consulting {@link ResultSet#wasNull}) is
	 * done there.
	 *
	 * @param rs The result set
	 * @param position The position of the value to extract.
	 * @param options The binding options
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates a problem access the result set
	 */
	protected abstract J doExtract(ResultSet rs, int position, WrapperOptions options) throws SQLException;

	@Override
	public J extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		final J value = doExtract( statement, index, options );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || statement.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted procedure output  parameter ([%s] : [%s]) - [null]",
						index,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted procedure output  parameter ([%s] : [%s]) - [%s]",
						index,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 * <p/>
	 * Called from {@link #extract}.  Null checking of the value (as well as consulting {@link ResultSet#wasNull}) is
	 * done there.
	 *
	 * @param statement The callable statement containing the output parameter
	 * @param index The index (position) of the output parameter
	 * @param options The binding options
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates a problem accessing the parameter value
	 */
	protected abstract J doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException;

	@Override
	public J extract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		final J value = doExtract( statement, name, options );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || statement.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [null]",
						name,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [%s]",
						name,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() ),
						getJavaDescriptor().extractLoggableRepresentation( value )
				);
			}
			return value;
		}
	}

	/**
	 * Perform the extraction.
	 * <p/>
	 * Called from {@link #extract}.  Null checking of the value (as well as consulting {@link ResultSet#wasNull}) is
	 * done there.
	 *
	 * @param statement The callable statement containing the output parameter
	 * @param name The output parameter name
	 * @param options The binding options
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates a problem accessing the parameter value
	 */
	protected abstract J doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException;
}
