/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;

/**
 * Represents a 'is null' check.
 *
 * @author Steve Ebersole
 */
public class IsNullLogicOperatorNode extends AbstractNullnessCheckNode {
	@Override
	protected int getExpansionConnectorType() {
		return HqlSqlTokenTypes.AND;
	}

	@Override
	protected String getExpansionConnectorText() {
		return "AND";
	}
}
