/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.internal;

import org.hibernate.Internal;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IndexColumn;
import org.hibernate.dialect.schema.spi.IndexDdlRequest;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentation;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentationRequest;
import org.hibernate.mapping.UniqueKey;

import static org.hibernate.internal.util.StringHelper.unqualify;

/// Uses `create unique index` when a constraint cannot provide the required
/// nullable-column semantics, or for every declaration when configured as an
/// index-only strategy.
///
/// @author Brett Meyer
/// @author Steve Ebersole
/// @since 8.0
@Internal
public class AlterTableUniqueIndexDelegate extends AlterTableUniqueDelegate {
	private final boolean alwaysIndex;

	public AlterTableUniqueIndexDelegate(Dialect dialect ) {
		this( dialect, false );
	}

	public AlterTableUniqueIndexDelegate(Dialect dialect, boolean alwaysIndex) {
		super( dialect );
		this.alwaysIndex = alwaysIndex;
	}

	@Override
	public UniqueKeyRepresentation representation(UniqueKeyRepresentationRequest request) {
		return alwaysIndex ? UniqueKeyRepresentation.INDEX : super.representation( request );
	}

	@Override
	public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		final var dialect = context.getDialect();
		if ( needsUniqueIndex( uniqueKey, dialect ) ) {
			final String constraintName = constraintName( uniqueKey, metadata.getDatabase() );
			final var columns = uniqueKey.getColumns();
			final var request = new IndexDdlRequest(
					true,
					columns.stream()
							.map( column -> new IndexColumn( column.getQuotedName( dialect ), column.isNullable() ) )
							.toList()
			);
			final var statement =
					new StringBuilder( dialect.getIndexDdlSupport().createCommand( request ) )
							.append( " " )
							.append( dialect.getIndexDdlSupport().nameQualification()
									== IndexNameQualification.QUALIFIED
									? constraintName
									: unqualify( constraintName ) )
							.append( " on " )
							.append( tableName( uniqueKey, context ) )
							.append( " (" );
			final var columnOrderMap = uniqueKey.getColumnOrderMap();
			boolean first = true;
			for ( var column : columns ) {
				if ( first ) {
					first = false;
				}
				else {
					statement.append(", ");
				}
				statement.append( column.getQuotedName( dialect ) );
				if ( columnOrderMap.containsKey( column ) ) {
					statement.append( " " ).append( columnOrderMap.get( column ) );
				}
			}
			statement.append( ")" );
			statement.append( dialect.getIndexDdlSupport().createTail( request ) );
			return statement.toString();
		}
		else {
			return super.getAlterTableToAddUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}

	@Override
	public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata,
			SqlStringGenerationContext context) {
		if ( needsUniqueIndex( uniqueKey, context.getDialect() ) ) {
			final var statement = new StringBuilder().append( "drop index " );
			if ( dialect.getIfExistsSupport().dropIndexPlacement() == ExistenceCheckPlacement.BEFORE_NAME ) {
				statement.append( "if exists " );
			}
			statement.append( tableName( uniqueKey, context ) ).append( '.' )
					.append( constraintName( uniqueKey, metadata.getDatabase() ) );
			return statement.toString();
		}
		else {
			return super.getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context );
		}
	}

	private boolean needsUniqueIndex(UniqueKey uniqueKey, Dialect dialect) {
		return alwaysIndex || uniqueKey.hasNullableColumn();
	}

}
