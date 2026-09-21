/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import org.hibernate.mapping.PhysicalTable;

import org.hibernate.mapping.NamedTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlRequest;
import org.hibernate.dialect.schema.spi.TableCleaner;
import org.hibernate.dialect.schema.spi.TruncateMode;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;


/// Standard table cleaner composed from focused Dialect schema strategies.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class StandardTableCleaner implements TableCleaner {
	protected final Dialect dialect;

	public StandardTableCleaner(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public ConstraintControlMode constraintControlMode() {
		return dialect.getConstraintControlSupport().constraintControlMode();
	}

	@Override
	public TruncateMode truncateMode() {
		return dialect.getTruncateSupport().truncateMode();
	}

	@Override
	public List<String> getSqlBeforeStrings() {
		return List.copyOf( dialect.getConstraintControlSupport().disableCommands() );
	}

	@Override
	public List<String> getSqlAfterStrings() {
		return List.copyOf( dialect.getConstraintControlSupport().enableCommands() );
	}

	@Override
	public List<String> getSqlDisableConstraintStrings(
			ForeignKey foreignKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return List.copyOf( dialect.getConstraintControlSupport().disableConstraintCommands(
				constraintRequest( foreignKey, context )
		) );
	}

	@Override
	public List<String> getSqlEnableConstraintStrings(
			ForeignKey foreignKey,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return List.copyOf( dialect.getConstraintControlSupport().enableConstraintCommands(
				constraintRequest( foreignKey, context )
		) );
	}

	@Override
	public List<String> getSqlTruncateStrings(
			Collection<PhysicalTable> tables,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final var tableNames = tables.stream()
				.map( table -> context.format( getTableName( table ) ) )
				.toList();
		final var commands = new ArrayList<>(
				dialect.getTruncateSupport().renderCommands( new TruncateRequest( tableNames ) )
		);
		for ( var table : tables ) {
			for ( var command : table.getInitCommands( context ) ) {
				commands.addAll( List.of( command.initCommands() ) );
			}
		}
		return List.copyOf( commands );
	}

	private static ConstraintControlRequest constraintRequest(
			ForeignKey foreignKey,
			SqlStringGenerationContext context) {
		return new ConstraintControlRequest(
				context.format( getTableName( foreignKey.getTable() ) ),
				foreignKey.getName()
		);
	}

	private static QualifiedPhysicalName getTableName(Table table) {
		return ((NamedTable) table).getPhysicalName();
	}
}
