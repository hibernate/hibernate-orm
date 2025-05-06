/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.Cleaner;

import java.util.Collection;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.collections.ArrayHelper.join;

/**
 * The basic implementation of {@link Cleaner}.
 *
 * @author Gavin King
 */
@Incubating
public class StandardTableCleaner implements Cleaner {
	protected final Dialect dialect;

	public StandardTableCleaner(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String getSqlBeforeString() {
		return dialect.getDisableConstraintsStatement();
	}

	@Override
	public String getSqlAfterString() {
		return dialect.getEnableConstraintsStatement();
	}

	@Override
	public String[] getSqlTruncateStrings(Collection<Table> tables, Metadata metadata, SqlStringGenerationContext context) {
		String[] tableNames = tables.stream()
				.map( table -> context.format( getTableName( table ) ) )
				.collect( toList() )
				.toArray( ArrayHelper.EMPTY_STRING_ARRAY );
		String[] truncateTableStatements = dialect.getTruncateTableStatements( tableNames );
		String[] initStatements = tables.stream()
				.flatMap( table -> table.getInitCommands( context ).stream() )
				.flatMap( command -> stream( command.initCommands() ) )
				.toList()
				.toArray( ArrayHelper.EMPTY_STRING_ARRAY );
		return join( truncateTableStatements, initStatements );
	}

	@Override
	public String getSqlDisableConstraintString(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getDisableConstraintStatement( context.format( getTableName( foreignKey.getTable() ) ), foreignKey.getName() );
	}

	@Override
	public String getSqlEnableConstraintString(ForeignKey foreignKey, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getEnableConstraintStatement( context.format( getTableName( foreignKey.getTable() ) ), foreignKey.getName() );
	}

	private static QualifiedNameParser.NameParts getTableName(Table table) {
		return new QualifiedNameParser.NameParts(
				Identifier.toIdentifier(table.getCatalog(), table.isCatalogQuoted()),
				Identifier.toIdentifier(table.getSchema(), table.isSchemaQuoted()),
				table.getNameIdentifier()
		);
	}
}
