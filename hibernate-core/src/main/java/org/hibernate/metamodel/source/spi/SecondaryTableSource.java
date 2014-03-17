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
package org.hibernate.metamodel.source.spi;

import java.util.List;

import org.hibernate.engine.FetchStyle;
import org.hibernate.metamodel.spi.binding.CustomSQL;

/**
 * @author Steve Ebersole
 */
public interface SecondaryTableSource extends ForeignKeyContributingSource {
	/**
	 * Obtain the table being joined to.
	 *
	 * @return The joined table.
	 */
	public TableSpecificationSource getTableSource();

	/**
	 * Retrieves the columns defines as making up this secondary tables primary key.  Each entry should have
	 * a corresponding entry in the foreign-key columns described by the {@link ForeignKeyContributingSource}
	 * aspect of this contract.
	 *
	 * @return The columns defining the primary key for this secondary table
	 */
	public List<ColumnSource> getPrimaryKeyColumnSources();

	public String getComment();

	public FetchStyle getFetchStyle();

	public boolean isInverse();

	public boolean isOptional();

	public boolean isCascadeDeleteEnabled();

	/**
	 * Obtain the custom SQL to be used for inserts for this entity
	 *
	 * @return The custom insert SQL
	 */
	public CustomSQL getCustomSqlInsert();

	/**
	 * Obtain the custom SQL to be used for updates for this entity
	 *
	 * @return The custom update SQL
	 */
	public CustomSQL getCustomSqlUpdate();

	/**
	 * Obtain the custom SQL to be used for deletes for this entity
	 *
	 * @return The custom delete SQL
	 */
	public CustomSQL getCustomSqlDelete();
}
