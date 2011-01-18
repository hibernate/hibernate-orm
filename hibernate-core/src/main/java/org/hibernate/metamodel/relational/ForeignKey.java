/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.relational;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Logger;

/**
 * Models the notion of a foreign key.
 * <p/>
 * Note that this need not mean a physical foreign key; we just mean a relationship between 2 table
 * specifications.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ForeignKey extends AbstractConstraint implements Constraint, Exportable {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Logger.class.getPackage().getName());

	private final TableSpecification targetTable;
	private List<Column> targetColumns;

	protected ForeignKey(TableSpecification sourceTable, TableSpecification targetTable, String name) {
		super( sourceTable, name );
		this.targetTable = targetTable;
	}

	protected ForeignKey(TableSpecification sourceTable, TableSpecification targetTable) {
		this( sourceTable, targetTable, null );
	}

	public TableSpecification getSourceTable() {
		return getTable();
	}

	public TableSpecification getTargetTable() {
		return targetTable;
	}

	public Iterable<Column> getSourceColumns() {
		return getColumns();
	}

	public Iterable<Column> getTargetColumns() {
		return targetColumns == null
				? getTargetTable().getPrimaryKey().getColumns()
				: targetColumns;
	}

	@Override
	public void addColumn(Column column) {
		addColumnMapping( column, null );
	}

	public void addColumnMapping(Column sourceColumn, Column targetColumn) {
		if ( targetColumn == null ) {
            if (targetColumns != null) LOG.attemptToMapColumnToNoTargetColumn(sourceColumn.toLoggableString(), getName());
		}
		else {
			if ( targetColumns == null ) {
                if (!internalColumnAccess().isEmpty()) LOG.valueMappingMismatch(getTable().toLoggableString(),
                                                                                getName(),
                                                                                sourceColumn.toLoggableString());
				targetColumns = new ArrayList<Column>();
			}
			targetColumns.add( targetColumn );
		}
		internalColumnAccess().add( sourceColumn );
	}

	@Override
	public String getExportIdentifier() {
		return getSourceTable().getLoggableValueQualifier() + ".FK-" + getName();
	}

	public void validate() {
		if ( getSourceTable() == null ) {

		}
	}
}
