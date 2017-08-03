/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.spi;

import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SqlSelectionGroupEmpty implements SqlSelectionGroup {
	/**
	 * Singleton access
	 */
	public static final SqlSelectionGroupEmpty INSTANCE = new SqlSelectionGroupEmpty();

	@Override
	public List<SqlSelection> getSqlSelections() {
		return Collections.emptyList();
	}
}
