/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.util.List;

import org.hibernate.community.dialect.CacheDialect;
import org.hibernate.community.dialect.DB2LegacyDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.DerbyLegacyDialect;
import org.hibernate.community.dialect.H2LegacyDialect;
import org.hibernate.community.dialect.HSQLLegacyDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.community.dialect.IngresDialect;
import org.hibernate.community.dialect.SQLServerLegacyDialect;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.community.dialect.TeradataDialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IndexColumn;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import static java.util.Objects.requireNonNull;

/// Renders schema fragments used by tests without depending on removed
/// compatibility methods from `Dialect`.
///
/// @author Steve Ebersole
public final class DialectTestSupport {
	private DialectTestSupport() {
	}

	public static String createTableCommand(Dialect dialect) {
		return requireNonNull( dialect ).getTableCreationSupport()
				.createTableCommand( TableCreationKind.STANDARD );
	}

	public static String tableCreationOptions(Dialect dialect) {
		return requireNonNull( dialect ).getTableCreationSupport().tableCreationOptions();
	}

	public static String alterTableCommand(Dialect dialect, String tableName) {
		final var support = requireNonNull( dialect ).getAlterTableSupport();
		return support.alterTableCommand(
				requireNonNull( tableName ),
				dialect.getIfExistsSupport().alterTablePlacement()
		);
	}

	public static boolean supportsAlterColumnType(Dialect dialect) {
		return requireNonNull( dialect ).getAlterTableSupport().alterColumnType(
				new AlterColumnTypeRequest( "column_name", "integer", "integer" )
		) != null;
	}

	public static boolean dropsConstraintsExplicitly(Dialect dialect) {
		return requireNonNull( dialect ).getSchemaDropSupport().constraintDropMode()
				== ConstraintDropMode.EXPLICIT;
	}

	/// Determine whether Hibernate's tests assume that the configured JDBC
	/// driver permits result-position queries on a forward-only cursor.
	///
	/// Use this only to select tests. It preserves assumptions historically
	/// declared by built-in Dialects, but it is not a production Dialect
	/// capability or proof of driver behavior. Provider tests with different
	/// environment behavior should define their own `DialectCheck` or
	/// `DialectFeatureCheck`.
	///
	/// @since 8.0
	public static boolean supportsResultSetPositioningOnForwardOnlyCursor(Dialect dialect) {
		requireNonNull( dialect );
		return !( dialect instanceof DB2Dialect
				|| dialect instanceof DB2LegacyDialect
				|| dialect instanceof SQLServerDialect
				|| dialect instanceof SQLServerLegacyDialect
				|| dialect instanceof DerbyDialect
				|| dialect instanceof DerbyLegacyDialect
				|| dialect instanceof CacheDialect );
	}

	/// Determine whether Hibernate's tests assume that read-committed writers
	/// block readers in the configured database environment.
	///
	/// Use this only to select tests. It preserves assumptions historically
	/// declared by built-in Dialects, but it is not a production Dialect
	/// capability or proof of transaction behavior. Provider tests with
	/// different environment behavior should define their own `DialectCheck` or
	/// `DialectFeatureCheck`.
	///
	/// @since 8.0
	public static boolean doesReadCommittedCauseWritersToBlockReaders(Dialect dialect) {
		requireNonNull( dialect );
		if ( dialect instanceof SQLServerDialect || dialect instanceof SQLServerLegacyDialect ) {
			return false;
		}
		if ( dialect instanceof IngresDialect ) {
			return dialect.getVersion().isSameOrAfter( 9, 3 );
		}
		if ( dialect instanceof HSQLLegacyDialect ) {
			return dialect.getVersion().isSameOrAfter( 2 );
		}
		return dialect instanceof AbstractTransactSQLDialect
				|| dialect instanceof DB2iDialect
				|| dialect instanceof DB2zDialect
				|| dialect instanceof DB2LegacyDialect
				|| dialect instanceof HSQLDialect
				|| dialect instanceof DerbyDialect
				|| dialect instanceof DerbyLegacyDialect
				|| dialect instanceof InformixDialect
				|| dialect instanceof SQLiteDialect
				|| dialect instanceof H2LegacyDialect
				|| dialect instanceof TeradataDialect;
	}

	/// Determine whether Hibernate's tests assume that repeatable-read readers
	/// block writers in the configured database environment.
	///
	/// Use this only to select tests. It preserves assumptions historically
	/// declared by built-in Dialects, but it is not a production Dialect
	/// capability or proof of transaction behavior. Provider tests with
	/// different environment behavior should define their own `DialectCheck` or
	/// `DialectFeatureCheck`.
	///
	/// @since 8.0
	public static boolean doesRepeatableReadCauseReadersToBlockWriters(Dialect dialect) {
		requireNonNull( dialect );
		if ( dialect instanceof SQLServerDialect || dialect instanceof SQLServerLegacyDialect ) {
			return false;
		}
		if ( dialect instanceof IngresDialect ) {
			return dialect.getVersion().isSameOrAfter( 9, 3 );
		}
		if ( dialect instanceof HSQLLegacyDialect ) {
			return dialect.getVersion().isSameOrAfter( 2 );
		}
		return dialect instanceof AbstractTransactSQLDialect
				|| dialect instanceof HSQLDialect
				|| dialect instanceof SQLiteDialect
				|| dialect instanceof TeradataDialect;
	}

	public static String createIndexCommand(Dialect dialect, boolean unique) {
		return requireNonNull( dialect ).getIndexDdlSupport().createCommand(
				new IndexDdlRequest( unique, List.of( new IndexColumn( "column_name", false ) ) )
		);
	}

	public static String columnDefinition(Dialect dialect, String sqlType, boolean nullable) {
		final var appender = new StringBuilderSqlAppender();
		requireNonNull( dialect ).getColumnDefinitionSupport().appendDefinition(
				appender,
				new ColumnDefinitionRequest( requireNonNull( sqlType ), null, nullable, null, null )
		);
		return appender.toString();
	}

	public static String dropTableCommand(Dialect dialect, String tableName) {
		requireNonNull( dialect );
		requireNonNull( tableName );
		final var command = new StringBuilder( "drop table " );
		final var placement = dialect.getIfExistsSupport().dropTablePlacement();
		if ( placement == ExistenceCheckPlacement.BEFORE_NAME ) {
			command.append( "if exists " );
		}
		command.append( tableName );
		if ( placement == ExistenceCheckPlacement.AFTER_NAME ) {
			command.append( " if exists" );
		}
		return command.append( dialect.getSchemaDropSupport().cascadeConstraintsClause() ).toString();
	}
}
