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
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A helper for dealing with LimitHandler implementations
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class LimitHelper {
	/**
	 * Is a max row limit indicated?
	 *
	 * @param selection The row selection options
	 *
	 * @return Whether a max row limit was indicated
	 */
	public static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0;
	}

	/**
	 * Should limit be applied?
	 *
	 * @param limitHandler The limit handler
	 * @param selection The row selection
	 *
	 * @return Whether limiting is indicated
	 */
	public static boolean useLimit(LimitHandler limitHandler, RowSelection selection) {
		return limitHandler.supportsLimit() && hasMaxRows( selection );
	}

	/**
	 * Is a first row limit indicated?
	 *
	 * @param selection The row selection options
	 *
	 * @return Whether a first row limit in indicated
	 */
	public static boolean hasFirstRow(RowSelection selection) {
		return getFirstRow( selection ) > 0;
	}

	/**
	 * Retrieve the indicated first row for pagination
	 *
	 * @param selection The row selection options
	 *
	 * @return The first row
	 */
	public static int getFirstRow(RowSelection selection) {
		return ( selection == null || selection.getFirstRow() == null ) ? 0 : selection.getFirstRow();
	}

	private LimitHelper() {
	}
}
