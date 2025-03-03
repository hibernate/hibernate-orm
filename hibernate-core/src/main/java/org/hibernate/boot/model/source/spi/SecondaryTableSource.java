/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	TableSpecificationSource getTableSource();

	/**
	 * Retrieves the columns defines as making up this secondary tables primary key.  Each entry should have
	 * a corresponding entry in the foreign-key columns described by the {@link ForeignKeyContributingSource}
	 * aspect of this contract.
	 *
	 * @return The columns defining the primary key for this secondary table
	 */
	List<ColumnSource> getPrimaryKeyColumnSources();

	String getLogicalTableNameForContainedColumns();

	String getComment();

	FetchStyle getFetchStyle();

	boolean isInverse();

	boolean isOptional();

	boolean isCascadeDeleteEnabled();

	/**
	 * Obtain the custom SQL to be used for inserts for this entity
	 *
	 * @return The custom insert SQL
	 */
	CustomSql getCustomSqlInsert();

	/**
	 * Obtain the custom SQL to be used for updates for this entity
	 *
	 * @return The custom update SQL
	 */
	CustomSql getCustomSqlUpdate();

	/**
	 * Obtain the custom SQL to be used for deletes for this entity
	 *
	 * @return The custom delete SQL
	 */
	CustomSql getCustomSqlDelete();
}
