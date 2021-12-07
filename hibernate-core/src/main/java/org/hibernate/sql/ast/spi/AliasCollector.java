/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.from.TableReference;

/**
 * A simple walker that checks for aggregate functions.
 *
 * @author Christian Beikov
 */
public class AliasCollector extends AbstractSqlAstWalker {

	private final Map<String, TableReference> tableReferenceMap = new HashMap<>();

	public static Map<String, TableReference> getTableReferences(SqlAstNode node) {
		final AliasCollector aliasCollector = new AliasCollector();
		node.accept( aliasCollector );
		return aliasCollector.tableReferenceMap;
	}

	@Override
	public void visitTableReference(TableReference tableReference) {
		tableReferenceMap.put( tableReference.getIdentificationVariable(), tableReference );
	}
}
