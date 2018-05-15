/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.HibernateException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull implements SqmLiteral<Void> {
	private BasicValuedExpressableType injectedExpressionType;

	public SqmLiteralNull() {
		injectedExpressionType = NULL_TYPE;
	}

	@Override
	public Void getLiteralValue() {
		return null;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return injectedExpressionType;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteralNullExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	private static BasicValuedExpressableType NULL_TYPE = new BasicValuedExpressableType<Object>() {
		@Override
		public int getNumberOfJdbcParametersNeeded() {
			return 0;
		}

		@Override
		public BasicType getBasicType() {
			return null;
		}

		@Override
		public PersistenceType getPersistenceType() {
			return null;
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return null;
		}

		@Override
		public Class getJavaType() {
			return void.class;
		}
	};

	@Override
	public void impliedType(ExpressableType type) {
		if ( !BasicValuedExpressableType.class.isInstance( type ) ) {
			throw new HibernateException( "Invalid type.  Found [" + type  + "], but expecting BasicValuedExpressableType" );
		}
		injectedExpressionType = (BasicValuedExpressableType) type;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return injectedExpressionType.getJavaTypeDescriptor();
	}
}
