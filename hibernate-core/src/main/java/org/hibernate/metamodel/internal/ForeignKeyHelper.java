/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal;

import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Gail Badner
 */
public class ForeignKeyHelper {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ForeignKeyHelper.class.getName()
	);

	private final Binder binder;

	public ForeignKeyHelper(Binder binder) {
		this.binder = binder;
	}

	public ForeignKey locateOrCreateForeignKey(
			final String foreignKeyName,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		ForeignKey foreignKey = null;
		if ( foreignKeyName != null ) {
			foreignKey = locateAndBindForeignKeyByName( foreignKeyName, sourceTable, sourceColumns, targetTable, targetColumns );
		}
		if ( foreignKey == null ) {
			foreignKey = locateForeignKeyByColumnMapping( sourceTable, sourceColumns, targetTable, targetColumns );
			if ( foreignKey != null && foreignKeyName != null ) {
				if ( foreignKey.getName() == null ) {
					// the foreign key name has not be initialized; set it to foreignKeyName
					foreignKey.setName( foreignKeyName );
				}
				else {
					// the foreign key name has already been initialized so cannot rename it
					// TODO: should this just be INFO?
					log.warn(
							String.format(
									"A foreign key mapped as %s will not be created because foreign key %s already exists with the same column mapping.",
									foreignKeyName,
									foreignKey.getName()
							)
					);
				}
			}
		}
		if ( foreignKey == null ) {
			// no foreign key found; create one
			foreignKey = sourceTable.createForeignKey( targetTable, foreignKeyName );
			bindForeignKeyColumns( foreignKey, sourceTable, sourceColumns, targetTable, targetColumns );
		}
		return foreignKey;
	}

	private static ForeignKey locateForeignKeyByColumnMapping(
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		// check for an existing foreign key with the same source/target columns
		ForeignKey foreignKey = null;
		Iterable<ForeignKey> possibleForeignKeys = sourceTable.locateForeignKey( targetTable );
		if ( possibleForeignKeys != null ) {
			for ( ForeignKey possibleFK : possibleForeignKeys ) {
				if ( possibleFK.getSourceColumns().equals( sourceColumns ) &&
						possibleFK.getTargetColumns().equals( targetColumns ) ) {
					// this is the foreign key
					foreignKey = possibleFK;
					break;
				}
			}
		}
		return foreignKey;
	}

	private void bindForeignKeyColumns(
			final ForeignKey foreignKey,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		if ( sourceColumns.size() != targetColumns.size() ) {
			throw binder.bindingContext().makeMappingException(
					String.format(
							"Non-matching number columns in foreign key source columns [%s : %s] and target columns [%s : %s]",
							sourceTable.getLogicalName().getText(),
							sourceColumns.size(),
							targetTable.getLogicalName().getText(),
							targetColumns.size()
					)
			);
		}
		for ( int i = 0; i < sourceColumns.size(); i++ ) {
			foreignKey.addColumnMapping( sourceColumns.get( i ), targetColumns.get( i ) );
		}
	}

	private ForeignKey locateAndBindForeignKeyByName(
			final String foreignKeyName,
			final TableSpecification sourceTable,
			final List<Column> sourceColumns,
			final TableSpecification targetTable,
			final List<Column> targetColumns) {
		if ( foreignKeyName == null ) {
			throw new AssertionFailure( "foreignKeyName must be non-null." );
		}
		ForeignKey foreignKey = sourceTable.locateForeignKey( foreignKeyName );
		if ( foreignKey != null ) {
			if ( !targetTable.equals( foreignKey.getTargetTable() ) ) {
				throw binder.bindingContext().makeMappingException(
						String.format(
								"Unexpected target table defined for foreign key \"%s\"; expected \"%s\"; found \"%s\"",
								foreignKeyName,
								targetTable.getLogicalName(),
								foreignKey.getTargetTable().getLogicalName()
						)
				);
			}
			// check if source and target columns have been bound already
			if ( foreignKey.getColumnSpan() == 0 ) {
				// foreign key was found, but no columns bound to it yet
				bindForeignKeyColumns( foreignKey, sourceTable, sourceColumns, targetTable, targetColumns );
			}
			else {
				// The located foreign key already has columns bound;
				// Make sure they are the same columns.
				if ( !foreignKey.getSourceColumns().equals( sourceColumns ) ||
						foreignKey.getTargetColumns().equals( targetColumns ) ) {
					throw binder.bindingContext().makeMappingException(
							String.format(
									"Attempt to bind exisitng foreign key \"%s\" with different columns.",
									foreignKeyName
							)
					);
				}
			}
		}
		return foreignKey;
	}
}
