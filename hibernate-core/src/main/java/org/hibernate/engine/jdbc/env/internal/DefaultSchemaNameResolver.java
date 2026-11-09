package org.hibernate.engine.jdbc.env.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Default implementation of {@link SchemaNameResolver}
 * using {@link Connection#getSchema()}.
 *
 * @author Steve Ebersole
 */
public class DefaultSchemaNameResolver implements SchemaNameResolver {
	public static final @Nonnull DefaultSchemaNameResolver INSTANCE = new DefaultSchemaNameResolver();

	private DefaultSchemaNameResolver() {
	}

	@Override
	@Nullable
	public String resolveSchemaName(@Nonnull Connection connection, @Nonnull Dialect dialect) throws SQLException {
		try {
			return connection.getSchema();
		}
		catch (AbstractMethodError ignore) {
			// jConnect and jTDS report that they "support" schemas, but they don't really
			return null;
		}
	}

}
