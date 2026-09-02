/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupDescriptor;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.translation.Clause;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies the SQM translation contracts exposed by the standalone provider
/// fixture.
///
/// @author Steve Ebersole
public class ExampleSqmTranslationSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesSyntheticTableGroupPolicy() {
		final var support = dialect.getSyntheticTableGroupSupport();
		final var literal = proxy( Literal.class );
		final var expression = proxy( Expression.class );

		assertEquals(
				new SyntheticTableGroupDescriptor( "(select 1)", "dummy_(x)" ),
				support.resolveSyntheticTableGroup( Clause.GROUP, literal )
		);
		assertEquals(
				new SyntheticTableGroupDescriptor( "(select 1)", "dummy_(x)" ),
				support.resolveSyntheticTableGroup( Clause.ORDER, literal )
		);
		assertNull( support.resolveSyntheticTableGroup( Clause.ORDER, expression ) );
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> contract) {
		return (T) java.lang.reflect.Proxy.newProxyInstance(
				contract.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> null
		);
	}
}
