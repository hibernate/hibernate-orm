package org.hibernate.tool.schema.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionAccessProvidedConnectionImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * Specialized DdlTransactionIsolator for cases where we have a user provided Connection
 *
 * @author Steve Ebersole
 */
class DdlTransactionIsolatorProvidedConnectionImpl implements DdlTransactionIsolator {
	private final JdbcContext jdbcContext;

	public DdlTransactionIsolatorProvidedConnectionImpl(JdbcContext jdbcContext) {
		assert jdbcContext.getJdbcConnectionAccess() instanceof JdbcConnectionAccessProvidedConnectionImpl;
		this.jdbcContext = jdbcContext;
	}

	@Override
	public JdbcContext getJdbcContext() {
		return jdbcContext;
	}

	@Override
	public Connection getIsolatedConnection() {
		try {
			return jdbcContext.getJdbcConnectionAccess().obtainConnection();
		}
		catch (SQLException e) {
			// should never happen
			throw new SchemaManagementException( "Error accessing user-provided Connection via JdbcConnectionAccessProvidedConnectionImpl", e );
		}
	}

	@Override
	public void prepare() {
	}

	@Override
	public void release() {
	}
}
