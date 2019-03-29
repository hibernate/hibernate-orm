/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree;

import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * A statement using a CTE
 *
 * @author Steve Ebersole
 */
public class CteStatement implements Statement {
	private final String cteLabel;
	private final Table cteTable;
	private final QuerySpec cteDefinition;
	private final Statement cteConsumer;

	public CteStatement(QuerySpec cteDefinition, String cteLabel, Table cteTable, Statement cteConsumer) {

		this.cteDefinition = cteDefinition;
		this.cteLabel = cteLabel;
		this.cteTable = cteTable;
		this.cteConsumer = cteConsumer;
	}

	public String getCteLabel() {
		return cteLabel;
	}

	public Table getCteTable() {
		return cteTable;
	}

	public QuerySpec getCteDefinition() {
		return cteDefinition;
	}

	public Statement getCteConsumer() {
		return cteConsumer;
	}
}
