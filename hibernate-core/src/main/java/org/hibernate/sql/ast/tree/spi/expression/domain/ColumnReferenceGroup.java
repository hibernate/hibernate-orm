/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;

/**
 * Encapsulates a group of ColumnReference references.
 *
 * @author Steve Ebersole
 */
public interface ColumnReferenceGroup {
	/**
	 * The grouped ColumnReferences.
	 */
	List<ColumnReference> getColumnReferences();
}
