/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression implements SqmExpression {
	private ExpressableType inherentType;

	public AbstractSqmExpression(ExpressableType inherentType) {
		this.inherentType = inherentType;
	}

	protected void setInherentType(ExpressableType inherentType) {
		if ( this.inherentType == null ) {
			this.inherentType = inherentType;
		}
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public ExpressableType getExpressableType() {
		return inherentType;
	}
}
