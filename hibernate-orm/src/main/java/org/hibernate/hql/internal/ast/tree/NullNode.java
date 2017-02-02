/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import antlr.SemanticException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.Type;

/**
 * @author Andrea Boriero
 */
public class NullNode extends AbstractSelectExpression {

	public Type getDataType() {
		return null;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

	public Object getValue() {
		return null;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		return "null";
	}
}
