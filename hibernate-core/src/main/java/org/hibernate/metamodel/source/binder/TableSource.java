/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.binder;

/**
 * Contract describing source of table information
 *
 * @author Steve Ebersole
 */
public interface TableSource {
	/**
	 * Obtain the supplied schema name
	 *
	 * @return The schema name. If {@code null}, the binder will apply the default.
	 */
	public String getExplicitSchemaName();

	/**
	 * Obtain the supplied catalog name
	 *
	 * @return The catalog name. If {@code null}, the binder will apply the default.
	 */
	public String getExplicitCatalogName();

	/**
	 * Obtain the supplied table name.
	 *
	 * @return The table name.
	 */
	public String getExplicitTableName();

	/**
	 * Obtain the logical name of the table.  This value is used to uniquely reference the table when binding
	 * values to the binding model.
	 *
	 * @return The logical name. Can be {@code null} in the case of the "primary table".
	 *
	 * @see RelationalValueSource#getContainingTableName()
	 */
	public String getLogicalName();
}
