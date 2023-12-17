/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.orm.test.subquery;

import java.util.List;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Query;

/**
 * Test some subquery scenarios.
 *
 * @author Christian Beikov
 */
@RequiresDialect(H2Dialect.class)
public class SubqueryTest extends BaseSessionFactoryFunctionalTest {

	private static final SqmFunctionDescriptor LIMIT_FUNCTION = new LimitSqmSelfRenderingFunctionDescriptor();

	public static class LimitSqmSelfRenderingFunctionDescriptor extends AbstractSqmSelfRenderingFunctionDescriptor {

		public LimitSqmSelfRenderingFunctionDescriptor() {
			this(
					"limit",
					StandardArgumentsValidators.exactly( 2 ),
					StandardFunctionReturnTypeResolvers.useArgType( 1 )
			);
		}

		public LimitSqmSelfRenderingFunctionDescriptor(
				String name,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver) {
			super( name, argumentsValidator, returnTypeResolver, null );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " limit " + ( (UnparsedNumericLiteral<?>) sqlAstArguments.get( 1 ) ).getUnparsedLiteralValue() );
		}
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applySqlFunction( "limit", LIMIT_FUNCTION );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class
		};
	}

	@Test
	public void testNestedOrderBySubqueryInFunction() {
		inSession(
				session -> {
					Query q = session.createQuery(
							"SELECT a.id FROM EntityA a " +
									"ORDER BY CASE WHEN (" +
									"SELECT 1 FROM EntityA s1 " +
									"WHERE s1.id IN(" +
									"LIMIT(" +
									"(" +
									"SELECT 1 FROM EntityA sub " +
									"ORDER BY " +
									"CASE WHEN sub.name IS NULL THEN 1 ELSE 0 END, " +
									"sub.name DESC, " +
									"CASE WHEN sub.id IS NULL THEN 1 ELSE 0 END, " +
									"sub.id DESC" +
									")," +
									"1)" +
									")" +
									") = 1 THEN 1 ELSE 0 END"
					);
					q.getResultList();
				}
		);
	}
}
