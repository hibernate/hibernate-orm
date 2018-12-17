/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull extends SqmLiteral<Void> {
	public SqmLiteralNull() {
		super( null, NULL_TYPE );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLiteral( this );
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
		public SqlExpressableType getSqlExpressableType() {
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
}
