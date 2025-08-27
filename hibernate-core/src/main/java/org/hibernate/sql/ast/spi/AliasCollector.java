/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;

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
	public void visitNamedTableReference(NamedTableReference tableReference) {
		tableReferenceMap.put( tableReference.getIdentificationVariable(), tableReference );
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		tableReferenceMap.put( tableReference.getIdentificationVariable(), tableReference );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		tableReferenceMap.put( tableReference.getIdentificationVariable(), tableReference );
	}

	@Override
	public void visitFunctionTableReference(FunctionTableReference tableReference) {
		tableReferenceMap.put( tableReference.getIdentificationVariable(), tableReference );
	}
}
