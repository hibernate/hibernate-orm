/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author Gail Badner
 */
public class ForeignKeyConstraintNameTests extends AbstractConstraintNameTests {
	@Override
	protected AbstractConstraint createConstraint(TableSpecification table, String constraintName) {
		// create referenced table with primary key; foreign key will reference
		// referencedTable's primary key
		TableSpecification referencedTable = createTable( "my_referenced_table" );
		referencedTable.getPrimaryKey().addColumn( referencedTable.locateColumn( COLUMN_NAMES[0] ) );
		return table.createForeignKey( referencedTable, constraintName );
	}
}
