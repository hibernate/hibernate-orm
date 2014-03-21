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
package org.hibernate.metamodel.spi.relational;

import java.util.List;

/**
 * Basic contract for the types of constraints we fully support as metadata constructs:<ul>
 * <li>primary key</li>
 * <li>foreign key</li>
 * <li>unique constraint</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface Constraint extends Exportable {
	/**
	 * Obtain the table to which this constraint applies.
	 *
	 * @return The constrained table.
	 */
	public TableSpecification getTable();

	/**
	 * Obtain the constraint name.
	 *
	 * @return the name.
	 */
	public Identifier getName();

	/**
	 * Obtain a read-only view of the columns that are part of this constraint.
	 *
	 * @return A read-only view of the constrained columns.
	 */
	public List<Column> getColumns();
}
