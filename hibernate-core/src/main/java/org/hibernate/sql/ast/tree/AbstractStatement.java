/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree;

import java.util.Map;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;

/**
 * @author Christian Beikov
 */
public abstract class AbstractStatement implements Statement, CteContainer {

	private final Map<String, CteStatement> cteStatements;

	public AbstractStatement(Map<String, CteStatement> cteStatements) {
		this.cteStatements = cteStatements;
	}

	@Override
	public Map<String, CteStatement> getCteStatements() {
		return cteStatements;
	}

	@Override
	public CteStatement getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	public void addCteStatement(CteStatement cteStatement) {
		if ( cteStatements.putIfAbsent( cteStatement.getCteTable().getTableExpression(), cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getTableExpression() + " already exists" );
		}
	}
}
