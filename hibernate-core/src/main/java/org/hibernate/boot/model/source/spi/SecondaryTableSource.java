/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.List;

import org.hibernate.boot.model.CustomSql;
import org.hibernate.engine.FetchStyle;

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

	public String getLogicalTableNameForContainedColumns();

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
	public CustomSql getCustomSqlInsert();

	/**
	 * Obtain the custom SQL to be used for updates for this entity
	 *
	 * @return The custom update SQL
	 */
	public CustomSql getCustomSqlUpdate();

	/**
	 * Obtain the custom SQL to be used for deletes for this entity
	 *
	 * @return The custom delete SQL
	 */
	public CustomSql getCustomSqlDelete();
}
