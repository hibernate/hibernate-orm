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

/**
 * Models what ANSI SQL terms a table specification which is a table or a view or an inline view.
 *
 * @author Steve Ebersole
 */
public interface TableSpecification extends ValueContainer {
	/**
	 * Get the primary key definition for this table spec.
	 *
	 * @return The PK definition.
	 */
	public PrimaryKey getPrimaryKey();

	public ForeignKey createForeignKey(TableSpecification targetTable, String name);

	public Iterable<ForeignKey> getForeignKeys();

	/**
	 * Get the physical table names modelled here.  This is especially important in the case of an inline view.
	 *
	 * @return The spaces.
	 */
	public Iterable<ObjectName> getSpaces();
}
