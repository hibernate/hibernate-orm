/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.sql;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Convenience base implementation of {@link org.hibernate.type.descriptor.ValueExtractor}
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
	public J extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		final J value = doExtract( rs, name, options );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || rs.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [null]",
						name,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted value ([%s] : [%s]) - [%s]",
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
	 * @param rs The result set
	 * @param name The value name in the result set
	 * @param options The binding options
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Indicates a problem access the result set
	 */
	protected abstract J doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException;

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
	public J extract(CallableStatement statement, String[] paramNames, WrapperOptions options) throws SQLException {
		if ( paramNames.length > 1 ) {
			throw new IllegalArgumentException( "Basic value extraction cannot handle multiple output parameters" );
		}
		final String paramName = paramNames[0];
		final J value = doExtract( statement, paramName, options );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null || statement.wasNull() ) {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [null]",
						paramName,
						JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
				);
			}
			return null;
		}
		else {
			if ( traceEnabled ) {
				log.tracef(
						"extracted named procedure output  parameter ([%s] : [%s]) - [%s]",
						paramName,
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
