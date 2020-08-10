/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Properties;
import java.util.logging.Logger;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;

/**
 * Dialect for <a href="https://prestosql.io/">Presto</a>
 *
 * @author findepi
 */
public class PrestoDialect extends Dialect {

	public PrestoDialect() {
		registerHibernateType( Types.BIGINT, StandardBasicTypes.LONG.getName() );

		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "", "||", "" ) );
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}


	@Override
	public LimitHandler getLimitHandler() {
		return new AbstractLimitHandler() {
			@Override
			public String processSql(String sql, RowSelection selection) {
				final boolean hasOffset = LimitHelper.hasFirstRow( selection );
				return sql + ( hasOffset
						? " OFFSET ? LIMIT ?"
						: " LIMIT ?" );
			}

			@Override
			public boolean supportsLimit() {
				return true;
			}

			@Override
			public boolean supportsLimitOffset() {
				return true;
			}

			@Override
			public boolean supportsVariableLimit() {
				return true;
			}
		};
	}

	@Override
	public boolean supportsExistsInSelect() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInSet() {
		return false;
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		// Presto does not support declaring primary keys. It is recommended that table schema is created externally.
		return "";
	}


	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return new Exporter<ForeignKey>() {
			@Override
			public String[] getSqlCreateStrings(ForeignKey exportable, Metadata metadata) {
				return new String[0];
			}

			@Override
			public String[] getSqlDropStrings(ForeignKey exportable, Metadata metadata) {
				return new String[0];
			}
		};
	}

	public static class PrestoDriverAdapter implements Driver {

		private final Driver implementation;

		public PrestoDriverAdapter() {
			try {
				implementation = (Driver) Class.forName( "io.prestosql.jdbc.PrestoDriver" ) // avoid compile-time dependency
						.getConstructor()
						.newInstance();
			}
			catch (ReflectiveOperationException e) {
				throw new RuntimeException( e );
			}
		}

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			if ( info != null ) {
				info = copy( info );
				// DdlTransactionIsolatorNonJtaImpl works with Presto driver only in autocommit mode (hibernate.connection.autocommit=true)
				// When this is set, DriverConnectionCreator will pass autocommit connection property which Presto
				// driver does not accept. Here we remove the property as a workaround, it's being handled on the
				// Hibernate's side.
				info.remove( "autocommit" );
			}
			return implementation.connect( url, info );
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return implementation.acceptsURL( url );
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return implementation.getPropertyInfo( url, info );
		}

		@Override
		public int getMajorVersion() {
			return implementation.getMajorVersion();
		}

		@Override
		public int getMinorVersion() {
			return implementation.getMinorVersion();
		}

		@Override
		public boolean jdbcCompliant() {
			return implementation.jdbcCompliant();
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return implementation.getParentLogger();
		}

		private static Properties copy(Properties properties) {
			Properties copy = new Properties();
			copy.putAll( properties );
			return copy;
		}
	}
}
