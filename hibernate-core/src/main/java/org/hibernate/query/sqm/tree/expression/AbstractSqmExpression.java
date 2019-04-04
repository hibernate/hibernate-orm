/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.produce.SqmTreeCreationLogger;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression implements SqmExpression {
	private ExpressableType<?> type;

	public AbstractSqmExpression(ExpressableType<?> type) {
		this.type = type;
	}

	@Override
	public ExpressableType<?> getExpressableType() {
		return type;
	}

	@Override
	public final void applyInferableType(ExpressableType<?> type) {
		if ( type == null ) {
			return;
		}

		final ExpressableType<?> newType = QueryHelper.highestPrecedenceType( this.type, type );
		if ( newType != null && newType != this.type ) {
			internalApplyInferableType( newType );
		}
	}

	protected void internalApplyInferableType(ExpressableType<?> newType) {
		SqmTreeCreationLogger.LOGGER.debugf(
				"Applying inferable type to SqmExpression [%s] : %s -> %s",
				this,
				this.type,
				newType
		);
		this.type = newType;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		final ExpressableType expressableType = getExpressableType();
		return expressableType != null ? expressableType.getJavaTypeDescriptor() : null;
	}
}
