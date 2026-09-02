/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.List;

import org.hibernate.dialect.schema.spi.AlterTableSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionSupport;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlSupport;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TableCleaner;
import org.hibernate.dialect.schema.spi.TableCreationKind;
import org.hibernate.dialect.schema.spi.TableCreationSupport;
import org.hibernate.dialect.schema.spi.TableMigrator;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateSupport;

/// Provider-owned, deliberately nonstandard schema support used to verify that
/// an external Dialect can implement the complete public Batch 6 surface.
///
/// @author Steve Ebersole
final class ExampleSchemaSupport {
	static final IfExistsSupport IF_EXISTS = new IfExistsSupport(
			ExistenceCheckPlacement.BEFORE_NAME,
			ExistenceCheckPlacement.AFTER_NAME,
			ExistenceCheckPlacement.BEFORE_NAME,
			ExistenceCheckPlacement.AFTER_NAME
	);

	static final AlterTableSupport ALTER_TABLE = new AlterTableSupport() {
		@Override
		public String alterTableCommand(String tableName, ExistenceCheckPlacement placement) {
			return "fixture alter table " + switch ( placement ) {
				case NONE -> tableName;
				case BEFORE_NAME -> "if exists " + tableName;
				case AFTER_NAME -> tableName + " if exists";
			};
		}

		@Override
		public String addColumnPrefix() {
			return "fixture add column";
		}

		@Override
		public String addColumnSuffix() {
			return " fixture column suffix";
		}

		@Override
		public String alterColumnType(org.hibernate.dialect.schema.spi.AlterColumnTypeRequest request) {
			return "fixture alter column " + request.columnName() + " as " + request.columnDefinition();
		}
	};

	static final TableCreationSupport TABLE_CREATION = new TableCreationSupport() {
		@Override
		public String createTableCommand(TableCreationKind kind) {
			return kind == TableCreationKind.MULTISET
					? "create fixture multiset table"
					: "create fixture table";
		}

		@Override
		public String tableCreationOptions() {
			return " fixture table options";
		}

		@Override
		public boolean requiresViewColumnList() {
			return true;
		}
	};

	static final ColumnDefinitionSupport COLUMN_DEFINITION = new ColumnDefinitionSupport() {
		@Override
		public void appendDefinition(
				org.hibernate.sql.spi.SqlAppender appender,
				org.hibernate.dialect.schema.spi.ColumnDefinitionRequest request) {
			appender.appendSql( " fixture_type(" );
			appender.appendSql( request.sqlType() );
			appender.appendSql( ')' );
			if ( request.renderedCollation() != null ) {
				appender.appendSql( " fixture_collate " );
				appender.appendSql( request.renderedCollation() );
			}
			if ( request.generatedExpression() != null ) {
				appender.appendSql( " fixture_generated(" );
				appender.appendSql( request.generatedExpression() );
				appender.appendSql( ')' );
			}
			if ( request.defaultExpression() != null ) {
				appender.appendSql( " fixture_default " );
				appender.appendSql( request.defaultExpression() );
			}
			appender.appendSql( request.nullable() ? " fixture_null" : " fixture_not_null" );
		}
	};

	static final IndexDdlSupport INDEX_DDL = new IndexDdlSupport() {
		@Override
		public String createCommand(org.hibernate.dialect.schema.spi.IndexDdlRequest request) {
			return request.unique() ? "create fixture unique index" : "create fixture index";
		}

		@Override
		public String createTail(org.hibernate.dialect.schema.spi.IndexDdlRequest request) {
			return request.columns().stream().anyMatch( org.hibernate.dialect.schema.spi.IndexColumn::nullable )
					? " fixture nullable index tail"
					: " fixture index tail";
		}

		@Override
		public IndexNameQualification nameQualification() {
			return IndexNameQualification.UNQUALIFIED;
		}
	};

	static final ConstraintControlSupport CONSTRAINT_CONTROL = new ConstraintControlSupport() {
		@Override
		public ConstraintControlMode constraintControlMode() {
			return ConstraintControlMode.GLOBAL;
		}

		@Override
		public List<String> disableCommands() {
			return List.of( "fixture constraints off" );
		}

		@Override
		public List<String> enableCommands() {
			return List.of( "fixture constraints on" );
		}
	};

	static final TruncateSupport TRUNCATE = new TruncateSupport() {
		@Override
		public TruncateMode truncateMode() {
			return TruncateMode.MULTI_TABLE;
		}

		@Override
		public List<String> renderCommands(org.hibernate.dialect.schema.spi.TruncateRequest request) {
			return request.tableNames().isEmpty()
					? List.of()
					: List.of( "fixture empty " + String.join( " then ", request.tableNames() ) );
		}
	};

	static final SchemaDropSupport SCHEMA_DROP = new SchemaDropSupport(
			List.of( "fixture prepare drop", "fixture prepare drop second" ),
			ConstraintDropMode.IMPLICIT,
			" fixture cascade"
	);

	static final TableMigrator TABLE_MIGRATOR = (table, metadata, tableInfo, context) ->
			new String[] { "fixture migrate table " + table.getName() };

	static final TableCleaner TABLE_CLEANER = new TableCleaner() {
		@Override
		public ConstraintControlMode constraintControlMode() {
			return ConstraintControlMode.PER_CONSTRAINT;
		}

		@Override
		public TruncateMode truncateMode() {
			return TruncateMode.MULTI_TABLE;
		}

		@Override
		public List<String> getSqlBeforeStrings() {
			return List.of( "fixture cleaner before" );
		}

		@Override
		public List<String> getSqlAfterStrings() {
			return List.of( "fixture cleaner after" );
		}

		@Override
		public List<String> getSqlDisableConstraintStrings(
				org.hibernate.mapping.ForeignKey foreignKey,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return List.of( "fixture disable " + foreignKey.getName() );
		}

		@Override
		public List<String> getSqlEnableConstraintStrings(
				org.hibernate.mapping.ForeignKey foreignKey,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return List.of( "fixture enable " + foreignKey.getName() );
		}

		@Override
		public List<String> getSqlTruncateStrings(
				java.util.Collection<org.hibernate.mapping.Table> tables,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return tables.isEmpty()
					? List.of()
					: List.of( "fixture cleaner empty " + tables.stream()
							.map( org.hibernate.mapping.Table::getName )
							.collect( java.util.stream.Collectors.joining( " then " ) ) );
		}
	};

	private ExampleSchemaSupport() {
	}
}
