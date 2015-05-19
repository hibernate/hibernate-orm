/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.internal.CoreLogging;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Convenience base implementation of {@link ValueBinder}
 *
 * @author Steve Ebersole
 */
public abstract class BasicBinder<J> implements ValueBinder<J> {
	private static final Logger log = CoreLogging.logger( BasicBinder.class );

	private static final String BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [%s]";
	private static final String NULL_BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [null]";

	private final JavaTypeDescriptor<J> javaDescriptor;
	private final SqlTypeDescriptor sqlDescriptor;

	public JavaTypeDescriptor<J> getJavaDescriptor() {
		return javaDescriptor;
	}

	public SqlTypeDescriptor getSqlDescriptor() {
		return sqlDescriptor;
	}

	public BasicBinder(JavaTypeDescriptor<J> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.sqlDescriptor = sqlDescriptor;
	}

	@Override
	public final void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		final boolean traceEnabled = log.isTraceEnabled();
		if ( value == null ) {
			if ( traceEnabled ) {
				log.trace(
						String.format(
								NULL_BIND_MSG_TEMPLATE,
								index,
								JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getSqlType() )
						)
				);
			}
			st.setNull( index, sqlDescriptor.getSqlType() );
		}
		else {
			if ( traceEnabled ) {
				log.trace(
						String.format(
								BIND_MSG_TEMPLATE,
								index,
								JdbcTypeNameMapper.getTypeName( sqlDescriptor.getSqlType() ),
								getJavaDescriptor().extractLoggableRepresentation( value )
						)
				);
			}
			doBind( st, value, index, options );
		}
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The prepared statement
	 * @param value The value to bind (not null).
	 * @param index The index at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(PreparedStatement st, J value, int index, WrapperOptions options)
			throws SQLException;
}
