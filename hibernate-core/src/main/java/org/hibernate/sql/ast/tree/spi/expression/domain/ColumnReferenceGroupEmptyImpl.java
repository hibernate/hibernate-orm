/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;

/**
 * @author Steve Ebersole
 */
public class ColumnReferenceGroupEmptyImpl implements ColumnReferenceGroup {
	/**
	 * Singleton access
	 */
	public static final ColumnReferenceGroupEmptyImpl INSTANCE = new ColumnReferenceGroupEmptyImpl();

	@Override
	public List<ColumnReference> getColumnReferences() {
		return Collections.emptyList();
	}
}
