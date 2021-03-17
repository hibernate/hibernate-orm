/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

import antlr.CommonAST;

/**
 * Basic implementation of a {@link Node} bridging to the Antlr {@link CommonAST} hierarchy.
 *
 * @author Steve Ebersole
 */
public class NodeSupport extends CommonAST implements Node {
	@Override
	public String getDebugText() {
		return getText();
	}

	@Override
	public String getRenderableText() {
		return getText();
	}
}
