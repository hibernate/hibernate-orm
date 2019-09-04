/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface SqlSelectAstWalker extends SqlAstWalker {
	void visitSelectQuery(SelectStatement selectQuery);
}
