/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.List;

import org.hibernate.naming.spi.QualifiedName;

/**
 * @author Steve Ebersole
 */
public interface TargetTableInfo {
	/**
	 * The name of the table
	 */
	QualifiedName getName();

	/**
	 * The columns that make up the id table
	 */
	List<TargetColumnInfo> getTargetColumns();
}
